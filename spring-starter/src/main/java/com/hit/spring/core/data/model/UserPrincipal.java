package com.hit.spring.core.data.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserPrincipal {

    private String ip;

    private String uri;

    private String method;

    private SimpleSecurityUser user;

}
