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

package com.alibaba.cloud.ai.dataagent.controller;

import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchOptions;
import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchResult;
import com.alibaba.cloud.ai.dataagent.common.service.TavilySearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Tavily 搜索测试控制器
 * 用于演示和测试 Tavily 搜索功能
 */
@Slf4j
@RestController
@RequestMapping("/api/tavily")
@Tag(name = "Tavily 搜索", description = "Tavily 网络搜索 API")
@ConditionalOnBean(TavilySearchService.class)
public class TavilySearchController {

    @Autowired(required = false)
    private TavilySearchService tavilySearchService;

    /**
     * 基本搜索
     */
    @GetMapping("/search")
    @Operation(summary = "基本搜索", description = "使用默认配置执行搜索")
    public ResponseEntity<TavilySearchResult> search(@RequestParam String query) {
        if (tavilySearchService == null) {
            return ResponseEntity.status(503).build();
        }

        try {
            TavilySearchResult result = tavilySearchService.search(query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("搜索失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 高级搜索
     */
    @PostMapping("/search/advanced")
    @Operation(summary = "高级搜索", description = "使用自定义选项执行搜索")
    public ResponseEntity<TavilySearchResult> advancedSearch(@RequestParam String query,
            @RequestBody(required = false) TavilySearchOptions options) {
        if (tavilySearchService == null) {
            return ResponseEntity.status(503).build();
        }

        try {
            TavilySearchResult result = tavilySearchService.search(query, options);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("高级搜索失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
