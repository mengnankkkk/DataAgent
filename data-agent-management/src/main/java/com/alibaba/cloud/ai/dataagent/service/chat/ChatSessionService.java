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

import com.alibaba.cloud.ai.dataagent.entity.ChatSession;

import java.util.List;

/**
 * Chat Session Service Class
 */
public interface ChatSessionService {

	/**
	 * Get session list by agent ID
	 */
	List<ChatSession> findByAgentId(Integer agentId);

	/**
	 * Create a new session
	 */
	ChatSession createSession(Integer agentId, String title, Long userId);

	/**
	 * Find session by id.
	 */
	ChatSession findBySessionId(String sessionId);

	ChatSession requireSessionForAgent(String sessionId, Long agentId);

	/**
	 * Clear all sessions for an agent
	 */
	void clearSessionsByAgentId(Integer agentId);

	/**
	 * Update the last activity time of a session
	 */
	void updateSessionTime(String sessionId);

	void updateSessionTime(String sessionId, Long agentId);

	/**
	 * 置顶/取消置顶会话
	 */
	void pinSession(String sessionId, boolean isPinned);

	void pinSession(String sessionId, boolean isPinned, Long agentId);

	/**
	 * Rename session
	 */
	void renameSession(String sessionId, String newTitle);

	void renameSession(String sessionId, String newTitle, Long agentId);

	/**
	 * Delete a single session
	 */
	void deleteSession(String sessionId);

	void deleteSession(String sessionId, Long agentId);

}
