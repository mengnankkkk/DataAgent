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
package com.alibaba.cloud.ai.dataagent.agentscope.tool.sqlguard;

import com.alibaba.cloud.ai.dataagent.agentscope.dto.AgentRequest;
import com.alibaba.cloud.ai.dataagent.agentscope.runtime.ToolContextRequestResolver;
import com.alibaba.cloud.ai.dataagent.agentscope.tool.AgentScopedToolProvider;
import com.alibaba.cloud.ai.dataagent.agentscope.tool.ToolError;
import com.alibaba.cloud.ai.dataagent.agentscope.tool.ToolErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SqlGuardToolProvider implements AgentScopedToolProvider {

	private static final String TOOL_NAME = "sql_guard.check";

	private static final String INPUT_SCHEMA = """
			{
			  "type": "object",
			  "properties": {
			    "action": {
			      "type": "string",
			      "enum": ["SQL_VERIFY", "DATA_PROFILE"],
			      "description": "可选。默认 SQL_VERIFY。"
			    },
			    "query": {
			      "type": "string",
			      "description": "SQL_VERIFY 时必填。用户原始问题。"
			    },
			    "sql": {
			      "type": "string",
			      "description": "SQL_VERIFY 时必填。待校验 SQL。"
			    },
			    "tableName": {
			      "type": "string",
			      "description": "DATA_PROFILE 时必填。目标表名。"
			    },
			    "columnNames": {
			      "type": "array",
			      "items": {
			        "type": "string"
			      },
			      "description": "DATA_PROFILE 时可选。优先只传少量关键字段。"
			    },
			    "limit": {
			      "type": "integer",
			      "description": "DATA_PROFILE 时可选。返回上限，默认 5，最大 20。"
			    }
			  }
			}
			""";

	private static final String DESCRIPTION = """
			统一 SQL 守卫工具，供所有基于 SQL 的回答使用。
			1. `action=SQL_VERIFY`：在执行 SQL 或基于 SQL 生成最终回答前，检查候选 SQL 是否真正符合用户意图。
			2. `action=DATA_PROFILE`：只有在完成 schema 检查后，仍有少量关键候选字段语义不明确，且这种不确定性会实质影响过滤、分组、排序、时间窗口或指标写法时，才用于补充查看字段值分布。
			3. 不要把 DATA_PROFILE 当作每次查询的默认前置步骤；如果用户问题、schema 和列名已经足够明确，就直接跳过。
			4. 使用 DATA_PROFILE 时，优先传少量关键 `columnNames`，不要对整张表做无差别 profile。
			5. 如果 SQL_VERIFY 返回 `isAligned=false`，请读取 `problems`、`ruleChecks` 和 `fixSuggestions`，自行改写 SQL 后再次调用 `sql_guard.check`。
			6. 如果使用 DATA_PROFILE，请重点读取返回的 `columnProfiles`，理解空值率、去重计数、高频值、样例值，以及字段更像枚举、数值还是时间字段。
			""";

	private final ObjectMapper objectMapper;

	private final SqlVerifyExplainService sqlVerifyExplainService;

	public SqlGuardToolProvider(ObjectMapper objectMapper, SqlVerifyExplainService sqlVerifyExplainService) {
		this.objectMapper = objectMapper;
		this.sqlVerifyExplainService = sqlVerifyExplainService;
	}

	@Override
	public Map<String, ToolCallback> getToolCallbacks(String agentId) {
		ToolDefinition toolDefinition = ToolDefinition.builder()
			.name(TOOL_NAME)
			.description(DESCRIPTION)
			.inputSchema(INPUT_SCHEMA)
			.build();
		return Map.of(TOOL_NAME,
				new SqlGuardToolCallback(agentId, toolDefinition, objectMapper, sqlVerifyExplainService));
	}

	private static final class SqlGuardToolCallback implements ToolCallback {

		private final String agentId;

		private final ToolDefinition toolDefinition;

		private final ObjectMapper objectMapper;

		private final SqlVerifyExplainService sqlVerifyExplainService;

		private SqlGuardToolCallback(String agentId, ToolDefinition toolDefinition, ObjectMapper objectMapper,
				SqlVerifyExplainService sqlVerifyExplainService) {
			this.agentId = agentId;
			this.toolDefinition = toolDefinition;
			this.objectMapper = objectMapper;
			this.sqlVerifyExplainService = sqlVerifyExplainService;
		}

		@Override
		public ToolDefinition getToolDefinition() {
			return toolDefinition;
		}

		@Override
		public String call(String toolInput) {
			return execute(toolInput, null);
		}

		@Override
		public String call(String toolInput, ToolContext toolContext) {
			return execute(toolInput, toolContext);
		}

		private String execute(String toolInput, ToolContext toolContext) {
			try {
				SqlGuardCheckRequest request = StringUtils.hasText(toolInput)
						? objectMapper.readValue(toolInput, SqlGuardCheckRequest.class) : new SqlGuardCheckRequest();
				enrichRequestFromToolContext(request, toolContext);
				String action = request.normalizedAction();
				validateRequest(request, action);
				SqlGuardCheckResult result = switch (action) {
					case "DATA_PROFILE" -> sqlVerifyExplainService.inspectProfile(agentId, request);
					case "SQL_VERIFY" -> sqlVerifyExplainService.explain(request);
					default -> throw new IllegalArgumentException(objectToJson(
							ToolError.of(ToolErrorCode.UNSUPPORTED_ACTION, "不支持的 sql_guard.check 动作：" + action)));
				};
				return objectMapper.writeValueAsString(result);
			}
			catch (Exception ex) {
				throw new IllegalStateException(objectToJson(
						ToolError.of(ToolErrorCode.EXECUTION_FAILED, "sql_guard.check 执行失败：" + ex.getMessage())), ex);
			}
		}

		private void validateRequest(SqlGuardCheckRequest request, String action) {
			if ("DATA_PROFILE".equals(action)) {
				requireText(request.getTableName(), "DATA_PROFILE 需要 tableName 参数");
				return;
			}
			if ("SQL_VERIFY".equals(action)) {
				requireText(request.getQuery(), "SQL_VERIFY 需要 query 参数");
				requireText(request.getSql(), "SQL_VERIFY 需要 sql 参数");
			}
		}

		private void requireText(String value, String message) {
			if (!StringUtils.hasText(value)) {
				throw new IllegalArgumentException(objectToJson(ToolError.of(ToolErrorCode.INVALID_INPUT, message)));
			}
		}

		private void enrichRequestFromToolContext(SqlGuardCheckRequest request, ToolContext toolContext) {
			if (request == null || StringUtils.hasText(request.getHumanFeedbackContent())) {
				return;
			}
			AgentRequest agentRequest = ToolContextRequestResolver.resolveGraphRequest(toolContext);
			if (agentRequest == null) {
				return;
			}
			request.setHumanFeedbackContent(agentRequest.getHumanFeedbackContent());
		}

		private String objectToJson(Object value) {
			try {
				return objectMapper.writeValueAsString(value);
			}
			catch (Exception ex) {
				return "{\"code\":\"EXECUTION_FAILED\",\"message\":\"工具错误序列化失败\"}";
			}
		}

	}

}
