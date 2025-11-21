package com.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.exception.ServerException;
import com.crm.common.result.PageResult;
import com.crm.convert.ContractConvert;
import com.crm.entity.*;
import com.crm.enums.ContractStatus;
import com.crm.mapper.*;
import com.crm.query.ApprovalQuery;
import com.crm.query.ContractQuery;
import com.crm.query.IdQuery;
import com.crm.security.user.SecurityUser;
import com.crm.service.ContractService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crm.vo.ContractVO;
import com.crm.vo.ProductVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.crm.utils.NumberUtils.generateContractNumber;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author crm
 * @since 2025-10-12
 */
@Service
@AllArgsConstructor
@Slf4j
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {
    private final ProductMapper productMapper;
    private final ContractProductMapper contractProductMapper;
    private final ApprovalMapper approvalMapper;
    private final ManagerMapper managerMapper;
    private final JavaMailSender mailSender;

    @Override
    public PageResult<ContractVO> getPage(ContractQuery query) {
        Page<ContractVO> page = new Page<>(query.getPage(), query.getLimit());
//        条件查询
        MPJLambdaWrapper<Contract> wrapper = new MPJLambdaWrapper<>();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(Contract::getName, query.getName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Contract::getStatus, query.getStatus());
        }
        if (query.getCustomerId() != null) {
            wrapper.eq(Contract::getCustomerId, query.getCustomerId());
        }
        if (StringUtils.isNotBlank(query.getNumber())) {
            wrapper.like(Contract::getNumber, query.getNumber());
        }
        // 只查询目前登录的员工签署的合同信息
        Integer managerId = SecurityUser.getManagerId();
        wrapper.selectAll(Contract.class)
                .selectAs(Customer::getName, ContractVO::getCustomerName)
                .leftJoin(Customer.class, Customer::getId, Contract::getCustomerId)
                .eq(Contract::getOwnerId, managerId).orderByDesc(Contract::getCreateTime);
        Page<ContractVO> result = baseMapper.selectJoinPage(page, ContractVO.class, wrapper);
//        查询合同签署的商品信息
        if (!result.getRecords().isEmpty()) {
            result.getRecords().forEach(contractVO -> {
                List<ContractProduct> contractProducts = contractProductMapper.selectList(new LambdaQueryWrapper<ContractProduct>().eq(ContractProduct::getCId, contractVO.getId()));
                contractVO.setProducts(ContractConvert.INSTANCE.toProductVOList(contractProducts));
            });
        }
        return new PageResult<>(result.getRecords(), page.getTotal());
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdate(ContractVO contractVO) {

        boolean isNew = contractVO.getId() == null;
        // 校验合同名称重复
        if (isNew && baseMapper.exists(new LambdaQueryWrapper<Contract>().eq(Contract::getName, contractVO.getName()))) {
            throw new ServerException("合同名称已存在，请勿重复添加");
        }
        // 转换并保存合同
        Contract contract = ContractConvert.INSTANCE.toContract(contractVO);
        contract.setCreaterId(SecurityUser.getManagerId());
        contract.setOwnerId(SecurityUser.getManagerId());
        if (isNew) {
            contract.setNumber(generateContractNumber());
            baseMapper.insert(contract);
        } else {
            Contract dbContract = baseMapper.selectById(contract.getId());
            if (dbContract == null) throw new ServerException("合同不存在");
            if (dbContract.getStatus() == ContractStatus.APPROVED) {
                throw new ServerException("该合同已审核通过，请勿修改");
            }
            baseMapper.updateById(contract);
        }
        if (contract.getReceivedAmount() == null) {
            contract.setReceivedAmount(BigDecimal.ZERO);
        }
        // 处理合同商品明细
        handleContractProducts(contract.getId(), contractVO.getProducts());

    }

    @Override
    public void startApproval(IdQuery idQuery) {
        Contract contract = baseMapper.selectById(idQuery.getId());
        if (contract == null) {
            throw new ServerException("合同不存在");
        }
        if (!ContractStatus.PENDING.getCode().equals(contract.getStatus())) {
            throw new ServerException("该合同不是待审核状态，请勿重复提交");
        }
        contract.setStatus(ContractStatus.PENDING);
        baseMapper.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approvalContract(ApprovalQuery query) {
        Contract contract = baseMapper.selectById(query.getId());
        if (contract == null) {
            throw new ServerException("合同不存在");
        }

        // 用户输入的审核原因，保存到合同表
        String approvalReason = query.getReason();

        // 默认的审批评论内容，保存到审批表
        String approvalComment = query.getType() == 0 ? "合同审核通过" : "合同审核未通过";

        Integer contractStatus = query.getType() == 0 ?
                ContractStatus.APPROVED.getCode() : ContractStatus.REJECTED.getCode();
        Approval approval = new Approval();
        approval.setType(0);
        approval.setStatus(query.getType());
        approval.setCreaterId(SecurityUser.getManagerId());
        approval.setContractId(contract.getId());
        approval.setComment(approvalComment);
        approvalMapper.insert(approval);

        contract.setApprovalReason(approvalReason);
        contract.setStatus(ContractStatus.fromCode(contractStatus));
        baseMapper.updateById(contract);
        // 如果审核通过，发送邮件通知
            sendApprovalNotificationAsync(contract);
    }

    private void sendApprovalNotificationAsync(Contract contract) {
        // 异步执行邮件发送
        CompletableFuture.runAsync(() -> sendApprovalNotification(contract));
    }


    private void handleContractProducts(Integer contractId, List<ProductVO> newProductList) {
        if (newProductList == null) return;

        List<ContractProduct> oldProducts = contractProductMapper.selectList(
                new LambdaQueryWrapper<ContractProduct>().eq(ContractProduct::getCId, contractId)
        );

        // === 1. 新增商品 ===
        List<ProductVO> newAdded = newProductList.stream()
                .filter(np -> oldProducts.stream().noneMatch(op -> op.getPId().equals(np.getPId())))
                .toList();
        for (ProductVO p : newAdded) {
            Product product = checkAndGetProduct(p.getPId(), p.getCount());
            decreaseStock(product, p.getCount());
            ContractProduct cp = buildContractProduct(contractId, product, p.getCount());
            contractProductMapper.insert(cp);
        }

        // === 2. 修改数量 ===
        List<ProductVO> changed = newProductList.stream()
                .filter(np -> oldProducts.stream()
                        .anyMatch(op -> op.getPId().equals(np.getPId()) && !op.getCount().equals(np.getCount())))
                .toList();
        for (ProductVO p : changed) {
            ContractProduct old = oldProducts.stream()
                    .filter(op -> op.getPId().equals(p.getPId()))
                    .findFirst().orElseThrow();

            Product product = checkAndGetProduct(p.getPId(), 0);
            int diff = p.getCount() - old.getCount();

            // 库存调整
            if (diff > 0) decreaseStock(product, diff);
            else if (diff < 0) increaseStock(product, -diff);

            // 更新合同商品
            old.setCount(p.getCount());
            old.setPrice(product.getPrice());
            old.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(p.getCount())));
            contractProductMapper.updateById(old);
        }

        // === 3. 删除商品 ===
        List<ContractProduct> removed = oldProducts.stream()
                .filter(op -> newProductList.stream().noneMatch(np -> np.getPId().equals(op.getPId())))
                .toList();
        for (ContractProduct rm : removed) {
            Product product = productMapper.selectById(rm.getPId());
            if (product != null) increaseStock(product, rm.getCount());
            contractProductMapper.deleteById(rm.getId());
        }
    }


    private Product checkAndGetProduct(Integer productId, int needCount) {
        Product product = productMapper.selectById(productId);
        if (product == null) throw new ServerException("商品不存在");
        if (needCount > 0 && product.getStock() < needCount) {
            throw new ServerException("商品库存不足");
        }
        return product;
    }

    private void decreaseStock(Product product, int count) {
        product.setStock(product.getStock() - count);
        product.setSales(product.getSales() + count);
        productMapper.updateById(product);
    }

    private void increaseStock(Product product, int count) {
        product.setStock(product.getStock() + count);
        product.setSales(product.getSales() - count);
        productMapper.updateById(product);
    }

    private ContractProduct buildContractProduct(Integer contractId, Product product, int count) {
        ContractProduct cp = new ContractProduct();
        cp.setCId(contractId);
        cp.setPId(product.getId());
        cp.setPName(product.getName());
        cp.setCount(count);
        cp.setPrice(product.getPrice());
        cp.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(count)));
        return cp;
    }




  @Async
public void sendApprovalNotification(Contract contract) {
    try {
        log.info("开始发送审批通知，合同ID: {}", contract.getId());

        // 查询创建者信息
        Manager creator = managerMapper.selectById(contract.getCreaterId());
        log.info("创建者信息: {}", creator);

        // 只有当创建者存在且有有效邮箱时才发送邮件
        if (creator != null && StringUtils.isNotBlank(creator.getEmail())) {
            String recipientEmail = creator.getEmail();
            log.info("使用创建者邮箱: {}", recipientEmail);

            // 根据合同状态确定邮件内容
            String subject;
            String text;
            if (contract.getStatus() == ContractStatus.APPROVED) {
                subject = "合同审核通过通知";
                text = "您创建的合同[" + contract.getName() + "]已审核通过，合同编号：" + contract.getNumber();
            } else {
                subject = "合同审核结果通知";
                String reason = StringUtils.isNotBlank(contract.getApprovalReason())
                    ? "，原因：" + contract.getApprovalReason()
                    : "";
                text = "您创建的合同[" + contract.getName() + "]审核未通过" + reason + "，合同编号：" + contract.getNumber();
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("3068195512@qq.com");
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("邮件发送成功，收件人: {}", recipientEmail);
        } else {
            log.warn("创建者邮箱为空或不存在，不发送邮件通知");
        }

    } catch (Exception e) {
        log.error("发送邮件通知失败，合同ID: " + contract.getId(), e);
    }
}
}
