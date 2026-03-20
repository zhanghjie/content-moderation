package com.moderation.service;

import com.moderation.model.req.VideoAnalyzeReq;
import com.moderation.model.res.VideoAnalyzeRes;
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
     * 查询分析结果
     */
    VideoAnalyzeRes getResult(String callId);

    TaskListRes getTasks(String callId, String status, String result, Integer page, Integer pageSize);

    VideoAnalyzeRes reAnalyze(String callId);
}
