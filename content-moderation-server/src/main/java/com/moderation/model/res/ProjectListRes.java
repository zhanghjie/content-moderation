package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProjectListRes {
    private List<ProjectItemRes> projects;
}
