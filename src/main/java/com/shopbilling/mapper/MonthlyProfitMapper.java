package com.shopbilling.mapper;

import com.shopbilling.dto.response.MonthlyProfitResponse;
import com.shopbilling.entity.MonthlyProfit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.Month;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MonthlyProfitMapper {
    
    @Mapping(target = "monthName", expression = "java(getMonthName(profit.getMonth()))")
    MonthlyProfitResponse toResponse(MonthlyProfit profit);
    
    List<MonthlyProfitResponse> toResponseList(List<MonthlyProfit> profits);
    
    default String getMonthName(int month) {
        return Month.of(month).name();
    }
}
