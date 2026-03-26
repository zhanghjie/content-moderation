package com.moderation.model.req;

import lombok.Data;

@Data
public class ProjectSaveReq {
    private String projectId;
    private String name;
    private String description;
    private String status;
}
