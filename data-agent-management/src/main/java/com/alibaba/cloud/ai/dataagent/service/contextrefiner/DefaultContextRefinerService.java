/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dataagent.service.contextrefiner;

import com.alibaba.cloud.ai.dataagent.agentscope.dto.GraphRequest;
import com.alibaba.cloud.ai.dataagent.agentscope.runtime.AgentScopeToolkitFactory;
import com.alibaba.cloud.ai.dataagent.entity.ChatMessage;
import com.alibaba.cloud.ai.dataagent.properties.DataAgentProperties;
import com.alibaba.cloud.ai.dataagent.service.chat.ChatMessageService;
import com.alibaba.cloud.ai.dataagent.service.contextrefiner.dto.ContextRefineMessage;
import com.alibaba.cloud.ai.dataagent.service.contextrefiner.dto.ContextRefineRequest;
import com.alibaba.cloud.ai.dataagent.service.contextrefiner.dto.ContextRefineResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultContextRefinerService implements ContextRefinerService {

	private static final int AGENT_MEMORY_MESSAGE_LIMIT = 20;

	private final DataAgentProperties dataAgentProperties;

	private final ChatMessageService chatMessageService;

	private final AgentScopeToolkitFactory agentScopeToolkitFactory;

	private final ContextRefinerClient contextRefinerClient;

	@Override
	public ContextRefinerResult prepareRuntimePrompt(GraphRequest request, String systemPrompt) {
		String rawUserPrompt = request.getQuery() == null ? "" : request.getQuery();
		DataAgentProperties.ContextRefiner config = dataAgentProperties.getContextRefiner();
		if (!config.isEnabled()) {
			return ContextRefinerResult.passthrough(systemPrompt, rawUserPrompt);
		}
		if (!StringUtils.hasText(config.getEndpoint()) || !StringUtils.hasText(rawUserPrompt)) {
			log.debug("Skip context refiner because endpoint or user prompt is blank. threadId={}", request.getThreadId());
			return ContextRefinerResult.passthrough(systemPrompt, rawUserPrompt);
		}

		ContextRefineRequest refineRequest = new ContextRefineRequest(systemPrompt, buildMessages(request, rawUserPrompt),
				buildRagContext(request), config.getBudget());
		try {
			ContextRefineResponse refineResponse = contextRefinerClient.refine(config.getEndpoint(), refineRequest,
					Duration.ofSeconds(Math.max(1, config.getTimeoutSeconds())));
			if (refineResponse == null || !StringUtils.hasText(refineResponse.getPrompt())) {
				log.warn("Context refiner returned empty prompt, fallback to raw prompt. threadId={}", request.getThreadId());
				return ContextRefinerResult.passthrough(systemPrompt, rawUserPrompt);
			}
			ContextRefinerResult result = switch (config.getApplyMode()) {
				case FULL_PROMPT -> new ContextRefinerResult("", refineResponse.getPrompt(), false, true,
						refineResponse.getTraceId(), refineResponse.getInputTokens(), refineResponse.getOutputTokens(),
						refineResponse.getSavedTokens(), refineResponse.getBudgetMet(), refineResponse.getCacheHit());
				case USER_PROMPT -> new ContextRefinerResult(systemPrompt, refineResponse.getPrompt(), true, true,
						refineResponse.getTraceId(), refineResponse.getInputTokens(), refineResponse.getOutputTokens(),
						refineResponse.getSavedTokens(), refineResponse.getBudgetMet(), refineResponse.getCacheHit());
			};
			log.info(
					"Context refiner applied. threadId={}, traceId={}, mode={}, cacheHit={}, inputTokens={}, outputTokens={}, savedTokens={}, budgetMet={}",
					request.getThreadId(), refineResponse.getTraceId(), config.getApplyMode(),
					refineResponse.getCacheHit(), refineResponse.getInputTokens(), refineResponse.getOutputTokens(),
					refineResponse.getSavedTokens(), refineResponse.getBudgetMet());
			return result;
		}
		catch (Exception ex) {
			if (config.isFailOpen()) {
				log.warn("Context refiner failed, fallback to raw prompt. threadId={}, reason={}", request.getThreadId(),
						ex.getMessage());
				return ContextRefinerResult.passthrough(systemPrompt, rawUserPrompt);
			}
			throw ex;
		}
	}

	private List<ContextRefineMessage> buildMessages(GraphRequest request, String rawUserPrompt) {
		List<ContextRefineMessage> messages = new ArrayList<>();
		String threadId = request.getThreadId();
		if (StringUtils.hasText(threadId)) {
			List<ChatMessage> history = chatMessageService.findRecentBySessionId(threadId, AGENT_MEMORY_MESSAGE_LIMIT);
			history.stream().map(this::toRefineMessage).filter(msg -> msg != null).forEach(messages::add);
		}
		appendFeedbackMessage(request, messages);
		appendLatestUserPromptIfMissing(messages, rawUserPrompt);
		return messages;
	}

	private List<Object> buildRagContext(GraphRequest request) {
		List<Object> rag = new ArrayList<>();
		appendToolCatalog(request, rag);
		return rag;
	}

	private void appendToolCatalog(GraphRequest request, List<Object> rag) {
		if (!StringUtils.hasText(request.getAgentId())) {
			return;
		}
		Map<String, ToolCallback> toolCallbacks = agentScopeToolkitFactory.getToolCallbacks(request.getAgentId());
		if (toolCallbacks == null || toolCallbacks.isEmpty()) {
			return;
		}
		for (Map.Entry<String, ToolCallback> entry : toolCallbacks.entrySet()) {
			Map<String, Object> toolContext = toToolContext(entry.getKey(), entry.getValue());
			if (toolContext == null || toolContext.isEmpty()) {
				continue;
			}
			rag.add(toolContext);
		}
	}

	private Map<String, Object> toToolContext(String fallbackName, ToolCallback toolCallback) {
		if (toolCallback == null) {
			return null;
		}
		ToolDefinition toolDefinition = toolCallback.getToolDefinition();
		String toolName = toolDefinition != null && StringUtils.hasText(toolDefinition.name()) ? toolDefinition.name()
				: fallbackName;
		if (!StringUtils.hasText(toolName)) {
			return null;
		}
		Map<String, Object> toolContext = new LinkedHashMap<>();
		toolContext.put("type", "tool");
		toolContext.put("name", toolName.trim());
		String description = normalizeWhitespace(toolDefinition == null ? null : toolDefinition.description());
		if (StringUtils.hasText(description)) {
			toolContext.put("description", description);
		}
		String inputSchema = normalizeWhitespace(toolDefinition == null ? null : toolDefinition.inputSchema());
		if (StringUtils.hasText(inputSchema)) {
			toolContext.put("input_schema", inputSchema);
		}
		return toolContext;
	}

	private void appendFeedbackMessage(GraphRequest request, List<ContextRefineMessage> messages) {
		if (!StringUtils.hasText(request.getHumanFeedbackContent())) {
			return;
		}
		messages.add(new ContextRefineMessage("user", "用户补充反馈: " + request.getHumanFeedbackContent().trim()));
	}

	private void appendLatestUserPromptIfMissing(List<ContextRefineMessage> messages, String rawUserPrompt) {
		if (!StringUtils.hasText(rawUserPrompt)) {
			return;
		}
		if (messages.isEmpty()) {
			messages.add(new ContextRefineMessage("user", rawUserPrompt));
			return;
		}
		ContextRefineMessage lastMessage = messages.get(messages.size() - 1);
		if (!"user".equals(lastMessage.getRole()) || !rawUserPrompt.equals(lastMessage.getContent())) {
			messages.add(new ContextRefineMessage("user", rawUserPrompt));
		}
	}

	private ContextRefineMessage toRefineMessage(ChatMessage chatMessage) {
		if (chatMessage == null || !StringUtils.hasText(chatMessage.getContent())) {
			return null;
		}
		return new ContextRefineMessage(normalizeRole(chatMessage.getRole()), chatMessage.getContent());
	}

	private String normalizeRole(String role) {
		if (!StringUtils.hasText(role)) {
			return "user";
		}
		String normalizedRole = role.trim().toLowerCase();
		return switch (normalizedRole) {
			case "assistant", "system", "tool", "user" -> normalizedRole;
			default -> "user";
		};
	}

	private String normalizeWhitespace(String value) {
		if (!StringUtils.hasText(value)) {
			return value;
		}
		return value.replaceAll("\\s+", " ").trim();
	}

}
