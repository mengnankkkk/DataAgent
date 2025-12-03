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

package com.alibaba.cloud.ai.dataagent.common.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Tavily 搜索 API 配置属性
 */
@Getter
@Setter
public class TavilyProperties {

    /**
     * 是否启用 Tavily 搜索
     */
    private boolean enabled = false;

    /**
     * Tavily API Key
     */
    private String apiKey;

    /**
     * Tavily API 基础 URL
     */
    private String baseUrl = "https://api.tavily.com";

    /**
     * 默认最大结果数
     */
    private int defaultMaxResults = 5;

    /**
     * 默认搜索深度: basic 或 advanced
     */
    private String defaultSearchDepth = "basic";

    /**
     * 请求超时时间（秒）
     */
    private int timeout = 30;

    /**
     * 是否默认包含 AI 生成的答案
     */
    private boolean defaultIncludeAnswer = true;

    /**
     * 是否默认包含原始内容
     */
    private boolean defaultIncludeRawContent = false;

}
