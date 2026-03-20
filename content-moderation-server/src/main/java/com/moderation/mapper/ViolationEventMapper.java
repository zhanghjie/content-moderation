package com.moderation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moderation.entity.ViolationEventEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 违规事件 Mapper
 */
@Mapper
public interface ViolationEventMapper extends BaseMapper<ViolationEventEntity> {
}
