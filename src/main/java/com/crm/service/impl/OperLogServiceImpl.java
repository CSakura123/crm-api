package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.result.PageResult;
import com.crm.entity.OperLog;
import com.crm.mapper.OperLogMapper;
import com.crm.query.OperLogQuery;
import com.crm.service.OperLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.utils.AddressUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 操作日志记录 服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Service
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, OperLog> implements OperLogService {

    @Override
    public void recordOperLog(OperLog operlog) {
        operlog.setOperLocation(AddressUtils.getRealAddressByIP(operlog.getOperIp()));
        operlog.setOperTime(LocalDateTime.now());
        baseMapper.insert(operlog);
    }
    @Override
    public PageResult<OperLog> getPage(OperLogQuery query) {
        Page<OperLog> page = new Page<>(query.getPage(), query.getLimit());
        LambdaQueryWrapper<OperLog> wrapper = new LambdaQueryWrapper<>();

        // 操作人筛选
        if (query.getOperName() != null) {
            wrapper.like(OperLog::getOperName, query.getOperName());
        }
        // 业务类型筛选
        if (query.getOperType() != null) {
            wrapper.eq(OperLog::getOperType, query.getOperType());
        }
        // 状态筛选
        if (query.getStatus() != null) {
            wrapper.eq(OperLog::getStatus, query.getStatus());
        }
        // 时间范围筛选（核心筛选条件）
        if (query.getStartTime() != null) {
            wrapper.ge(OperLog::getOperTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(OperLog::getOperTime, query.getEndTime());
        }

        // 按操作时间倒序
        wrapper.orderByDesc(OperLog::getOperTime);

        Page<OperLog> result = baseMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal());
    }
}
