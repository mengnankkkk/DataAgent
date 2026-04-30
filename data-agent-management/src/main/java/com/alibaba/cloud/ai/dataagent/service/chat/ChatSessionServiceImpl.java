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
package com.alibaba.cloud.ai.dataagent.service.chat;

import com.alibaba.cloud.ai.dataagent.agentscope.session.AgentScopeNativeSessionService;
import com.alibaba.cloud.ai.dataagent.constant.AgentSessionConstant;
import com.alibaba.cloud.ai.dataagent.entity.ChatSession;
import com.alibaba.cloud.ai.dataagent.mapper.ChatSessionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@AllArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

	private final ChatSessionMapper chatSessionMapper;

	private final AgentScopeNativeSessionService nativeSessionService;

	/**
	 * Get session list by agent ID
	 */
	@Override
	public List<ChatSession> findByAgentId(Integer agentId) {
		return chatSessionMapper.selectByAgentId(agentId);
	}

	@Override
	public ChatSession findBySessionId(String sessionId) {
		return chatSessionMapper.selectBySessionId(sessionId);
	}

	@Override
	public ChatSession requireSessionForAgent(String sessionId, Long agentId) {
		ChatSession session = chatSessionMapper.selectBySessionId(sessionId);
		if (session == null || agentId == null || session.getAgentId() == null
				|| !agentId.equals(session.getAgentId().longValue())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "session not found");
		}
		return session;
	}

	/**
	 * Create a new session
	 */
	@Override
	public ChatSession createSession(Integer agentId, String title, Long userId) {
		String sessionId = UUID.randomUUID().toString();

		ChatSession session = new ChatSession(sessionId, agentId,
				title != null ? title : AgentSessionConstant.DEFAULT_SESSION_TITLE, "active", userId);
		chatSessionMapper.insert(session);

		log.info("Created new chat session: {} for agent: {}", sessionId, agentId);
		return session;
	}

	/**
	 * Clear all sessions for an agent
	 */
	@Override
	public void clearSessionsByAgentId(Integer agentId) {
		List<String> sessionIds = findByAgentId(agentId).stream().map(ChatSession::getId).collect(Collectors.toList());
		LocalDateTime now = LocalDateTime.now();
		int updated = chatSessionMapper.softDeleteByAgentId(agentId, now);
		nativeSessionService.deleteSessionStates(sessionIds);
		log.info("Cleared {} sessions for agent: {}", updated, agentId);
	}

	/**
	 * Update the last activity time of a session
	 */
	@Override
	public void updateSessionTime(String sessionId) {
		LocalDateTime now = LocalDateTime.now();
		chatSessionMapper.updateSessionTime(sessionId, now);
	}

	@Override
	public void updateSessionTime(String sessionId, Long agentId) {
		requireSessionForAgent(sessionId, agentId);
		updateSessionTime(sessionId);
	}

	/**
	 * 置顶/取消置顶会话
	 */
	@Override
	public void pinSession(String sessionId, boolean isPinned) {
		LocalDateTime now = LocalDateTime.now();
		chatSessionMapper.updatePinStatus(sessionId, isPinned, now);
		log.info("Updated pin status for session: {} to: {}", sessionId, isPinned);
	}

	@Override
	public void pinSession(String sessionId, boolean isPinned, Long agentId) {
		requireSessionForAgent(sessionId, agentId);
		pinSession(sessionId, isPinned);
	}

	/**
	 * Rename session
	 */
	@Override
	public void renameSession(String sessionId, String newTitle) {
		LocalDateTime now = LocalDateTime.now();
		chatSessionMapper.updateTitle(sessionId, newTitle, now);
		log.info("Renamed session: {} to: {}", sessionId, newTitle);
	}

	@Override
	public void renameSession(String sessionId, String newTitle, Long agentId) {
		requireSessionForAgent(sessionId, agentId);
		renameSession(sessionId, newTitle);
	}

	/**
	 * Delete a single session
	 */
	@Override
	public void deleteSession(String sessionId) {
		LocalDateTime now = LocalDateTime.now();
		chatSessionMapper.softDeleteById(sessionId, now);
		nativeSessionService.deleteSessionState(sessionId);
		log.info("Deleted session: {}", sessionId);
	}

	@Override
	public void deleteSession(String sessionId, Long agentId) {
		requireSessionForAgent(sessionId, agentId);
		deleteSession(sessionId);
	}

}
