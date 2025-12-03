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

package com.alibaba.cloud.ai.dataagent.tool;

import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchOptions;
import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchResult;
import com.alibaba.cloud.ai.dataagent.common.service.TavilySearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Tavily 搜索工具定义
 */
@Configuration
@Slf4j
@AllArgsConstructor
public class TavilySearchTools {

    private final TavilySearchService tavilySearchService;

    @Bean
    @Description("使用 Tavily API 执行网络搜索")
    public Function<TavilySearchRequest, TavilySearchResult> tavilySearch() {
        return request -> {
            log.info("Calling Tavily Search Tool with query: {}", request.query);
            TavilySearchOptions options = new TavilySearchOptions();
            options.setMaxResults(request.maxResults);
            options.setSearchDepth(request.searchDepth);
            options.setIncludeAnswer(request.includeAnswer);
            options.setIncludeRawContent(request.includeRawContent);
            return tavilySearchService.search(request.query, options);
        };
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("Tavily 搜索请求参数")
    public static class TavilySearchRequest {

        @JsonPropertyDescription("搜索查询关键词")
        @JsonProperty(required = true)
        private String query;

        @JsonPropertyDescription("最大结果数量 (默认: 5)")
        private Integer maxResults;

        @JsonPropertyDescription("搜索深度: 'basic' 或 'advanced' (默认: 'basic')")
        private String searchDepth;

        @JsonPropertyDescription("是否包含 AI 生成的答案 (默认: true)")
        private Boolean includeAnswer;

        @JsonPropertyDescription("是否包含原始内容 (默认: false)")
        private Boolean includeRawContent;

    }

}
