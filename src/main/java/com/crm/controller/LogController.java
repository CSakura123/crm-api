package com.crm.controller;

import com.crm.common.aop.Log;
import com.crm.common.result.PageResult;
import com.crm.common.result.Result;
import com.crm.entity.OperLog;
import com.crm.enums.BusinessType;
import com.crm.query.OperLogQuery;
import com.crm.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 小c
 */
@Tag(name = "日志管理")
@RestController
@RequestMapping("log")
@AllArgsConstructor
public class LogController {
    private final OperLogService operLogService;
    @RequestMapping("page")
    @Operation(summary = "日志查询")
    @Log(title = "日志查询",businessType = BusinessType.SELECT)
    public Result<PageResult<OperLog>> getPage(@RequestBody @Validated OperLogQuery query) {
        return Result.ok(operLogService.getPage(query));
    }
}
