package com.moderation.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 违规类型枚举
 */
@Getter
@AllArgsConstructor
public enum ViolationTypeEnum {
    ENVIRONMENT_MESSY("环境杂乱"),
    NOISY("声音嘈杂"),
    SEXUAL_ACTION("色情动作"),
    PUBLIC_PLACE("公共场合"),
    OTHER_PLATFORM_OR_OFFLINE_JOB("其他平台/实体工作"),
    MULTI_PERSON_CONTEXT("多人出镜"),
    WATCH_TV_OR_PLAY_PHONE("看电视/玩手机"),
    CALL_IN_BED("躺在床上通话"),
    SILENT_ALL_TIME("全程不说话"),
    NO_ONE_ON_CAMERA("无人出镜"),
    SLEEPING("睡觉"),
    BLACK_SCREEN("黑屏"),
    PLAY_RECORDING("播放录屏");
    
    private final String description;
}
