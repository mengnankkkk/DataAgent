/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.dataagent.node;

import com.alibaba.cloud.ai.dataagent.common.dto.ResearchResult;
import com.alibaba.cloud.ai.dataagent.service.research.CausalAnalysisService;
import com.alibaba.cloud.ai.dataagent.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.dataagent.constant.Constant.*;

/**
 * 因果分析节点
 * 将外部研究结果与内部 SQL 数据进行因果关联分析
 */
@Slf4j
@Component
@AllArgsConstructor
public class CausalAnalysisNode implements NodeAction {

	private final CausalAnalysisService causalAnalysisService;

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		// 获取输入
		String question = StateUtil.getStringValue(state, INPUT_KEY);
		List<ResearchResult> researchResults = StateUtil.getObjectValue(state, RESEARCH_RESULTS, List.class);
		List<Map<String, Object>> sqlData = StateUtil.getObjectValue(state, SQL_EXECUTE_NODE_OUTPUT, List.class);

		log.info("开始因果分析 - 问题: {}, 研究结果数: {}, SQL数据行数: {}", question,
				researchResults != null ? researchResults.size() : 0, sqlData != null ? sqlData.size() : 0);

		// 执行因果分析
		String analysis = causalAnalysisService.analyzeCausality(question, researchResults, sqlData);

		log.info("因果分析完成，生成报告长度: {} 字符", analysis.length());

		// 返回分析结果
		return Map.of(CAUSAL_ANALYSIS_OUTPUT, analysis);
	}

}
