package com.moderation.service;

import com.moderation.model.req.ProjectSaveReq;
import com.moderation.model.res.ProjectListRes;

public interface ProjectManageService {
    ProjectListRes listProjects();
    ProjectListRes createProject(ProjectSaveReq req);
    ProjectListRes updateProject(String projectId, ProjectSaveReq req);
    ProjectListRes deleteProject(String projectId);
}
