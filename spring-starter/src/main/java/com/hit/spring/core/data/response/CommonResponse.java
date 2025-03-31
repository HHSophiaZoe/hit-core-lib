package com.hit.spring.core.data.response;

import lombok.*;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CommonResponse {

    private Boolean status;

    private String message;

}
