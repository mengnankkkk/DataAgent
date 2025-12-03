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

package com.alibaba.cloud.ai.dataagent.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Tavily 搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TavilySearchResult {

    /**
     * 原始搜索查询
     */
    private String query;

    /**
     * AI 生成的答案（如果请求）
     */
    private String answer;

    /**
     * 搜索结果列表
     */
    private List<SearchResultItem> results;

    /**
     * 图片列表
     */
    private List<String> images;

    /**
     * 响应时间（秒）
     */
    @JsonProperty("response_time")
    private Double responseTime;

    /**
     * 单个搜索结果项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResultItem {

        /**
         * 结果标题
         */
        private String title;

        /**
         * 结果 URL
         */
        private String url;

        /**
         * 内容摘要
         */
        private String content;

        /**
         * 原始内容（如果请求）
         */
        @JsonProperty("raw_content")
        private String rawContent;

        /**
         * 相关性分数
         */
        private Double score;

        /**
         * 发布日期
         */
        @JsonProperty("published_date")
        private String publishedDate;

    }

}
