package com.crm.query;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author 小c
 */
@Data
public class IdQuery {

    @NotNull(message = "id不能为空")
    private Integer id;
}
