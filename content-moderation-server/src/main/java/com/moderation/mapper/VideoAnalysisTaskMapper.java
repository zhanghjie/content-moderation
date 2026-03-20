package com.moderation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moderation.entity.VideoAnalysisTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 视频分析任务 Mapper
 */
@Mapper
public interface VideoAnalysisTaskMapper extends BaseMapper<VideoAnalysisTaskEntity> {
    
    @Select("SELECT * FROM video_analysis_task WHERE call_id = #{callId} ORDER BY created_at DESC LIMIT 1")
    VideoAnalysisTaskEntity selectByCallId(@Param("callId") String callId);
    
    @Select("SELECT * FROM video_analysis_task WHERE task_id = #{taskId}")
    VideoAnalysisTaskEntity selectByTaskId(@Param("taskId") String taskId);

    @Select("""
            SELECT *
            FROM video_analysis_task
            WHERE content_id = #{contentId}
              AND status IN ('PENDING', 'PROCESSING')
            ORDER BY created_at DESC
            LIMIT 1
            """)
    VideoAnalysisTaskEntity selectActiveByContentId(@Param("contentId") String contentId);
}
