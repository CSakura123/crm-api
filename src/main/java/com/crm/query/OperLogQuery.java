package com.crm.query;

import com.baomidou.mybatisplus.annotation.TableField;
import com.crm.common.model.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author 小c
 */
@Data
public class OperLogQuery extends Query {
    @Schema(description = "模块标题")
    @TableField("title")
    private String title;

    @Schema(description = "业务类型")
    @TableField("oper_type")
    private String operType;

    @Schema(description = "请求url")
    @TableField("oper_url")
    private String operUrl;

    @Schema(description = "主机地址")
    @TableField("oper_ip")
    private String operIp;

    @Schema(description = "操作地点")
    @TableField("oper_location")
    private String operLocation;

    @Schema(description = "操作人员")
    @TableField("oper_name")
    private String operName;

    @Schema(description = "操作状态")
    @TableField("status")
    private String status;

    @Schema(description = "请求方法")
    @TableField("request_method")
    private String requestMethod;

    @Schema(description = "请求参数")
    @TableField("oper_param")
    private String operParam;

    @Schema(description = "返回参数")
    @TableField("json_result")
    private String jsonResult;

    @Schema(description = "错误消息")
    @TableField("error_msg")
    private String errorMsg;

    @Schema(description = "操作时间")
    @TableField("oper_time")
    private LocalDateTime operTime;

    @Schema(description = "消耗时间(ms)")
    @TableField("cost_time")
    private Long costTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}

