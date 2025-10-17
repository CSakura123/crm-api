package com.crm.query;

import com.crm.common.model.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author 小c
 */
@Data

public class DepartmentQuery extends Query {
    @Schema(description = "部门名称")
    private String name;

}
