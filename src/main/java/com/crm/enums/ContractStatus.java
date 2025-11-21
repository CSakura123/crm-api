package com.crm.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @author 小c
 */

// ContractStatus.java
@Getter
public enum ContractStatus {
    DRAFT(0, "草稿"),
    PENDING(1, "待审核"),
    APPROVED(2, "审核通过"),
    REJECTED(3, "审核不通过");
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;

    ContractStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ContractStatus fromCode(Integer code) {
        for (ContractStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown contract status code: " + code);
    }
}

