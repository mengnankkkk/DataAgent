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

package com.alibaba.cloud.ai.dataagent.mapper;

import com.alibaba.cloud.ai.dataagent.entity.DataImportTask;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据导入任务Mapper接口
 */
@Mapper
public interface DataImportTaskMapper {

    @Insert("""
            INSERT INTO data_import_task (id, agent_id, file_name, file_path, file_size, file_type,
                                          target_table_name, status, total_rows, processed_rows,
                                          error_message, start_time, end_time, create_time)
            VALUES (#{id}, #{agentId}, #{fileName}, #{filePath}, #{fileSize}, #{fileType},
                    #{targetTableName}, #{status}, #{totalRows}, #{processedRows},
                    #{errorMessage}, #{startTime}, #{endTime}, #{createTime})
            """)
    int insert(DataImportTask task);

    @Select("""
            SELECT * FROM data_import_task WHERE id = #{id}
            """)
    DataImportTask findById(String id);

    @Select("""
            SELECT * FROM data_import_task WHERE agent_id = #{agentId} ORDER BY create_time DESC
            """)
    List<DataImportTask> findByAgentId(Integer agentId);

    @Update("""
            UPDATE data_import_task
            SET status = 'processing', start_time = #{startTime}
            WHERE id = #{id}
            """)
    int updateToProcessing(@Param("id") String id, @Param("startTime") LocalDateTime startTime);

    @Update("""
            UPDATE data_import_task
            SET status = 'completed', end_time = #{endTime}, total_rows = #{totalRows},
                processed_rows = #{processedRows}, target_table_name = #{targetTableName}
            WHERE id = #{id}
            """)
    int updateToCompleted(@Param("id") String id, @Param("endTime") LocalDateTime endTime,
            @Param("totalRows") Integer totalRows, @Param("processedRows") Integer processedRows,
            @Param("targetTableName") String targetTableName);

    @Update("""
            UPDATE data_import_task
            SET status = 'failed', end_time = #{endTime}, error_message = #{errorMessage}
            WHERE id = #{id}
            """)
    int updateToFailed(@Param("id") String id, @Param("endTime") LocalDateTime endTime,
            @Param("errorMessage") String errorMessage);

    @Delete("""
            DELETE FROM data_import_task WHERE id = #{id}
            """)
    int deleteById(String id);

}
