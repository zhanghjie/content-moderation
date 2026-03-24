package com.moderation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moderation.entity.PolicyExecutionFeedbackEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PolicyExecutionFeedbackMapper extends BaseMapper<PolicyExecutionFeedbackEntity> {
    @Select("SELECT * FROM policy_execution_feedback WHERE execution_id = #{executionId} ORDER BY created_at DESC")
    List<PolicyExecutionFeedbackEntity> selectByExecutionId(@Param("executionId") String executionId);
}
