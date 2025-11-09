package com.crm.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowVO {
    private Integer id;

    private String content;
    private Integer followType;
    private LocalDateTime nextFollowType;
}