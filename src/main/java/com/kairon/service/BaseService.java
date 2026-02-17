package com.kairon.service;

import com.kairon.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class BaseService {

    protected void validateCompanyAccess(String requestedCompanyId, String userCompanyId) {
        if (!requestedCompanyId.equals(userCompanyId)) {
            throw new BusinessException("Access denied to company resources");
        }
    }

    protected String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    protected void validateResourceBelongsToCompany(String resourceCompanyId, String userCompanyId) {
        if (!resourceCompanyId.equals(userCompanyId)) {
            throw new BusinessException("Resource does not belong to your company");
        }
    }
}