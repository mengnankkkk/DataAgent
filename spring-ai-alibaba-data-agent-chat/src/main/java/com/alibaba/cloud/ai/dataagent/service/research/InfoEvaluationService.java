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

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 信息充分性评估服务
 */
@Slf4j
@Service
@AllArgsConstructor
public class InfoEvaluationService {

	private final LlmService llmService;

	/**
	 * 评估信息是否充分
	 * @param question 用户问题
	 * @param results 已收集的搜索结果
	 * @param iteration 当前迭代次数
	 * @return true 表示信息充分，false 表示需要继续搜索
	 */
	public boolean evaluateSufficiency(String question, List<ResearchResult> results, int iteration) {
		log.info("评估信息充分性 - 问题: {}, 已收集结果数: {}, 迭代次数: {}", question, results.size(), iteration);

		// 快速规则判断
		if (!isInfoSufficientByRules(results)) {
			log.info("规则判断：信息不足");
			return false;
		}

		// LLM 深度判断
		try {
			String summary = summarizeResults(results);
			String prompt = PromptHelper.buildInfoSufficiencyPrompt(question, summary);

			String judgment = llmService.callUser(prompt)
				.blockLast()
				.getResult()
				.getOutput()
				.getText();

			boolean isSufficient = judgment.contains("信息充分") || judgment.contains("SUFFICIENT")
					|| judgment.toUpperCase().contains("SUFFICIENT");
			log.info("LLM判断结果: {}", isSufficient ? "信息充分" : "信息不足");

			return isSufficient;
		}
		catch (Exception e) {
			log.error("LLM判断失败，默认返回信息不足: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * 基于规则快速判断信息是否充分
	 */
	private boolean isInfoSufficientByRules(List<ResearchResult> results) {
		if (results == null || results.isEmpty()) {
			return false;
		}

		// 规则1：至少3个不同来源
		long distinctSources = results.stream()
			.map(this::extractDomain)
			.filter(domain -> domain != null && !domain.isEmpty())
			.distinct()
			.count();

		if (distinctSources < 3) {
			log.debug("不同来源数量不足: {} < 3", distinctSources);
			return false;
		}

		// 规则2：内容总长度 > 2000字
		int totalLength = results.stream().mapToInt(r -> r.getContent() != null ? r.getContent().length() : 0).sum();

		if (totalLength < 2000) {
			log.debug("内容总长度不足: {} < 2000", totalLength);
			return false;
		}

		// 规则3：平均相关性分数 > 0.7
		double avgScore = results.stream()
			.filter(r -> r.getScore() != null)
			.mapToDouble(ResearchResult::getScore)
			.average()
			.orElse(0.0);

		if (avgScore < 0.7) {
			log.debug("平均相关性分数不足: {} < 0.7", avgScore);
			return false;
		}

		log.debug("规则判断通过: 来源数={}, 内容长度={}, 平均分={}", distinctSources, totalLength, avgScore);
		return true;
	}

	/**
	 * 提取域名
	 */
	private String extractDomain(ResearchResult result) {
		try {
			if (result.getUrl() == null) {
				return "";
			}
			URI uri = new URI(result.getUrl());
			return uri.getHost();
		}
		catch (Exception e) {
			return "";
		}
	}

	/**
	 * 汇总搜索结果
	 */
	private String summarizeResults(List<ResearchResult> results) {
		return results.stream()
			.limit(10) // 限制最多10条结果用于摘要
			.map(r -> String.format("- %s\n  %s\n  (来源: %s)", r.getTitle(),
					r.getContent() != null && r.getContent().length() > 200
							? r.getContent().substring(0, 200) + "..." : r.getContent(),
					r.getUrl()))
			.collect(Collectors.joining("\n\n"));
	}

}
