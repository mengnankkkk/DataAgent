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
package com.alibaba.cloud.ai.dataagent.agentscope.session;

import com.alibaba.cloud.ai.dataagent.entity.ChatMessage;
import com.alibaba.cloud.ai.dataagent.mapper.ChatMessageMapper;
import com.alibaba.cloud.ai.dataagent.mapper.ChatSessionMapper;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.core.state.State;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.util.JsonCodec;
import io.agentscope.core.util.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentScopeMysqlSession implements Session {

	static final String STATE_MESSAGE_TYPE_PREFIX = "agentscope-state:";

	private static final String STATE_KIND = "agentscope-session-state";

	private static final String STATE_VISIBILITY = "system-hidden";

	private static final String STATE_ROLE = "system";

	private final ChatSessionMapper chatSessionMapper;

	private final ChatMessageMapper chatMessageMapper;

	private final JsonCodec jsonCodec = JsonUtils.getJsonCodec();

	@Override
	public void save(SessionKey sessionKey, String moduleName, State state) {
		String sessionId = sessionId(sessionKey);
		assertManagedChatSession(sessionId);
		String messageType = messageType(moduleName);
		chatMessageMapper.deleteBySessionIdAndMessageType(sessionId, messageType);
		chatMessageMapper.insert(buildStateMessage(sessionId, messageType, moduleName, state.getClass().getName(), 0,
				jsonCodec.toPrettyJson(state)));
	}

	@Override
	public void save(SessionKey sessionKey, String moduleName, List<? extends State> states) {
		String sessionId = sessionId(sessionKey);
		assertManagedChatSession(sessionId);
		String messageType = messageType(moduleName);
		chatMessageMapper.deleteBySessionIdAndMessageType(sessionId, messageType);
		if (states == null || states.isEmpty()) {
			return;
		}
		for (int index = 0; index < states.size(); index++) {
			State state = states.get(index);
			if (state == null) {
				continue;
			}
			chatMessageMapper.insert(buildStateMessage(sessionId, messageType, moduleName, state.getClass().getName(),
					index, jsonCodec.toJson(state)));
		}
	}

	@Override
	public <T extends State> Optional<T> get(SessionKey sessionKey, String moduleName, Class<T> stateClass) {
		List<ChatMessage> rows = chatMessageMapper.selectStateBySessionIdAndMessageType(sessionId(sessionKey),
				messageType(moduleName));
		if (rows.isEmpty()) {
			return Optional.empty();
		}
		ChatMessage latestRow = rows.get(rows.size() - 1);
		return Optional.ofNullable(jsonCodec.fromJson(latestRow.getContent(), stateClass));
	}

	@Override
	public <T extends State> List<T> getList(SessionKey sessionKey, String moduleName, Class<T> stateClass) {
		return chatMessageMapper.selectStateBySessionIdAndMessageType(sessionId(sessionKey), messageType(moduleName))
			.stream()
			.map(ChatMessage::getContent)
			.map(content -> jsonCodec.fromJson(content, stateClass))
			.collect(Collectors.toList());
	}

	@Override
	public boolean exists(SessionKey sessionKey) {
		return chatMessageMapper.countAgentScopeStateBySessionId(sessionId(sessionKey)) > 0;
	}

	@Override
	public void delete(SessionKey sessionKey) {
		chatMessageMapper.deleteAgentScopeStateBySessionId(sessionId(sessionKey));
	}

	@Override
	public void delete(SessionKey sessionKey, String moduleName) {
		chatMessageMapper.deleteBySessionIdAndMessageType(sessionId(sessionKey), messageType(moduleName));
	}

	@Override
	public Set<SessionKey> listSessionKeys() {
		return chatMessageMapper.selectSessionIdsWithAgentScopeState()
			.stream()
			.map(SimpleSessionKey::of)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public Mono<Integer> clearAllSessions() {
		return Mono.fromCallable(chatMessageMapper::deleteAllAgentScopeStateMessages)
			.subscribeOn(Schedulers.boundedElastic());
	}

	private void assertManagedChatSession(String sessionId) {
		if (chatSessionMapper.selectBySessionId(sessionId) == null) {
			throw new IllegalStateException(
					"AgentScope session persistence requires an existing chat_session. sessionId=" + sessionId);
		}
	}

	private String sessionId(SessionKey sessionKey) {
		return sessionKey.toIdentifier();
	}

	private String messageType(String moduleName) {
		String encodedModuleName = java.util.Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString(moduleName.getBytes(StandardCharsets.UTF_8));
		return STATE_MESSAGE_TYPE_PREFIX + encodedModuleName;
	}

	private ChatMessage buildStateMessage(String sessionId, String messageType, String moduleName, String stateClass,
			int sequence, String content) {
		Map<String, Object> metadata = Map.of("kind", STATE_KIND, "visibility", STATE_VISIBILITY, "moduleName",
				moduleName, "stateClass", stateClass, "sequence", sequence);
		return ChatMessage.builder()
			.sessionId(sessionId)
			.role(STATE_ROLE)
			.content(content)
			.messageType(messageType)
			.metadata(jsonCodec.toJson(metadata))
			.build();
	}

}
