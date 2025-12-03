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

import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchResult;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.dataagent.util.StateUtil;
import lombok.extern.slf4j.Slf4j;

import static com.alibaba.cloud.ai.dataagent.constant.Constant.*;
import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * 根据 Web 搜索结果决定下一个节点的分发器
 */
@Slf4j
public class WebSearchDispatcher implements EdgeAction {

	@Override
	public String apply(OverAllState state) throws Exception {
		// 获取 Web 搜索结果
		TavilySearchResult searchResult = StateUtil.getObjectValue(state, WEB_SEARCH_NODE_OUTPUT,
				TavilySearchResult.class);

		if (searchResult == null) {
			log.warn("Web search result is null, ending conversation");
			return END;
		}

		// 检查搜索结果是否有效
		boolean hasResults = searchResult.getResults() != null && !searchResult.getResults().isEmpty();
		boolean hasAnswer = searchResult.getAnswer() != null && !searchResult.getAnswer().trim().isEmpty();

		if (!hasResults && !hasAnswer) {
			log.warn("Web search returned no results or answer, ending conversation");
			return END;
		}

		log.info("Web search completed successfully with {} results, proceeding to query enhance",
				hasResults ? searchResult.getResults().size() : 0);

		// Web 搜索成功后，继续到查询增强节点
		return QUERY_ENHANCE_NODE;
	}

}
