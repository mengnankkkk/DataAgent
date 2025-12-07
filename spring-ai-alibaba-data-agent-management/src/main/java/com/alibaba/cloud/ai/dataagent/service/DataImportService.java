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

package com.alibaba.cloud.ai.dataagent.service;

import com.alibaba.cloud.ai.dataagent.entity.DataImportTask;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 数据导入服务接口
 */
public interface DataImportService {

    /**
     * 上传文件并启动异步导入任务
     *
     * @param agentId 智能体ID
     * @param file    上传的文件
     * @return 任务ID
     */
    String uploadAndImport(Integer agentId, MultipartFile file);

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务信息
     */
    DataImportTask getTaskStatus(String taskId);

    /**
     * 查询智能体的所有导入任务
     *
     * @param agentId 智能体ID
     * @return 任务列表
     */
    List<DataImportTask> getTasksByAgentId(Integer agentId);

    /**
     * 执行数据导入（异步方法）
     *
     * @param taskId 任务ID
     */
    void executeImport(String taskId);

}
