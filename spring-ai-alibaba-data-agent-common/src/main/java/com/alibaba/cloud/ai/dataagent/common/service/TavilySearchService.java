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

package com.alibaba.cloud.ai.dataagent.common.service;

import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchOptions;
import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchResult;

/**
 * Tavily 搜索服务接口
 */
public interface TavilySearchService {

    /**
     * 执行基本搜索
     * 
     * @param query 搜索查询
     * @return 搜索结果
     */
    TavilySearchResult search(String query);

    /**
     * 执行高级搜索
     * 
     * @param query   搜索查询
     * @param options 搜索选项
     * @return 搜索结果
     */
    TavilySearchResult search(String query, TavilySearchOptions options);

}
