package com.crm.query;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author 小c
 */
@Data
public class ApprovalQuery {
    @NotNull(message = "审核id不能为空")
    private Integer id;
    @NotNull(message = "审核状态不能为空")
    private Integer type;
    @NotNull(message = "审核理由不能为空")
    private String reason;
}
