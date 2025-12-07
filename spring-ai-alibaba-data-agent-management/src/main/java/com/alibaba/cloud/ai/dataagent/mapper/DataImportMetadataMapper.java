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

import com.alibaba.cloud.ai.dataagent.entity.DataImportMetadata;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 数据导入元数据Mapper接口
 */
@Mapper
public interface DataImportMetadataMapper {

    @Insert("""
            INSERT INTO data_import_metadata (task_id, table_name, logical_table_name, column_count, row_count, datasource_type, create_time)
            VALUES (#{taskId}, #{tableName}, #{logicalTableName}, #{columnCount}, #{rowCount}, #{datasourceType}, #{createTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(DataImportMetadata metadata);

    @Select("""
            SELECT * FROM data_import_metadata WHERE task_id = #{taskId}
            """)
    DataImportMetadata findByTaskId(String taskId);

    @Select("""
            SELECT * FROM data_import_metadata WHERE table_name = #{tableName}
            """)
    List<DataImportMetadata> findByTableName(String tableName);

    @Delete("""
            DELETE FROM data_import_metadata WHERE task_id = #{taskId}
            """)
    int deleteByTaskId(String taskId);

}
