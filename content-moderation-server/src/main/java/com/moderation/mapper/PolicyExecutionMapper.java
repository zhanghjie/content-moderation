package com.moderation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moderation.entity.PolicyExecutionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PolicyExecutionMapper extends BaseMapper<PolicyExecutionEntity> {
    @Select("SELECT * FROM policy_execution WHERE execution_id = #{executionId} LIMIT 1")
    PolicyExecutionEntity selectByExecutionId(@Param("executionId") String executionId);

    @Select("SELECT * FROM policy_execution WHERE policy_id = #{policyId} ORDER BY created_at DESC LIMIT 1")
    PolicyExecutionEntity selectLatestByPolicyId(@Param("policyId") String policyId);
}
