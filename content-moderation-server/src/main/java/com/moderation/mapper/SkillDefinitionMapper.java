package com.moderation.mapper;

import com.moderation.entity.SkillDefinitionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SkillDefinitionMapper {
    @Select("""
            SELECT
              skill_id AS skillId,
              name,
              type,
              description,
              tags_json AS tagsJson,
              output_schema_json AS outputSchemaJson,
              state_mapping_json AS stateMappingJson,
              execution_config_json AS executionConfigJson,
              script_config_json AS scriptConfigJson,
              status,
              timeout_ms AS timeoutMs,
              version,
              executor_bean AS executorBean,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM skill_definition
            """)
    List<SkillDefinitionEntity> listAll();

    @Select("""
            SELECT
              skill_id AS skillId,
              name,
              type,
              description,
              tags_json AS tagsJson,
              output_schema_json AS outputSchemaJson,
              state_mapping_json AS stateMappingJson,
              execution_config_json AS executionConfigJson,
              script_config_json AS scriptConfigJson,
              status,
              timeout_ms AS timeoutMs,
              version,
              executor_bean AS executorBean,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM skill_definition
            WHERE skill_id = #{skillId}
            LIMIT 1
            """)
    SkillDefinitionEntity selectById(String skillId);

    @Insert("""
            INSERT INTO skill_definition (
              skill_id, name, type, description, tags_json, output_schema_json, state_mapping_json,
              execution_config_json, script_config_json, status, timeout_ms, version, executor_bean
            ) VALUES (
              #{skillId}, #{name}, #{type}, #{description}, #{tagsJson}, #{outputSchemaJson}, #{stateMappingJson},
              #{executionConfigJson}, #{scriptConfigJson}, #{status}, #{timeoutMs}, #{version}, #{executorBean}
            )
            ON CONFLICT (skill_id) DO UPDATE SET
              name = EXCLUDED.name,
              type = EXCLUDED.type,
              description = EXCLUDED.description,
              tags_json = EXCLUDED.tags_json,
              output_schema_json = EXCLUDED.output_schema_json,
              state_mapping_json = EXCLUDED.state_mapping_json,
              execution_config_json = EXCLUDED.execution_config_json,
              script_config_json = EXCLUDED.script_config_json,
              status = EXCLUDED.status,
              timeout_ms = EXCLUDED.timeout_ms,
              version = EXCLUDED.version,
              executor_bean = EXCLUDED.executor_bean,
              updated_at = now()
            """)
    int upsert(SkillDefinitionEntity entity);

    @Delete("DELETE FROM skill_definition WHERE skill_id = #{skillId}")
    int deleteById(String skillId);
}
