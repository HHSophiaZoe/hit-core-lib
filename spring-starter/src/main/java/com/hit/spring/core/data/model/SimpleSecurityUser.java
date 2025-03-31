package com.hit.spring.core.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SimpleSecurityUser {

    private String id;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private List<String> authorities;

    public boolean isSystemAdmin() {
        return "system_admin".equals(id);
    }

    public static SimpleSecurityUser initSystemAdmin() {
        return SimpleSecurityUser.builder()
                .id("system_admin")
                .build();
    }
}
