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
package com.alibaba.cloud.ai.dataagent.agentscope.tool.semantic;

import com.alibaba.cloud.ai.dataagent.agentscope.tool.AgentScopedToolProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SemanticModelToolProvider implements AgentScopedToolProvider {

	private static final String TOOL_NAME = "semantic_model.search";

	private static final String DESCRIPTION = """
			仅用于补充理解表和字段语义的辅助工具。
			当用户在询问某张表或某个字段的含义、业务友好名称、枚举含义、字段使用备注，或数据库物理表结构中未显式存储的关系提示时，才使用本工具。
			典型问题包括：“token 名称类型”“status 字段什么意思”“这个字段有哪些别名”“这两个表可能怎么关联”。
			数据库里的物理表结构、字段列表、字段类型、样例预览和只读 SQL，应优先使用数据源探索工具获取。
			不要把本工具用于 SQL 执行、数据源探索工具已能覆盖的表结构探索，或属于 `domain_business_knowledge.search` 的业务定义、指标口径和 SOP 检索。
			""";

	private final SemanticModelToolSupport toolSupport;

	@Override
	public Map<String, ToolCallback> getToolCallbacks(String agentId) {
		if (!StringUtils.hasText(agentId)) {
			return Map.of();
		}
		return Map.of(TOOL_NAME, toolSupport.createSearchToolCallback(agentId, TOOL_NAME, DESCRIPTION));
	}

}
