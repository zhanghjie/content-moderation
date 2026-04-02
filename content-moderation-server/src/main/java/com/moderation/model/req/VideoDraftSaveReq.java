package com.moderation.model.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class VideoDraftSaveReq {

    /**
     * 草稿任务 ID，更新草稿时传入；新建草稿可不传
     */
    private String taskId;

    /**
     * 选择的 Policy ID
     */
    @NotBlank(message = "policyId is required")
    private String policyId;

    /**
     * Policy 执行输入（动态字段，根据 Policy 定义传入）
     */
    private Map<String, Object> policyInput;

    /**
     * 分析类型（可选）
     */
    private String analysisType;
}

