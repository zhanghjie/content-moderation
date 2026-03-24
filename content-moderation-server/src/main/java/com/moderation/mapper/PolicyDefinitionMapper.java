package com.moderation.mapper;

import com.moderation.entity.PolicyDefinitionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PolicyDefinitionMapper {
    @Select("""
            SELECT
              policy_id AS policyId,
              name,
              skill_pipeline_json AS skillPipelineJson,
              config_json AS configJson,
              execution_input_json AS executionInputJson,
              version,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM policy_definition
            """)
    List<PolicyDefinitionEntity> listAll();

    @Select("""
            SELECT
              policy_id AS policyId,
              name,
              skill_pipeline_json AS skillPipelineJson,
              config_json AS configJson,
              execution_input_json AS executionInputJson,
              version,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM policy_definition
            WHERE policy_id = #{policyId}
            LIMIT 1
            """)
    PolicyDefinitionEntity selectById(String policyId);

    @Insert("""
            INSERT INTO policy_definition (
              policy_id, name, skill_pipeline_json, config_json, execution_input_json, version
            ) VALUES (
              #{policyId}, #{name}, #{skillPipelineJson}, #{configJson}, #{executionInputJson}, #{version}
            )
            ON CONFLICT (policy_id) DO UPDATE SET
              name = EXCLUDED.name,
              skill_pipeline_json = EXCLUDED.skill_pipeline_json,
              config_json = EXCLUDED.config_json,
              execution_input_json = EXCLUDED.execution_input_json,
              version = EXCLUDED.version,
              updated_at = now()
            """)
    int upsert(PolicyDefinitionEntity entity);

    @Delete("DELETE FROM policy_definition WHERE policy_id = #{policyId}")
    int deleteById(String policyId);
}
