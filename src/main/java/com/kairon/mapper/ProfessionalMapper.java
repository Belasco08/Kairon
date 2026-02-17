package com.kairon.mapper;

import com.kairon.domain.entity.Professional;
import com.kairon.dto.response.ProfessionalResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ProfessionalMapper {

    ProfessionalMapper INSTANCE = Mappers.getMapper(ProfessionalMapper.class);


    ProfessionalResponse toResponse(Professional professional);


    ProfessionalResponse toBasicResponse(Professional professional);
}