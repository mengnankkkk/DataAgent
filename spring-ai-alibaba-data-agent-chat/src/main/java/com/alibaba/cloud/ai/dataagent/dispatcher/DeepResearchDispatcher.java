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

package com.alibaba.cloud.ai.dataagent.dispatcher;

import com.alibaba.cloud.ai.dataagent.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import lombok.extern.slf4j.Slf4j;

import static com.alibaba.cloud.ai.dataagent.constant.Constant.*;

/**
 * 深度研究调度器
 * 决定是否继续迭代搜索或进入下一阶段
 */
@Slf4j
public class DeepResearchDispatcher implements EdgeAction {

	private static final int MAX_ITERATIONS = 3;

	@Override
	public String apply(OverAllState state) throws Exception {
		// 获取当前状态
		Boolean isInfoSufficient = StateUtil.getObjectValue(state, RESEARCH_INFO_SUFFICIENT, Boolean.class, false);
		Integer iterationCount = StateUtil.getObjectValue(state, RESEARCH_ITERATION_COUNT, Integer.class, 0);

		log.info("深度研究调度 - 信息充分: {}, 迭代次数: {}/{}", isInfoSufficient, iterationCount, MAX_ITERATIONS);

		// 1. 如果信息充分，进入因果分析节点
		if (isInfoSufficient) {
			log.info("信息收集充分，进入因果分析阶段");
			return CAUSAL_ANALYSIS_NODE;
		}

		// 2. 如果未达到最大迭代次数，继续迭代
		if (iterationCount < MAX_ITERATIONS) {
			log.info("信息不足，继续第 {} 轮搜索", iterationCount + 1);
			return DEEP_RESEARCH_NODE;
		}

		// 3. 达到最大迭代次数但信息仍不足，强制进入因果分析（使用现有信息）
		log.warn("已达到最大迭代次数，使用现有信息进入因果分析");
		return CAUSAL_ANALYSIS_NODE;
	}

}
