package com.crm.service;

import com.crm.vo.FileUrlVO;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MultipartFilter;

public interface CommonService {
    FileUrlVO upload(MultipartFile multipartFile);
}
