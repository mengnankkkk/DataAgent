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

import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchResult;
import com.alibaba.cloud.ai.dataagent.common.service.TavilySearchService;
import com.alibaba.cloud.ai.dataagent.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.alibaba.cloud.ai.dataagent.constant.Constant.INPUT_KEY;
import static com.alibaba.cloud.ai.dataagent.constant.Constant.WEB_SEARCH_NODE_OUTPUT;

/**
 * Web 搜索节点
 * 使用 Tavily API 执行网络搜索
 */
@Slf4j
@Component
public class WebSearchNode implements NodeAction {

    private final TavilySearchService tavilySearchService;

    @Autowired(required = false)
    public WebSearchNode(TavilySearchService tavilySearchService) {
        this.tavilySearchService = tavilySearchService;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        if (tavilySearchService == null) {
            log.warn("TavilySearchService is not available. Skipping web search.");
            return Map.of();
        }

        // 获取搜索查询
        String query = StateUtil.getStringValue(state, INPUT_KEY);
        log.info("Executing web search for query: {}", query);

        // 执行实际搜索以获取结果放入 State
        TavilySearchResult result = tavilySearchService.search(query);

        return Map.of(WEB_SEARCH_NODE_OUTPUT, result);
    }
}
