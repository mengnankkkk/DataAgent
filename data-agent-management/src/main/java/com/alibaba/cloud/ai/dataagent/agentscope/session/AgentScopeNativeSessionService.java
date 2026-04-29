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

import io.agentscope.core.memory.Memory;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.core.state.SessionKey;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class AgentScopeNativeSessionService {

	private static final Pattern SESSION_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

	private final Session session;

	public AgentScopeNativeSessionService(AgentScopeMysqlSession session) {
		this.session = session;
	}

	public boolean loadMemoryIfExists(Memory memory, String sessionId) {
		Objects.requireNonNull(memory, "memory must not be null");
		if (!StringUtils.hasText(sessionId)) {
			return false;
		}
		try {
			return memory.loadIfExists(session, sessionKey(sessionId));
		}
		catch (RuntimeException ex) {
			log.warn("Failed to load AgentScope native session state from MySQL. sessionId={}", sessionId, ex);
			return false;
		}
	}

	public void saveMemory(Memory memory, String sessionId) {
		Objects.requireNonNull(memory, "memory must not be null");
		if (!StringUtils.hasText(sessionId)) {
			return;
		}
		memory.saveTo(session, sessionKey(sessionId));
	}

	public void deleteSessionState(String sessionId) {
		if (!StringUtils.hasText(sessionId)) {
			return;
		}
		try {
			SessionKey sessionKey = sessionKey(sessionId);
			if (session.exists(sessionKey)) {
				session.delete(sessionKey);
			}
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Failed to delete AgentScope session state: " + sessionId, ex);
		}
	}

	public void deleteSessionStates(Collection<String> sessionIds) {
		if (sessionIds == null || sessionIds.isEmpty()) {
			return;
		}
		for (String sessionId : new LinkedHashSet<>(sessionIds)) {
			deleteSessionState(sessionId);
		}
	}

	private SessionKey sessionKey(String sessionId) {
		return SimpleSessionKey.of(normalizeSessionId(sessionId));
	}

	private String normalizeSessionId(String sessionId) {
		if (!StringUtils.hasText(sessionId)) {
			throw new IllegalArgumentException("sessionId 不能为空");
		}
		String normalized = sessionId.trim();
		if (!SESSION_ID_PATTERN.matcher(normalized).matches()) {
			throw new IllegalArgumentException("sessionId 包含非法字符: " + sessionId);
		}
		return normalized;
	}

}
