package com.moderation.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果
 */
@Data
public class BaseResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer code;
    private String message;
    private T data;
    
    public static <T> BaseResult<T> success() {
        return success(null);
    }
    
    public static <T> BaseResult<T> success(T data) {
        BaseResult<T> result = new BaseResult<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }
    
    public static <T> BaseResult<T> failed(String message) {
        return failed(500, message);
    }
    
    public static <T> BaseResult<T> failed(Integer code, String message) {
        BaseResult<T> result = new BaseResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
