package com.crm.convert;

import com.crm.entity.Contract;
import com.crm.entity.ContractProduct;
import com.crm.enums.ContractStatus;
import com.crm.vo.ContractVO;
import com.crm.vo.ProductVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author 小c
 */
@Mapper
public interface ContractConvert {
    ContractConvert INSTANCE = Mappers.getMapper(ContractConvert.class);

    Contract toContract(ContractVO contractVO);

    ProductVO toProductVO(ContractProduct product);

    List<ProductVO> toProductVOList(List<ContractProduct> productList);
    // 添加状态字段的映射方法
    default ContractStatus map(Integer value) {
        if (value == null) {
            return null;
        }
        return ContractStatus.fromCode(value);
    }

    // 如果需要反向映射，也添加这个方法
    default Integer map(ContractStatus status) {
        if (status == null) {
            return null;
        }
        return status.getCode();
    }
}