package com.moderation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Content Moderation Server 启动类
 */
@SpringBootApplication
@MapperScan("com.moderation.mapper")
public class ContentModerationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentModerationApplication.class, args);
    }
}
