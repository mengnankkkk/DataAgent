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

package com.alibaba.cloud.ai.dataagent.common.service.impl;

import com.alibaba.cloud.ai.dataagent.common.config.TavilyProperties;
import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchOptions;
import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchResult;
import com.alibaba.cloud.ai.dataagent.common.service.TavilySearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Tavily 搜索服务实现
 */
@Slf4j
public class TavilySearchServiceImpl implements TavilySearchService {

    private final TavilyProperties properties;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    public TavilySearchServiceImpl(TavilyProperties properties, RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    @Override
    public TavilySearchResult search(String query) {
        return search(query, null);
    }

    @Override
    public TavilySearchResult search(String query, TavilySearchOptions options) {
        log.info("执行 Tavily 搜索，查询: {}", query);

        try {
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(query, options);

            // 发送请求
            String response = restClient.post()
                    .uri("/search")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // 解析响应
            TavilySearchResult result = objectMapper.readValue(response, TavilySearchResult.class);
            log.info("Tavily 搜索成功，返回 {} 个结果", result.getResults() != null ? result.getResults().size() : 0);

            return result;
        } catch (Exception e) {
            log.error("Tavily 搜索失败: {}", e.getMessage(), e);
            throw new RuntimeException("Tavily 搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String query, TavilySearchOptions options) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);

        // 使用提供的选项或默认值
        if (options != null) {
            if (options.getMaxResults() != null) {
                body.put("max_results", options.getMaxResults());
            } else {
                body.put("max_results", properties.getDefaultMaxResults());
            }

            if (options.getSearchDepth() != null) {
                body.put("search_depth", options.getSearchDepth());
            } else {
                body.put("search_depth", properties.getDefaultSearchDepth());
            }

            if (options.getIncludeAnswer() != null) {
                body.put("include_answer", options.getIncludeAnswer());
            } else {
                body.put("include_answer", properties.isDefaultIncludeAnswer());
            }

            if (options.getIncludeRawContent() != null) {
                body.put("include_raw_content", options.getIncludeRawContent());
            } else {
                body.put("include_raw_content", properties.isDefaultIncludeRawContent());
            }

            if (options.getIncludeImages() != null) {
                body.put("include_images", options.getIncludeImages());
            }

            if (options.getIncludeDomains() != null && !options.getIncludeDomains().isEmpty()) {
                body.put("include_domains", options.getIncludeDomains());
            }

            if (options.getExcludeDomains() != null && !options.getExcludeDomains().isEmpty()) {
                body.put("exclude_domains", options.getExcludeDomains());
            }
        } else {
            // 使用默认值
            body.put("max_results", properties.getDefaultMaxResults());
            body.put("search_depth", properties.getDefaultSearchDepth());
            body.put("include_answer", properties.isDefaultIncludeAnswer());
            body.put("include_raw_content", properties.isDefaultIncludeRawContent());
        }

        return body;
    }

}
