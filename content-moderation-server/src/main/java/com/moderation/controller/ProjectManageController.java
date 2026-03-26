package com.moderation.controller;

import com.moderation.common.BaseResult;
import com.moderation.model.req.ProjectSaveReq;
import com.moderation.model.res.ProjectListRes;
import com.moderation.service.ProjectManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "项目管理", description = "项目配置持久化管理")
public class ProjectManageController {
    private final ProjectManageService projectManageService;

    @GetMapping("/projects")
    @Operation(summary = "查询项目列表")
    public BaseResult<ProjectListRes> listProjects() {
        return BaseResult.success(projectManageService.listProjects());
    }

    @PostMapping("/projects")
    @Operation(summary = "新增项目")
    public BaseResult<ProjectListRes> createProject(@RequestBody ProjectSaveReq req) {
        try {
            return BaseResult.success(projectManageService.createProject(req));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @PutMapping("/projects/{projectId}")
    @Operation(summary = "更新项目")
    public BaseResult<ProjectListRes> updateProject(@PathVariable String projectId, @RequestBody ProjectSaveReq req) {
        try {
            return BaseResult.success(projectManageService.updateProject(projectId, req));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }

    @DeleteMapping("/projects/{projectId}")
    @Operation(summary = "删除项目")
    public BaseResult<ProjectListRes> deleteProject(@PathVariable String projectId) {
        try {
            return BaseResult.success(projectManageService.deleteProject(projectId));
        } catch (IllegalArgumentException e) {
            return BaseResult.failed(400, e.getMessage());
        }
    }
}
