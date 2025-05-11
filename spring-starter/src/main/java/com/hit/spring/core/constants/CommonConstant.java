package com.hit.spring.core.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonConstant {

    public static final Integer PAGE_SIZE_DEFAULT = 10;
    public static final Integer ZERO_INT_VALUE = 0;
    public static final Integer ONE_INT_VALUE = 1;
    public static final Integer TWO_INT_VALUE = 2;
    public static final Integer THREE_INT_VALUE = 3;
    public static final Long ZERO_VALUE = 0L;
    public static final Long ONE_VALUE = 1L;
    public static final Long TWO_VALUE = 2L;
    public static final Long THREE_VALUE = 3L;
    public static final String EMPTY_STRING = "";
    public static final String BEARER_TOKEN = "Bearer";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CommonSymbol {

        public static final String SPACE = " ";

        public static final String DOT = ".";

        public static final String BACKSLASH = "\\";

        public static final String COMMA = ",";

        public static final String DASH = "-";

        public static final String SHIFT_DASH = "_";

        public static final String COLON = ":";

        public static final String FORWARD_SLASH = "/";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ContentDisposition {

        public static final String ATTACHMENT = "attachment";

    }

}
