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

import java.util.List;

/**
 * 深度研究汇总结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchSummary {

	/**
	 * 用户问题
	 */
	private String question;

	/**
	 * 总迭代次数
	 */
	private Integer iterationCount;

	/**
	 * 所有搜索结果
	 */
	private List<ResearchResult> allResults;

	/**
	 * 信息是否充分
	 */
	private Boolean isInfoSufficient;

	/**
	 * 研究摘要文本
	 */
	private String summary;

	/**
	 * 主题分类（key: 主题, value: 该主题的结果列表）
	 */
	private java.util.Map<String, List<ResearchResult>> topicClusters;

}
