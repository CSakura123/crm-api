package com.crm.controller;

import com.crm.common.result.Result;
import com.crm.service.CommonService;
import com.crm.vo.FileUrlVO;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "通用模块") // 新
@RestController
@RequestMapping("common")
@AllArgsConstructor
public class CommonController {
    private final CommonService commonService;

    @PostMapping("/upload/file") // 新
    public Result<FileUrlVO> upload(@RequestBody MultipartFile file) {
        return Result.ok(commonService.upload(file));
    }
}
