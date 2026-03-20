package com.moderation.model.req;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WorkflowExecuteReq {
    private String workflowDsl;
    private List<String> promptDsls;
    private Map<String, Object> inputs;
}
