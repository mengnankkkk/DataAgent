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

package com.alibaba.cloud.ai.dataagent.service.impl;

import com.alibaba.cloud.ai.dataagent.config.file.FileStorageProperties;
import com.alibaba.cloud.ai.dataagent.entity.DataImportMetadata;
import com.alibaba.cloud.ai.dataagent.entity.DataImportTask;
import com.alibaba.cloud.ai.dataagent.mapper.DataImportMetadataMapper;
import com.alibaba.cloud.ai.dataagent.mapper.DataImportTaskMapper;
import com.alibaba.cloud.ai.dataagent.service.DataImportService;
import com.alibaba.cloud.ai.dataagent.service.ImportProgressService;
import com.alibaba.cloud.ai.dataagent.service.file.FileStorageService;
import com.alibaba.cloud.ai.dataagent.util.FileTypeValidator;
import com.alibaba.cloud.ai.dataagent.util.DatabaseTypeDetector;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据导入服务实现类
 */
@Slf4j
@Service
@AllArgsConstructor
public class DataImportServiceImpl implements DataImportService {

    private static final int BATCH_SIZE = 1000; // 批量插入大小

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    private static final Set<String> ALLOWED_FILE_TYPES = Set.of("xlsx", "xls", "csv");

    private final DataImportTaskMapper taskMapper;

    private final DataImportMetadataMapper metadataMapper;

    private final FileStorageService fileStorageService;

    private final FileStorageProperties fileStorageProperties;

    private final DataSource dataSource;

    private final ImportProgressService progressService;

    private final DatabaseTypeDetector databaseTypeDetector;

    @Override
    @Transactional
    public String uploadAndImport(Integer agentId, MultipartFile file) {
        Assert.notNull(agentId, "智能体ID不能为空");
        Assert.notNull(file, "文件不能为空");
        Assert.isTrue(!file.isEmpty(), "文件不能为空");

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        Assert.notNull(originalFilename, "文件名不能为空");

        String fileExtension = getFileExtension(originalFilename);
        if (!ALLOWED_FILE_TYPES.contains(fileExtension.toLowerCase())) {
            throw new RuntimeException("不支持的文件类型，仅支持: " + ALLOWED_FILE_TYPES);
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("文件大小超限，最大允许: " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // Magic Number校验真实文件格式
        try {
            // 读取文件前8个字节用于格式检测
            byte[] fileBytes = file.getBytes();
            byte[] header = new byte[Math.min(8, fileBytes.length)];
            System.arraycopy(fileBytes, 0, header, 0, header.length);

            String detectedType = FileTypeValidator.validateAndDetectType(header, originalFilename);
            log.info("文件格式校验通过: 声明类型={}, 实际类型={}", fileExtension, detectedType);
        } catch (Exception e) {
            throw new RuntimeException("文件格式校验失败: " + e.getMessage(), e);
        }

        try {
            // 存储文件
            String filePath = fileStorageService.storeFile(file, "data-imports");
            log.info("文件已存储: {}", filePath);

            // 创建任务记录
            String taskId = UUID.randomUUID().toString();
            DataImportTask task = DataImportTask.builder().id(taskId).agentId(agentId).fileName(originalFilename)
                    .filePath(filePath).fileSize(file.getSize()).fileType(fileExtension).status("pending")
                    .createTime(LocalDateTime.now()).build();

            taskMapper.insert(task);
            log.info("创建导入任务: {}", taskId);

            // 异步执行导入
            executeImport(taskId);

            return taskId;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public DataImportTask getTaskStatus(String taskId) {
        Assert.hasText(taskId, "任务ID不能为空");
        DataImportTask task = taskMapper.findById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        return task;
    }

    @Override
    public List<DataImportTask> getTasksByAgentId(Integer agentId) {
        Assert.notNull(agentId, "智能体ID不能为空");
        return taskMapper.findByAgentId(agentId);
    }

    @Override
    @Async("dataImportExecutor")
    public void executeImport(String taskId) {
        log.info("开始执行导入任务: {}", taskId);
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 更新任务状态为处理中
            taskMapper.updateToProcessing(taskId, startTime);
            progressService.sendParsing(taskId, "开始解析文件...");

            // 获取任务信息
            DataImportTask task = taskMapper.findById(taskId);
            if (task == null) {
                throw new RuntimeException("任务不存在: " + taskId);
            }

            // 获取文件路径（相对路径）并转换为绝对路径
            String relativePath = task.getFilePath();
            String absolutePath = Paths.get(fileStorageProperties.getPath(), relativePath).toAbsolutePath().toString();
            File file = new File(absolutePath);
            if (!file.exists()) {
                throw new RuntimeException("文件不存在: " + absolutePath + " (相对路径: " + relativePath + ")");
            }
            log.info("找到文件: {}", absolutePath);

            // 生成目标表名（物理表名，保证唯一性）
            String physicalTableName = generateTableName(task.getFileName());
            log.info("物理表名: {}", physicalTableName);

            // 提取逻辑表名（来自文件名，用于显示）
            String logicalTableName = extractLogicalTableName(task.getFileName());
            log.info("逻辑表名: {}", logicalTableName);

            // 解析文件并导入数据
            ImportResult result = importData(taskId, file, physicalTableName, task.getFileType());

            // 动态检测数据库类型
            String dbType = databaseTypeDetector.getDatabaseType();
            log.info("检测到数据库类型: {}", dbType);

            // 保存元数据
            DataImportMetadata metadata = DataImportMetadata.builder()
                    .taskId(taskId)
                    .tableName(physicalTableName)
                    .logicalTableName(logicalTableName)
                    .columnCount(result.columnCount)
                    .rowCount(result.rowCount)
                    .datasourceType(dbType)
                    .createTime(LocalDateTime.now())
                    .build();
            metadataMapper.insert(metadata);

            // 更新任务状态为完成
            LocalDateTime endTime = LocalDateTime.now();
            taskMapper.updateToCompleted(taskId, endTime, result.rowCount, result.rowCount, physicalTableName);
            progressService.sendCompleted(taskId, result.rowCount, logicalTableName);

            log.info("导入任务完成: {}, 物理表名: {}, 逻辑表名: {}, 行数: {}",
                    taskId, physicalTableName, logicalTableName, result.rowCount);
        } catch (Exception e) {
            log.error("导入任务失败: " + taskId, e);
            LocalDateTime endTime = LocalDateTime.now();
            taskMapper.updateToFailed(taskId, endTime, e.getMessage());
            progressService.sendFailed(taskId, e.getMessage());
        }
    }

    /**
     * 导入数据到数据库
     */
    private ImportResult importData(String taskId, File file, String tableName, String fileType) throws Exception {
        List<Map<Integer, String>> headerData = new ArrayList<>();
        List<List<String>> dataRows = new ArrayList<>();

        // 使用EasyExcel读取文件，headRowNumber(0)表示没有表头行，所有行都作为数据读取
        EasyExcel.read(file, new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                // EasyExcel的行索引从0开始
                int rowIndex = context.readRowHolder().getRowIndex();

                if (rowIndex == 0) {
                    // 第一行作为表头
                    headerData.add(data);
                    log.debug("读取表头: {}", data);
                } else {
                    // 数据行
                    List<String> row = new ArrayList<>();
                    // 按列索引顺序添加数据
                    for (int i = 0; i < data.size(); i++) {
                        row.add(data.get(i));
                    }
                    dataRows.add(row);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("文件解析完成，表头行数: {}, 数据行数: {}", headerData.size(), dataRows.size());
            }
        }).headRowNumber(0).sheet().doRead(); // headRowNumber(0)表示没有表头，从第0行开始读取

        // 获取表头
        if (headerData.isEmpty()) {
            throw new RuntimeException("文件为空，无法读取表头");
        }

        Map<Integer, String> headers = headerData.get(0);
        int columnCount = headers.size();

        if (columnCount == 0) {
            throw new RuntimeException("文件表头为空或格式不正确");
        }

        log.info("解析到 {} 列，{} 行数据", columnCount, dataRows.size());

        // 创建表并获取处理后的列名
        progressService.sendCreatingTable(taskId, tableName);
        List<String> columnNames = createTable(tableName, columnCount, headers);

        // 批量插入数据
        int totalRows = batchInsertData(taskId, tableName, columnNames, dataRows);

        return new ImportResult(columnCount, totalRows);
    }

    /**
     * 创建表并返回处理后的列名列表
     */
    private List<String> createTable(String tableName, int columnCount, Map<Integer, String> headers) throws Exception {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (");
        ddl.append("`id` BIGINT AUTO_INCREMENT PRIMARY KEY");

        // 保存处理后的列名
        List<String> columnNames = new ArrayList<>();

        for (int i = 0; i < columnCount; i++) {
            String columnName = headers.get(i);
            // 如果列名为空或null，使用默认列名
            if (columnName == null || columnName.trim().isEmpty()) {
                columnName = "column_" + (i + 1);
            }
            // 清理列名，移除特殊字符
            columnName = columnName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
            // 确保列名不为空
            if (columnName.isEmpty()) {
                columnName = "column_" + (i + 1);
            }
            columnNames.add(columnName);
            ddl.append(", `").append(columnName).append("` TEXT");
        }

        ddl.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(ddl.toString());
            log.info("表创建成功: {}, 列名: {}", tableName, columnNames);
        }

        return columnNames;
    }

    /**
     * 批量插入数据
     */
    private int batchInsertData(String taskId, String tableName, List<String> columnNames, List<List<String>> dataRows)
            throws Exception {
        if (dataRows.isEmpty()) {
            return 0;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO `").append(tableName).append("` (");

        // 使用实际的列名
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0)
                sql.append(", ");
            sql.append("`").append(columnNames.get(i)).append("`");
        }

        sql.append(") VALUES (");
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0)
                sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        int totalInserted = 0;

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            conn.setAutoCommit(false);

            for (int rowIndex = 0; rowIndex < dataRows.size(); rowIndex++) {
                List<String> row = dataRows.get(rowIndex);

                for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {
                    String value = colIndex < row.size() ? row.get(colIndex) : null;
                    pstmt.setString(colIndex + 1, value);
                }

                pstmt.addBatch();

                // 每BATCH_SIZE条执行一次
                if ((rowIndex + 1) % BATCH_SIZE == 0) {
                    int[] results = pstmt.executeBatch();
                    conn.commit();
                    totalInserted += results.length;
                    progressService.sendInserting(taskId, dataRows.size(), totalInserted);
                    log.info("已插入 {} 行数据", totalInserted);
                }
            }

            // 插入剩余数据
            int[] results = pstmt.executeBatch();
            conn.commit();
            totalInserted += results.length;

            log.info("数据插入完成，总计: {} 行", totalInserted);
        }

        return totalInserted;
    }

    /**
     * 生成表名
     */
    private String generateTableName(String fileName) {
        // 移除文件扩展名
        String baseName = fileName;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            baseName = fileName.substring(0, lastDotIndex);
        }

        // 清理文件名，只保留字母、数字、下划线和中文
        baseName = baseName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");

        // 如果清理后为空，使用默认名称
        if (baseName.isEmpty()) {
            baseName = "data";
        }

        // 生成唯一表名
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "import_" + baseName + "_" + timestamp + "_" + uuid;
    }

    /**
     * 提取逻辑表名（用于显示）
     * 从文件名中提取，保留中文和特殊字符
     */
    private String extractLogicalTableName(String fileName) {
        // 移除文件扩展名
        String baseName = fileName;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            baseName = fileName.substring(0, lastDotIndex);
        }

        // 如果为空，使用默认名称
        if (baseName.isEmpty()) {
            baseName = "导入数据";
        }

        return baseName;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 导入结果
     */
    private static class ImportResult {

        final int columnCount;

        final int rowCount;

        ImportResult(int columnCount, int rowCount) {
            this.columnCount = columnCount;
            this.rowCount = rowCount;
        }

    }

}
