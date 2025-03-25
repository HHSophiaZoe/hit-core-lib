package com.hit.spring.office.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelCellStyle {

    boolean wrapText() default false;

    // Font styles
    String fontName() default "Calibri";
    short fontSize() default 11;
    boolean bold() default false;
    boolean italic() default false;

    // Horizontal alignment styles
    HorizontalAlignment horizontalAlignment() default HorizontalAlignment.CENTER;
    enum HorizontalAlignment {
        LEFT, CENTER, RIGHT, GENERAL
    }

    // Vertical alignment styles
    enum VerticalAlignment {
        TOP, CENTER, BOTTOM
    }
    VerticalAlignment verticalAlignment() default VerticalAlignment.CENTER;

    // Border styles
    BorderStyle borderTop() default BorderStyle.NONE;
    BorderStyle borderRight() default BorderStyle.NONE;
    BorderStyle borderBottom() default BorderStyle.NONE;
    BorderStyle borderLeft() default BorderStyle.NONE;
    enum BorderStyle {
        NONE, THIN, MEDIUM, THICK, DOUBLE
    }

    // Data format styles
    DataFormat dataFormat() default DataFormat.TEXT;
    enum DataFormat {
        GENERAL, NUMBER, TEXT, DATE, DATETIME, PERCENTAGE, CURRENCY
    }

}
