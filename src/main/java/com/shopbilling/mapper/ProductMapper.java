package com.shopbilling.mapper;

import com.shopbilling.dto.request.ProductRequest;
import com.shopbilling.dto.response.ProductResponse;
import com.shopbilling.entity.Product;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ProductVariantMapper.class})
public interface ProductMapper {
    ProductResponse toResponse(Product product);
    List<ProductResponse> toResponseList(List<Product> products);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductRequest request);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(ProductRequest request, @MappingTarget Product product);
}
