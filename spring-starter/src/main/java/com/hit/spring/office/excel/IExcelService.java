package com.hit.spring.office.excel;

import com.hit.spring.core.reactive.DataStream;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface IExcelService {

    Workbook createWorkbook(InputStream inputStream);

    SXSSFWorkbook createSXSSFWorkbook(InputStream inputStream, int bufferSize);

    ByteArrayResource workbookToResource(Workbook workbook);

    boolean isExcelInvalid(Workbook target, Workbook sample, Class<?> classConfig);

    <T> List<T> getWorkbookData(Workbook workbook, Class<T> classConfig);

    default <T, R> void streamDataToWorkbook(SXSSFWorkbook workbook, Class<R> classConfig, DataStream<List<T>> dataStream, Function<T, R> mapper) {
        this.streamDataToWorkbook(workbook, classConfig, dataStream, mapper, null, null);
    }

    default <T, R> void streamDataToWorkbook(SXSSFWorkbook workbook, Class<R> classConfig, DataStream<List<T>> dataStream, Function<T, R> mapper, Map<String, String> context) {
        this.streamDataToWorkbook(workbook, classConfig, dataStream, mapper, context, null);
    }

    <T, R> void streamDataToWorkbook(SXSSFWorkbook workbook, Class<R> classConfig, DataStream<List<T>> dataStream, Function<T, R> mapper, Map<String, String> context, Map<String, List<String>> dataSheetDropdown);

    default <T> void insertDataToWorkbook(Workbook workbook, Class<T> classConfig, List<T> data) {
        this.insertDataToWorkbook(workbook, classConfig, data, null, null);
    }

    default <T> void insertDataToWorkbook(Workbook workbook, Class<T> classConfig, List<T> data, Map<String, String> context) {
        this.insertDataToWorkbook(workbook, classConfig, data, context, null);
    }

    <T> void insertDataToWorkbook(Workbook workbook, Class<T> classConfig, List<T> data, Map<String, String> context, Map<String, List<String>> dataSheetDropdown);

}
