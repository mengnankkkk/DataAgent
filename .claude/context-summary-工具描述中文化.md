## 项目上下文摘要（工具描述中文化）
生成时间：2026-04-28

### 1. 相似实现分析
- **实现1**: data-agent-management/src/main/java/com/alibaba/cloud/ai/dataagent/agentscope/tool/datasource/DatasourceExplorerToolProvider.java:131-152
  - 模式：由 provider 动态构造 `ToolDefinition.description`
  - 可复用：中文分点式说明风格，明确适用范围、顺序和禁用场景
  - 需注意：描述会直接影响 Agent 工具选择与调用顺序

- **实现2**: data-agent-management/src/main/java/com/alibaba/cloud/ai/dataagent/agentscope/tool/semantic/SemanticModelToolSupport.java:32-50
  - 模式：`INPUT_SCHEMA` 采用中文字段说明，强调“必填/可选/何时不必传”
  - 可复用：schema 文案风格可直接作为其他工具输入说明参考
  - 需注意：和 provider 的 description 要保持边界一致

- **实现3**: data-agent-management/src/main/java/com/alibaba/cloud/ai/dataagent/agentscope/tool/knowledge/DomainBusinessKnowledgeToolSupport.java:37-63
  - 模式：support 负责统一 `ToolDefinition` 的 input schema
  - 可复用：中文化时优先改 provider 的 DESCRIPTION，support 仅修正残留英文术语
  - 需注意：不要改动工具行为，只改面向 Agent 的说明文本

### 2. 项目约定
- **命名约定**: Java 类、常量、方法名保持现有英文命名；说明文本统一简体中文
- **文件组织**: provider 负责工具注册与 description；support 负责 input schema 和 callback 封装
- **导入顺序**: 保持现有 import 顺序，不做无关调整
- **代码风格**: 使用 Java 文本块 `"""` 保存多行描述，延续现有缩进和换行风格

### 3. 可复用组件清单
- `data-agent-management/src/main/java/com/alibaba/cloud/ai/dataagent/agentscope/tool/datasource/DatasourceExplorerToolProvider.java`：中文工具描述样板
- `data-agent-management/src/main/resources/prompts/commonagent.md`：工具边界和调用顺序的权威规则
- `data-agent-management/src/main/java/com/alibaba/cloud/ai/dataagent/agentscope/tool/semantic/SemanticModelToolSupport.java`：中文 schema 样板
- `data-agent-management/src/main/java/com/alibaba/cloud/ai/dataagent/agentscope/tool/knowledge/DomainBusinessKnowledgeToolSupport.java`：中文 schema 样板

### 4. 测试策略
- **测试框架**: 本次先做静态验证，不新增测试
- **验证方式**: 搜索所有注册工具 description / input schema，确认英文描述已清理且工具边界与 `commonagent.md` 一致
- **参考文件**: `commonagent.md`、各 ToolProvider / ToolSupport
- **覆盖要求**: 至少覆盖 `sql_guard.check`、`semantic_model.search`、`domain_business_knowledge.search` 以及已存在中文样板的对齐检查

### 5. 依赖和集成点
- **外部依赖**: Spring AI `ToolDefinition`
- **内部依赖**: `AgentScopedToolProvider`、各 `ToolSupport`
- **集成方式**: provider 将 description 和 inputSchema 注入 `ToolDefinition.builder()`
- **配置来源**: `data-agent-management/src/main/resources/prompts/commonagent.md` 定义工具路由规则

### 6. 技术选型理由
- **为什么用这个方案**: 直接修改 provider/support 文案即可完成目标，影响面准确且不改变运行逻辑
- **优势**: 风险低、改动集中、与现有工具注册结构一致
- **劣势和风险**: 只改文案不跑行为测试；需确保中文说明与现有路由规则完全一致，避免误导模型

### 7. 关键风险点
- **边界条件**: `sql_guard.check` 同时支持 `SQL_VERIFY` 与 `DATA_PROFILE`，描述必须避免把 profile 说成默认步骤
- **一致性风险**: provider description、schema description、`commonagent.md` 三处边界不能冲突
- **维护风险**: 若遗漏某个注册工具，前后端展示和模型行为提示会出现中英混杂
