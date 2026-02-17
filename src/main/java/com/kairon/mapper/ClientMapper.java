package com.kairon.mapper;

import com.kairon.domain.entity.Client;
import com.kairon.dto.request.ClientRequest;
import com.kairon.dto.response.ClientListResponse;
import com.kairon.dto.response.ClientResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    Client toEntity(ClientRequest request);

    ClientResponse toResponse(Client client);

    ClientListResponse toListResponse(Client client);

    void updateEntity(ClientRequest request, @MappingTarget Client client);
}
