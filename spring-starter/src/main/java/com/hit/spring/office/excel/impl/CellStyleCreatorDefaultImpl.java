package com.hit.spring.office.excel.impl;

import com.hit.spring.office.annotation.ExcelCellStyle;
import com.hit.spring.office.excel.CellStyleCreator;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CellStyleCreatorDefaultImpl extends CellStyleCreator {

    private final Map<String, CellStyle> styleCache = new HashMap<>();

    public CellStyleCreatorDefaultImpl(Workbook workbook) {
        super(workbook);
    }

    @Override
    public CellStyle create(ExcelCellStyle excelCellStyle) {
        String styleKey = this.createStyleKey(excelCellStyle);
        log.trace("styleKey = {}", styleKey);
        return styleCache.computeIfAbsent(styleKey, k -> {
            log.trace("create new style for styleKey = {}", k);
            return this.createCellStyle(excelCellStyle);
        });
    }

}
