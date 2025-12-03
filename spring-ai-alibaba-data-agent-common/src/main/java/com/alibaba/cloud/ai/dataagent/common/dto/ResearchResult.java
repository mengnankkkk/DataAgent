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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 深度研究单条结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchResult {

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
	 * 相关性分数
	 */
	private Double score;

	/**
	 * 发布日期
	 */
	private String publishedDate;

	/**
	 * 来源域名
	 */
	private String source;

	/**
	 * 搜索关键词（标记是通过哪个关键词搜索到的）
	 */
	private String searchKeyword;

}
