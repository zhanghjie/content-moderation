package com.moderation.model.dto;

import lombok.Data;

/**
 * 违规信息 DTO
 */
@Data
public class ViolationDTO {
    
    private String type;
    
    private Boolean detected;
    
    private Double confidence;
    
    private String evidence;
    
    private Integer startSec;
    
    private Integer endSec;
}
