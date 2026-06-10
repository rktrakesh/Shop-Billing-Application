package com.shopbilling.mapper;

import com.shopbilling.dto.response.ProductVariantResponse;
import com.shopbilling.entity.ProductVariant;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductVariantMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "designName", source = "product.designName")
    @Mapping(target = "lowStock", expression = "java(variant.getStock() <= variant.getMinimumStock())")
    ProductVariantResponse toResponse(ProductVariant variant);
    
    List<ProductVariantResponse> toResponseList(List<ProductVariant> variants);
}
