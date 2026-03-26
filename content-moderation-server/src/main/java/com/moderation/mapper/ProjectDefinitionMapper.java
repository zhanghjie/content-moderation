package com.moderation.mapper;

import com.moderation.entity.ProjectDefinitionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProjectDefinitionMapper {
    @Select("""
            SELECT
              project_id AS projectId,
              name,
              description,
              status,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM project_definition
            ORDER BY created_at DESC
            """)
    List<ProjectDefinitionEntity> listAll();

    @Select("""
            SELECT
              project_id AS projectId,
              name,
              description,
              status,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM project_definition
            WHERE project_id = #{projectId}
            LIMIT 1
            """)
    ProjectDefinitionEntity selectById(String projectId);

    @Insert("""
            INSERT INTO project_definition (
              project_id, name, description, status
            ) VALUES (
              #{projectId}, #{name}, #{description}, #{status}
            )
            ON CONFLICT (project_id) DO UPDATE SET
              name = EXCLUDED.name,
              description = EXCLUDED.description,
              status = EXCLUDED.status,
              updated_at = now()
            """)
    int upsert(ProjectDefinitionEntity entity);

    @Delete("DELETE FROM project_definition WHERE project_id = #{projectId}")
    int deleteById(String projectId);
}
