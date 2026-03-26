package com.moderation.service.impl;

import com.moderation.entity.ProjectDefinitionEntity;
import com.moderation.mapper.ProjectDefinitionMapper;
import com.moderation.model.req.ProjectSaveReq;
import com.moderation.model.res.ProjectItemRes;
import com.moderation.model.res.ProjectListRes;
import com.moderation.service.ProjectManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectManageServiceImpl implements ProjectManageService {
    private final ProjectDefinitionMapper projectDefinitionMapper;

    @Override
    public ProjectListRes listProjects() {
        return ProjectListRes.builder().projects(toRes(projectDefinitionMapper.listAll())).build();
    }

    @Override
    @Transactional
    public ProjectListRes createProject(ProjectSaveReq req) {
        String projectId = normalizeId(req.getProjectId());
        if (projectDefinitionMapper.selectById(projectId) != null) {
            throw new IllegalArgumentException("项目已存在: " + projectId);
        }
        ProjectDefinitionEntity entity = new ProjectDefinitionEntity();
        entity.setProjectId(projectId);
        entity.setName(normalizeName(req.getName()));
        entity.setDescription(normalizeDescription(req.getDescription()));
        entity.setStatus(normalizeStatus(req.getStatus(), "ACTIVE"));
        projectDefinitionMapper.upsert(entity);
        return listProjects();
    }

    @Override
    @Transactional
    public ProjectListRes updateProject(String projectId, ProjectSaveReq req) {
        String targetId = normalizeId(projectId);
        ProjectDefinitionEntity existing = projectDefinitionMapper.selectById(targetId);
        if (existing == null) {
            throw new IllegalArgumentException("项目不存在: " + targetId);
        }
        ProjectDefinitionEntity entity = new ProjectDefinitionEntity();
        entity.setProjectId(targetId);
        entity.setName(hasText(req.getName()) ? normalizeName(req.getName()) : existing.getName());
        entity.setDescription(req.getDescription() == null ? existing.getDescription() : normalizeDescription(req.getDescription()));
        entity.setStatus(normalizeStatus(req.getStatus(), existing.getStatus()));
        projectDefinitionMapper.upsert(entity);
        return listProjects();
    }

    @Override
    @Transactional
    public ProjectListRes deleteProject(String projectId) {
        String targetId = normalizeId(projectId);
        int deleted = projectDefinitionMapper.deleteById(targetId);
        if (deleted <= 0) {
            throw new IllegalArgumentException("项目不存在: " + targetId);
        }
        return listProjects();
    }

    private List<ProjectItemRes> toRes(List<ProjectDefinitionEntity> entities) {
        return entities.stream()
                .map(item -> ProjectItemRes.builder()
                        .projectId(item.getProjectId())
                        .name(item.getName())
                        .description(item.getDescription())
                        .status(item.getStatus())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build())
                .toList();
    }

    private String normalizeId(String value) {
        String projectId = value == null ? "" : value.trim();
        if (projectId.isEmpty()) {
            throw new IllegalArgumentException("projectId 不能为空");
        }
        return projectId;
    }

    private String normalizeName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        return name;
    }

    private String normalizeDescription(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeStatus(String value, String defaultValue) {
        String normalized = value == null || value.isBlank() ? defaultValue : value.trim().toUpperCase();
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new IllegalArgumentException("status 仅支持 ACTIVE 或 INACTIVE");
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
