package com.hit.spring.office.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelCell {

    int index();

    boolean required() default false;

    String defaultValue() default ""; // default value if null on insert

    ExcelCellStyle cellStyle() default @ExcelCellStyle;

}
