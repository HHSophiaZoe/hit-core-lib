package com.hit.spring.autoconfigure;

import com.hit.spring.convert.DataConverter;
import com.hit.spring.excel.CellStyleCreatorFactory;
import com.hit.spring.excel.IExcelService;
import com.hit.spring.excel.impl.ExcelServiceDefaultImpl;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Excel import/export with Apache POI.
 */
@ConditionalOnClass(Workbook.class)
@AutoConfiguration(after = HitSpringAutoConfiguration.class)
public class HitExcelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CellStyleCreatorFactory cellStyleCreatorFactory() {
        return new CellStyleCreatorFactory();
    }

    @Bean
    @ConditionalOnMissingBean(IExcelService.class)
    public ExcelServiceDefaultImpl excelService(CellStyleCreatorFactory cellStyleCreatorFactory, DataConverter dataConverter) {
        return new ExcelServiceDefaultImpl(cellStyleCreatorFactory, dataConverter);
    }
}
