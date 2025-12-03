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

package com.alibaba.cloud.ai.dataagent.node;

import com.alibaba.cloud.ai.dataagent.common.dto.ResearchResult;
import com.alibaba.cloud.ai.dataagent.common.dto.TavilySearchResult;
import com.alibaba.cloud.ai.dataagent.common.service.TavilySearchService;
import com.alibaba.cloud.ai.dataagent.prompt.PromptHelper;
import com.alibaba.cloud.ai.dataagent.service.llm.LlmService;
import com.alibaba.cloud.ai.dataagent.service.research.InfoEvaluationService;
import com.alibaba.cloud.ai.dataagent.util.StateUtil;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.dataagent.constant.Constant.*;

/**
 * 深度研究节点
 * 实现迭代式搜索：搜索 -> 阅读 -> 判断 -> (不足)生成新关键词 -> (充分)汇总
 */
@Slf4j
@Component
public class DeepResearchNode implements NodeAction {

	private static final int MAX_ITERATIONS = 3;

	private static final int MIN_RESULTS_FOR_SUFFICIENCY = 5;

	private final LlmService llmService;

	private final TavilySearchService tavilySearchService;

	private final InfoEvaluationService infoEvaluationService;

	@Autowired(required = false)
	public DeepResearchNode(LlmService llmService, TavilySearchService tavilySearchService,
			InfoEvaluationService infoEvaluationService) {
		this.llmService = llmService;
		this.tavilySearchService = tavilySearchService;
		this.infoEvaluationService = infoEvaluationService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		if (tavilySearchService == null) {
			log.warn("TavilySearchService 不可用，跳过深度研究");
			return Map.of(RESEARCH_INFO_SUFFICIENT, false);
		}

		// 1. 获取当前状态
		String question = StateUtil.getStringValue(state, INPUT_KEY);
		Integer currentIteration = StateUtil.getObjectValue(state, RESEARCH_ITERATION_COUNT, Integer.class, 0);
		List<ResearchResult> existingResults = StateUtil.getObjectValue(state, RESEARCH_RESULTS, List.class,
				new ArrayList<>());

		log.info("开始深度研究 - 问题: {}, 当前迭代: {}/{}", question, currentIteration + 1, MAX_ITERATIONS);

		// 2. 生成搜索关键词
		List<String> keywords = generateKeywords(question, existingResults, currentIteration);
		log.info("生成搜索关键词: {}", keywords);

		// 3. 执行搜索并收集结果
		List<ResearchResult> newResults = executeSearch(keywords);
		log.info("搜索完成，获得 {} 条新结果", newResults.size());

		// 4. 合并结果
		List<ResearchResult> allResults = mergeResults(existingResults, newResults);

		// 5. 判断信息是否充分
		boolean isSufficient = evaluateInfoSufficiency(question, allResults, currentIteration + 1);
		log.info("信息充分性评估: {}, 当前结果总数: {}", isSufficient ? "充分" : "不足", allResults.size());

		// 6. 更新状态
		Map<String, Object> updates = new HashMap<>();
		updates.put(RESEARCH_ITERATION_COUNT, currentIteration + 1);
		updates.put(RESEARCH_KEYWORDS, keywords);
		updates.put(RESEARCH_RESULTS, allResults);
		updates.put(RESEARCH_INFO_SUFFICIENT, isSufficient);

		return updates;
	}

	/**
	 * 生成搜索关键词
	 * @param question 用户问题
	 * @param existingResults 已有搜索结果
	 * @param iteration 当前迭代次数
	 * @return 关键词列表
	 */
	private List<String> generateKeywords(String question, List<ResearchResult> existingResults, int iteration) {
		try {
			String prompt;
			if (iteration == 0) {
				// 初次搜索：从问题中提取关键词
				prompt = PromptHelper.buildKeywordExtractionPrompt(question);
			}
			else {
				// 后续搜索：根据已有结果精炼关键词
				String summary = summarizeResults(existingResults);
				prompt = PromptHelper.buildKeywordRefinementPrompt(question, summary);
			}

			String response = llmService.callUser(prompt).blockLast().getResult().getOutput().getText();

			// 解析关键词（假设 LLM 返回逗号分隔的关键词）
			return Arrays.stream(response.split("[,，\n]"))
				.map(String::trim)
				.filter(k -> !k.isEmpty())
				.limit(5)
				.collect(Collectors.toList());
		}
		catch (Exception e) {
			log.error("关键词生成失败: {}", e.getMessage(), e);
			// 降级：使用问题本身作为关键词
			return List.of(question);
		}
	}

	/**
	 * 执行搜索
	 * @param keywords 关键词列表
	 * @return 搜索结果列表
	 */
	private List<ResearchResult> executeSearch(List<String> keywords) {
		List<ResearchResult> results = new ArrayList<>();

		for (String keyword : keywords) {
			try {
				TavilySearchResult searchResult = tavilySearchService.search(keyword);

				if (searchResult.getResults() != null) {
					// 转换 TavilySearchResult.Result 为 ResearchResult
					List<ResearchResult> converted = searchResult.getResults()
						.stream()
						.map(r -> ResearchResult.builder()
							.title(r.getTitle())
							.url(r.getUrl())
							.content(r.getContent())
							.score(r.getScore())
							.publishedDate(r.getPublishedDate())
							.source(extractDomain(r.getUrl()))
							.searchKeyword(keyword)
							.build())
						.collect(Collectors.toList());

					results.addAll(converted);
				}
			}
			catch (Exception e) {
				log.error("搜索关键词 '{}' 失败: {}", keyword, e.getMessage(), e);
			}
		}

		return results;
	}

	/**
	 * 合并搜索结果（去重）
	 */
	private List<ResearchResult> mergeResults(List<ResearchResult> existing, List<ResearchResult> newResults) {
		Set<String> existingUrls = existing.stream().map(ResearchResult::getUrl).collect(Collectors.toSet());

		List<ResearchResult> merged = new ArrayList<>(existing);

		// 只添加未重复的新结果
		for (ResearchResult result : newResults) {
			if (!existingUrls.contains(result.getUrl())) {
				merged.add(result);
			}
		}

		return merged;
	}

	/**
	 * 评估信息是否充分
	 */
	private boolean evaluateInfoSufficiency(String question, List<ResearchResult> results, int iteration) {
		// 达到最大迭代次数，强制结束
		if (iteration >= MAX_ITERATIONS) {
			log.info("已达到最大迭代次数 {}，强制结束研究", MAX_ITERATIONS);
			return true;
		}

		// 结果数量太少，继续搜索
		if (results.size() < MIN_RESULTS_FOR_SUFFICIENCY) {
			log.info("结果数量不足（{} < {}），继续搜索", results.size(), MIN_RESULTS_FOR_SUFFICIENCY);
			return false;
		}

		// 使用 InfoEvaluationService 进行深度评估
		return infoEvaluationService.evaluateSufficiency(question, results, iteration);
	}

	/**
	 * 汇总搜索结果
	 */
	private String summarizeResults(List<ResearchResult> results) {
		if (results == null || results.isEmpty()) {
			return "无搜索结果";
		}

		return results.stream()
			.limit(10)
			.map(r -> String.format("- %s: %s", r.getTitle(),
					r.getContent() != null && r.getContent().length() > 100 ? r.getContent().substring(0, 100) + "..."
							: r.getContent()))
			.collect(Collectors.joining("\n"));
	}

	/**
	 * 从 URL 提取域名
	 */
	private String extractDomain(String url) {
		if (url == null || url.isEmpty()) {
			return "unknown";
		}

		try {
			String domain = url.replaceFirst("^https?://", "").split("/")[0];
			return domain;
		}
		catch (Exception e) {
			return "unknown";
		}
	}

}
