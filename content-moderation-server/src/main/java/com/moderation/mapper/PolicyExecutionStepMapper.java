package com.moderation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moderation.entity.PolicyExecutionStepEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PolicyExecutionStepMapper extends BaseMapper<PolicyExecutionStepEntity> {
    @Select("SELECT * FROM policy_execution_step WHERE execution_id = #{executionId} ORDER BY created_at ASC")
    List<PolicyExecutionStepEntity> selectByExecutionId(@Param("executionId") String executionId);

    @Select("SELECT * FROM policy_execution_step WHERE trace_id = #{traceId} LIMIT 1")
    PolicyExecutionStepEntity selectByTraceId(@Param("traceId") String traceId);
}
