package com.kairon.mapper;

import com.kairon.domain.entity.Services;
import com.kairon.dto.response.ServiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    ServiceMapper INSTANCE = Mappers.getMapper(ServiceMapper.class);

    ServiceResponse toResponse(Services service);
}