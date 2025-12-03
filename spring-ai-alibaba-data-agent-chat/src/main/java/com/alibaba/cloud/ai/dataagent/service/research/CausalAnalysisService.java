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

package com.alibaba.cloud.ai.dataagent.service.research;

import com.alibaba.cloud.ai.dataagent.common.dto.ResearchResult;
import com.alibaba.cloud.ai.dataagent.prompt.PromptHelper;
import com.alibaba.cloud.ai.dataagent.service.llm.LlmService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 因果关联分析服务
 * 负责将外部研究结果与内部SQL数据进行对齐分析
 */
@Slf4j
@Service
@AllArgsConstructor
public class CausalAnalysisService {

	private final LlmService llmService;

	/**
	 * 分析外部研究与SQL数据之间的因果关系
	 * @param question 用户问题
	 * @param researchResults 外部研究结果
	 * @param sqlData SQL查询结果数据
	 * @return 因果分析报告
	 */
	public String analyzeCausality(String question, List<ResearchResult> researchResults,
			List<Map<String, Object>> sqlData) {
		log.info("开始因果关联分析 - 问题: {}, 研究结果数: {}, SQL数据行数: {}", question, researchResults.size(),
				sqlData != null ? sqlData.size() : 0);

		try {
			// 1. 汇总外部研究结果
			String researchSummary = summarizeResearch(researchResults);

			// 2. 汇总SQL数据
			String dataSummary = summarizeSqlData(sqlData);

			// 3. 使用LLM进行因果对齐分析
			String prompt = PromptHelper.buildCausalAlignmentPrompt(question, dataSummary, researchSummary);

			String analysis = llmService.callUser(prompt).blockLast().getResult().getOutput().getText();

			log.info("因果分析完成");
			return analysis;
		}
		catch (Exception e) {
			log.error("因果分析失败: {}", e.getMessage(), e);
			return "因果分析过程中发生错误: " + e.getMessage();
		}
	}

	/**
	 * 汇总外部研究结果（按主题分类）
	 */
	private String summarizeResearch(List<ResearchResult> results) {
		if (results == null || results.isEmpty()) {
			return "无外部研究数据";
		}

		// 按主题聚类
		Map<String, List<ResearchResult>> clustered = clusterByTopic(results);

		StringBuilder summary = new StringBuilder();
		summary.append("【外部研究发现】\n\n");

		for (Map.Entry<String, List<ResearchResult>> entry : clustered.entrySet()) {
			summary.append("## ").append(entry.getKey()).append("\n");
			for (ResearchResult result : entry.getValue()) {
				summary.append("- **").append(result.getTitle()).append("**\n");
				String content = result.getContent();
				if (content != null && content.length() > 300) {
					content = content.substring(0, 300) + "...";
				}
				summary.append("  ").append(content).append("\n");
				summary.append("  (来源: ").append(result.getUrl()).append(")\n\n");
			}
		}

		return summary.toString();
	}

	/**
	 * 按主题对搜索结果进行聚类
	 */
	private Map<String, List<ResearchResult>> clusterByTopic(List<ResearchResult> results) {
		return results.stream().collect(Collectors.groupingBy(this::extractTopic));
	}

	/**
	 * 从搜索结果中提取主题
	 * 简化版：基于关键词匹配
	 */
	private String extractTopic(ResearchResult result) {
		String title = result.getTitle() != null ? result.getTitle().toLowerCase() : "";
		String content = result.getContent() != null ? result.getContent().toLowerCase() : "";

		// 天气相关
		if (title.contains("天气") || title.contains("气候") || title.contains("weather") || content.contains("暴雨")
				|| content.contains("寒潮") || content.contains("台风")) {
			return "天气因素";
		}

		// 竞品相关
		if (title.contains("竞品") || title.contains("促销") || title.contains("活动") || content.contains("打折")
				|| content.contains("降价") || content.contains("优惠")) {
			return "竞争态势";
		}

		// 物流相关
		if (title.contains("物流") || title.contains("配送") || title.contains("快递") || content.contains("延迟")
				|| content.contains("故障")) {
			return "物流影响";
		}

		// 政策相关
		if (title.contains("政策") || title.contains("法规") || title.contains("监管") || content.contains("规定")) {
			return "政策因素";
		}

		// 市场趋势
		if (title.contains("趋势") || title.contains("市场") || title.contains("行业") || content.contains("增长")
				|| content.contains("下降")) {
			return "市场趋势";
		}

		// 默认分类
		return "其他因素";
	}

	/**
	 * 汇总SQL数据
	 */
	private String summarizeSqlData(List<Map<String, Object>> sqlData) {
		if (sqlData == null || sqlData.isEmpty()) {
			return "无SQL查询数据";
		}

		StringBuilder summary = new StringBuilder();
		summary.append("【内部数据分析】\n\n");

		// 显示前10行数据
		int displayCount = Math.min(10, sqlData.size());
		for (int i = 0; i < displayCount; i++) {
			Map<String, Object> row = sqlData.get(i);
			summary.append("记录 ").append(i + 1).append(": ");
			summary.append(row.entrySet()
				.stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(", ")));
			summary.append("\n");
		}

		if (sqlData.size() > 10) {
			summary.append("... (共 ").append(sqlData.size()).append(" 行数据)\n");
		}

		return summary.toString();
	}

}
