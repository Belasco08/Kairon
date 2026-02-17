package com.kairon.security.util;

import com.kairon.exception.BusinessException;
import com.kairon.security.auth.UserDetailsImpl;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class SecurityUtils {

    public String getCurrentCompanyId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("User not authenticated");
        }

        if (!(authentication.getPrincipal() instanceof UserDetailsImpl user)) {
            throw new BusinessException("Invalid user details");
        }

        if (user.getCompanyId() == null) {
            throw new BusinessException("User is not linked to a company");
        }

        return user.getCompanyId();
    }

    public String getCurrentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("User not authenticated");
        }

        if (!(authentication.getPrincipal() instanceof UserDetailsImpl user)) {
            throw new BusinessException("Invalid user details");
        }

        return user.getId();
    }
}
