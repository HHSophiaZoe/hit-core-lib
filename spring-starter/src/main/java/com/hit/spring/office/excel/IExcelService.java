package com.hit.spring.office.excel;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface IExcelService {

    Workbook createWorkbook(InputStream inputStream);

    SXSSFWorkbook createSXSSFWorkbook(InputStream inputStream, int bufferSize);

    ByteArrayResource workbookToResource(Workbook workbook);

    boolean isExcelInvalid(Workbook target, Workbook sample, Class<?> classConfig);

    <T> List<T> getWorkbookData(Workbook workbook, Class<T> classConfig);

//    Sử dụng nếu sử dụng reactive programming rxjava/reactor hoặc tự thiết kế
//
//    default <T, R> Single<SXSSFWorkbook> insertBigDataToWorkbook(SXSSFWorkbook workbook, Class<R> classConfig,
//                                                                 Flowable<List<T>> data, Function<T, R> mapper) {
//        return this.insertBigDataToWorkbook(workbook, classConfig, data, mapper, null, null);
//    }
//
//    default <T, R> Single<SXSSFWorkbook> insertBigDataToWorkbook(SXSSFWorkbook workbook, Class<R> classConfig,
//                                                                 Flowable<List<T>> data, Function<T, R> mapper,
//                                                                 Map<String, String> context) {
//        return this.insertBigDataToWorkbook(workbook, classConfig, data, mapper, context, null);
//    }
//
//    <T, R> Single<SXSSFWorkbook> insertBigDataToWorkbook(SXSSFWorkbook workbook, Class<R> classConfig,
//                                                         Flowable<List<T>> data, Function<T, R> mapper,
//                                                         Map<String, String> context,
//                                                         Map<String, List<String>> dataSheetDropdown);

    default <T> void insertDataToWorkbook(Workbook workbook, Class<T> classConfig, List<T> data) {
        this.insertDataToWorkbook(workbook, classConfig, data, null, null);
    }

    default <T> void insertDataToWorkbook(Workbook workbook, Class<T> classConfig, List<T> data, Map<String, String> context) {
        this.insertDataToWorkbook(workbook, classConfig, data, context, null);
    }

    <T> void insertDataToWorkbook(Workbook workbook, Class<T> classConfig, List<T> data, Map<String, String> context,
                                  Map<String, List<String>> dataSheetDropdown);

}
