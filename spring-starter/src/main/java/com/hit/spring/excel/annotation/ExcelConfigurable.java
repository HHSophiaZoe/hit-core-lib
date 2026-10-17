package com.hit.spring.excel.annotation;

import com.hit.spring.excel.CellStyleCreator;
import com.hit.spring.excel.impl.CellStyleCreatorDefaultImpl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelConfigurable {

    int sheetIndex();

    int headerIndex();

    int startRow();

    int[] placeholderRowIndexes() default {};

    Class<? extends CellStyleCreator> cellStyleCreator() default CellStyleCreatorDefaultImpl.class;
}
