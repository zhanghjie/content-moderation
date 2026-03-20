package com.moderation.model.res;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TaskListRes {

    private Long total;

    private Integer page;

    private Integer pageSize;

    private List<VideoAnalyzeRes> list;
}

