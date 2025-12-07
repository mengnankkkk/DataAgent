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

import com.alibaba.cloud.ai.dataagent.entity.DataImportTask;
import com.alibaba.cloud.ai.dataagent.service.DataImportService;
import com.alibaba.cloud.ai.dataagent.vo.DataImportResponse;
import com.alibaba.cloud.ai.dataagent.vo.TaskStatusResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据导入控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/data-import")
@CrossOrigin(origins = "*")
@AllArgsConstructor
public class DataImportController {

    private final DataImportService dataImportService;

    /**
     * 上传Excel/CSV文件并启动导入任务
     *
     * @param agentId 智能体ID
     * @param file    上传的文件
     * @return 导入响应
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataImportResponse> uploadFile(@RequestParam("agentId") Integer agentId,
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("接收到文件上传请求, agentId: {}, fileName: {}", agentId, file.getOriginalFilename());

            String taskId = dataImportService.uploadAndImport(agentId, file);

            return ResponseEntity
                    .ok(DataImportResponse.ok("文件上传成功，正在后台处理", taskId, file.getOriginalFilename()));
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ResponseEntity.badRequest().body(DataImportResponse.error("文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
        try {
            log.info("查询任务状态: {}", taskId);

            DataImportTask task = dataImportService.getTaskStatus(taskId);
            TaskStatusResponse response = TaskStatusResponse.fromEntity(task);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查询任务状态失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 查询智能体的所有导入任务
     *
     * @param agentId 智能体ID
     * @return 任务列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskStatusResponse>> getTasksByAgentId(@RequestParam("agentId") Integer agentId) {
        try {
            log.info("查询智能体的导入任务: {}", agentId);

            List<DataImportTask> tasks = dataImportService.getTasksByAgentId(agentId);
            List<TaskStatusResponse> responses = tasks.stream().map(TaskStatusResponse::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("查询任务列表失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

}
