package com.kairon.security.auth;

import com.kairon.domain.enums.Role;

public interface UserAuthProjection {

    String getId();
    String getEmail();
    String getPassword();
    Role getRole();
    String getCompanyId();
    boolean getIsActive();
}
