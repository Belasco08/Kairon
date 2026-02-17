package com.kairon.mapper;

import com.kairon.domain.entity.Company;
import com.kairon.dto.response.CompanyResponse;
import com.kairon.dto.response.PublicCompanyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

    CompanyMapper INSTANCE = Mappers.getMapper(CompanyMapper.class);

    CompanyResponse toResponse(Company company);

    PublicCompanyResponse toPublicResponse(Company company);
}