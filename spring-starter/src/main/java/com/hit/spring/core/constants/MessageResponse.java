package com.hit.spring.core.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageResponse {

    public static final String CREATE_SUCCESS = "Create successfully";
    public static final String UPDATE_SUCCESS = "Update successfully";
    public static final String DELETE_SUCCESS = "Delete successfully";
    public static final String RESTORE_SUCCESS = "Restore successfully";
    public static final String LOCK_SUCCESS = "Lock successfully";
    public static final String UNLOCK_SUCCESS = "Unlock successfully";
    public static final String INVALID_SOME_THING_FIELD_IS_REQUIRED = "Trường này là bắt buộc!";
    public static final String INVALID_SOME_THING_FIELD = "Dữ liệu không hợp lệ!";
    public static final String INVALID_FORMAT_PASSWORD = "Mật khẩu không đạt yêu cầu!";
    public static final String NOT_EMPTY_FIELD = "Không thể trống!";
    public static final String ERR_INVALID_FILE = "Định dạng tệp không hợp lệ!";
    public static final String INVALID_FORMAT_SOME_THING_FIELD = "Định dạng không hợp lệ!";

}
