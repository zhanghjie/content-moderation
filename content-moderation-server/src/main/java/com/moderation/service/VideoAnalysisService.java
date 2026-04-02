package com.moderation.service;

import com.moderation.model.req.VideoAnalyzeReq;
import com.moderation.model.req.VideoDraftSaveReq;
import com.moderation.model.res.VideoAnalyzeRes;
import com.moderation.model.res.VideoDraftRes;
import com.moderation.model.res.TaskListRes;

/**
 * 视频分析服务接口
 */
public interface VideoAnalysisService {
    
    /**
     * 发起视频分析
     */
    VideoAnalyzeRes analyze(VideoAnalyzeReq req);

    /**
     * 基于 Policy 执行视频分析并返回标准化结果
     */
    VideoAnalyzeRes analyzeAndSave(VideoAnalyzeReq req);

    /**
     * 查询分析结果
     */
    VideoAnalyzeRes getResult(String callId);

    VideoDraftRes saveDraft(VideoDraftSaveReq req);

    VideoDraftRes getDraft(String taskId);

    VideoAnalyzeRes executeDraft(String taskId);

    TaskListRes getTasks(String callId, String policyId, String status, String result, Integer page, Integer pageSize);

    VideoAnalyzeRes reAnalyze(String callId);
}
