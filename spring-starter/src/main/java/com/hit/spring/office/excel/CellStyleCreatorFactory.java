package com.hit.spring.office.excel;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

@Component
public class CellStyleCreatorFactory {

    public CellStyleCreator createCellStyleCreator(Workbook workbook, Class<? extends CellStyleCreator> creatorClass) {
        try {
            return creatorClass.getConstructor(Workbook.class).newInstance(workbook);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create CellStyleCreator instance", e);
        }
    }

}
