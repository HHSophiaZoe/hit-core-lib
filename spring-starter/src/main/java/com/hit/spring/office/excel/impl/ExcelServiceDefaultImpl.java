package com.hit.spring.office.excel.impl;

import com.hit.spring.core.converter.DataConverter;
import com.hit.spring.core.exception.BusinessException;
import com.hit.spring.office.annotation.ExcelCell;
import com.hit.spring.office.annotation.ExcelConfigurable;
import com.hit.spring.office.excel.CellStyleCreator;
import com.hit.spring.office.excel.CellStyleCreatorFactory;
import com.hit.spring.office.excel.IExcelService;
import com.hit.spring.utils.DataUtils;
import com.hit.spring.utils.ReflectUtils;
import com.hit.spring.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.replaceEach;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExcelServiceDefaultImpl implements IExcelService {

    private final CellStyleCreatorFactory cellStyleCreatorFactory;

    private final DataConverter dataConverter;

    private static final String EXCEL_CONFIG_ERR_MESSAGE = "Excel is not configured yet";
    private static final String EXCEL_TEMPLATE_INVALID_ERR_MESSAGE = "Excel Template invalid!!!";

    @Override
    public Workbook createWorkbook(InputStream inputStream) {
        if (inputStream == null) {
            throw new BusinessException("createWorkbook InputStream empty");
        }
        try {
            return WorkbookFactory.create(inputStream);
        } catch (Exception e) {
            log.error("createWorkbook error {}", e.getMessage(), e);
            throw new BusinessException("Create Workbook error");
        }
    }

    @Override
    public SXSSFWorkbook createSXSSFWorkbook(InputStream inputStream, int bufferSize) {
        if (inputStream == null) {
            throw new BusinessException("createSXSSFWorkbook InputStream empty");
        }
        try {
            return new SXSSFWorkbook(new XSSFWorkbook(inputStream), bufferSize);
        } catch (Exception e) {
            log.error("createSXSSFWorkbook error {}", e.getMessage(), e);
            throw new BusinessException("Create SXSSFWorkbook error");
        }
    }

    @Override
    @SneakyThrows
    public ByteArrayResource workbookToResource(Workbook workbook) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());
        } catch (IOException e) {
            throw new BusinessException("Error while converting Workbook to ByteArrayResource", e);
        }
    }

    @Override
    public boolean isExcelInvalid(Workbook target, Workbook sample, Class<?> classConfig) {
        ExcelConfigurable excelConfigurable = ReflectUtils.getAnnotationInClass(classConfig, ExcelConfigurable.class)
                .orElseThrow(() -> new BusinessException(EXCEL_CONFIG_ERR_MESSAGE));
        Collection<ExcelCell> excelCells = ReflectUtils.getListAnnotationInFields(classConfig, ExcelCell.class);

        Sheet sheetTarget = target.getSheetAt(excelConfigurable.sheetIndex());
        Row rowHeaderTarget = sheetTarget.getRow(excelConfigurable.headerIndex());
        List<String> targetHeader = this.getLabelByConfig(rowHeaderTarget, excelCells);

        Sheet sheetSample = sample.getSheetAt(excelConfigurable.sheetIndex());
        Row rowHeaderSample = sheetSample.getRow(excelConfigurable.headerIndex());
        List<String> sampleHeader = this.getLabelByConfig(rowHeaderSample, excelCells);

        return target.getNumberOfSheets() != sample.getNumberOfSheets() || !areHeadersEqual(targetHeader, sampleHeader);
    }

    @Override
    public <T> List<T> getWorkbookData(Workbook workbook, Class<T> classConfig) {
        Objects.requireNonNull(workbook, "Workbook must not be null");
        ExcelConfigurable excelConfigurable = ReflectUtils.getAnnotationInClass(classConfig, ExcelConfigurable.class)
                .orElseThrow(() -> new BusinessException(EXCEL_CONFIG_ERR_MESSAGE));
        Map<String, ExcelCell> excelCells = ReflectUtils.getMapAnnotationInFields(classConfig, ExcelCell.class);

        Sheet sheet = workbook.getSheetAt(excelConfigurable.sheetIndex());
        if (sheet == null) {
            log.debug("Workbook not found sheet index {}", excelConfigurable.sheetIndex());
            return Collections.emptyList();
        }

        List<T> data = new ArrayList<>();
        int countRow = 0;
        for (Row row : sheet) {
            if (countRow < excelConfigurable.startRow()) {
                countRow++;
                continue;
            }
            T rowData = this.getRowData(row, excelCells, classConfig);
            data.add(rowData);
            countRow++;
        }
        return data;
    }

//    @Override
//    public <T, R> Single<SXSSFWorkbook> insertBigDataToWorkbook(SXSSFWorkbook workbook, Class<R> classConfig,
//                                                                Flowable<List<T>> flowableData, Function<T, R> mapper,
//                                                                Map<String, String> context,
//                                                                Map<String, List<String>> dataSheetDropdown) {
//        Objects.requireNonNull(workbook, "Workbook must not be null");
//        ExcelConfigurable excelConfigurable = ReflectUtils.getAnnotationInClass(classConfig, ExcelConfigurable.class)
//                .orElseThrow(() -> new BusinessException(EXCEL_CONFIG_ERR_MESSAGE));
//        Map<String, ExcelCell> excelCells = ReflectUtils.getMapAnnotationInFields(classConfig, ExcelCell.class);
//        SXSSFSheet sheet = workbook.getSheetAt(excelConfigurable.sheetIndex());
//        if (ObjectUtils.isEmpty(sheet)) {
//            throw new BusinessException(EXCEL_TEMPLATE_INVALID_ERR_MESSAGE);
//        }
//        XSSFSheet xssfSheet = workbook.getXSSFWorkbook().getSheetAt(excelConfigurable.sheetIndex());
//
//        Single<SXSSFWorkbook> preProcessTask = Single.fromCallable(() -> {
//            this.processContext(xssfSheet, context, excelConfigurable.placeholderRowIndexes());
//            return workbook;
//        });
//        return preProcessTask
//                .flatMap(result ->
//                        this.streamingData(result, sheet, flowableData, mapper, excelConfigurable, excelCells)
//                                .map(workbookResult -> {
//                                    this.insertDataToSheetSuggest(workbookResult, dataSheetDropdown);
//                                    return workbookResult;
//                                })
//                )
//                .onErrorResumeNext(error -> {
//                    log.error("insertBigDataToWorkbook failed: {}", error.getMessage());
//                    return Single.error(new BusinessException("Insert data to Workbook error"));
//                });
//    }
//
//    private <T, R> Single<SXSSFWorkbook> streamingData(SXSSFWorkbook workbook, SXSSFSheet sheet, Flowable<List<T>> flowableData,
//                                                       Function<T, R> mapper, ExcelConfigurable excelConfigurable,
//                                                       Map<String, ExcelCell> excelCells) {
//        CellStyleCreator cellStyleCreator = cellStyleCreatorFactory.createCellStyleCreator(workbook, excelConfigurable.cellStyleCreator());
//        AtomicInteger currentRowIdx = new AtomicInteger(excelConfigurable.startRow());
//        return flowableData
//                .doOnNext(dataList -> {
//                    for (T data : dataList) {
//                        R dataMapper = mapper.apply(data);
//                        SXSSFRow currentRow = sheet.createRow(currentRowIdx.get());
//                        // Insert data into the row
//                        this.insertDataToRow(cellStyleCreator, currentRow, dataMapper, excelCells);
//                        currentRowIdx.getAndIncrement();
//                    }
//                })
//                .doOnError(error -> log.error("Error while processing data: {}", error.getMessage(), error))
//                .ignoreElements()
//                .andThen(Single.just(workbook));
//    }

    @Override
    public <T> void insertDataToWorkbook(Workbook workbook, Class<T> classConfig, List<T> dataList,
                                         Map<String, String> context, Map<String, List<String>> dataSheetDropdown) {
        Objects.requireNonNull(workbook, "Workbook must not be null");
        ExcelConfigurable excelConfigurable = ReflectUtils.getAnnotationInClass(classConfig, ExcelConfigurable.class)
                .orElseThrow(() -> new BusinessException(EXCEL_CONFIG_ERR_MESSAGE));
        Map<String, ExcelCell> excelCells = ReflectUtils.getMapAnnotationInFields(classConfig, ExcelCell.class);

        Sheet sheet = workbook.getSheetAt(excelConfigurable.sheetIndex());
        if (ObjectUtils.isEmpty(sheet)) {
            throw new BusinessException(EXCEL_TEMPLATE_INVALID_ERR_MESSAGE);
        }
        this.processContext(sheet, context, excelConfigurable.placeholderRowIndexes());

        CellStyleCreator cellStyleCreator = cellStyleCreatorFactory.createCellStyleCreator(workbook, excelConfigurable.cellStyleCreator());
        int currentRowIdx = excelConfigurable.startRow();
        for (T data : dataList) {
            Row currentRow = sheet.getRow(currentRowIdx);
            if (currentRow == null) {
                currentRow = sheet.createRow(currentRowIdx);
            }
            // insert data to row
            this.insertDataToRow(cellStyleCreator, currentRow, data, excelCells);
            currentRowIdx++;
        }
        this.insertDataToSheetSuggest(workbook, dataSheetDropdown);
    }

    private <T> T getRowData(Row row, Map<String, ExcelCell> excelCells, Class<T> classConfig) {
        try {
            T instance = classConfig.getDeclaredConstructor().newInstance();
            excelCells.forEach((fieldName, excelCell) -> {
                Method setter = ReflectUtils.getSetter(classConfig, fieldName);
                Cell cell = row.getCell(excelCell.index());
                if (setter != null && cell != null) {
                    Object cellValue;
                    switch (cell.getCellType()) {
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                cellValue = cell.getDateCellValue();
                            } else {
                                cellValue = cell.getNumericCellValue();
                            }
                            break;
                        case STRING:
                            cellValue = cell.getStringCellValue();
                            break;
                        case BOOLEAN:
                            cellValue = cell.getBooleanCellValue();
                            break;
                        default:
                            cellValue = null;
                            break;
                    }
                    this.setFieldValue(instance, setter, cellValue);
                }
            });
            return instance;
        } catch (Exception e) {
            log.error("getRowData error: {}", e.getMessage(), e);
            return null;
        }
    }

    private boolean areHeadersEqual(List<String> targetHeader, List<String> sampleHeader) {
        boolean isValid = false;
        int i = 0;
        int j = 0;
        while (i < targetHeader.size() || j < sampleHeader.size()) {
            if (targetHeader.get(i).equals(sampleHeader.get(j))) {
                isValid = true;
                i++;
                j++;
            } else {
                isValid = false;
                break;
            }
        }
        return !isValid;
    }

    private List<String> getLabelByConfig(Row row, Collection<ExcelCell> excelCells) {
        List<String> listHeader = new ArrayList<>();
        if (row == null) return listHeader;
        try {
            for (ExcelCell excelCell : excelCells) {
                Cell cell = row.getCell(excelCell.index());
                if (cell != null) {
                    Object cellValue = cell.getStringCellValue();
                    listHeader.add(DataUtils.safeToString(cellValue));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return listHeader;
    }

    private void setFieldValue(Object instance, Method setter, Object value) {
        try {
            Class<?> paramType = setter.getParameterTypes()[0];
            setter.invoke(instance, dataConverter.convertValue(value, paramType));
        } catch (Exception e) {
            log.error("setFieldValue error: {}", e.getMessage(), e);
        }
    }

    private <T> void insertDataToRow(CellStyleCreator cellStyleCreator, Row currentRow, T data, Map<String, ExcelCell> excelCells) {
        excelCells.forEach((fieldName, excelCell) -> {
            Object fieldValue = ReflectUtils.getValueByFieldName(data, fieldName);
            if (excelCell.required() && fieldValue == null) {
                throw new BusinessException(fieldName + " must not be null !!!");
            }
            String cellValue;
            if (fieldValue == null) {
                cellValue = excelCell.defaultValue();
            } else {
                cellValue = String.valueOf(fieldValue);
            }

            CellStyle cellStyle = cellStyleCreator.create(excelCell.cellStyle());
            Cell currentCell = Optional.ofNullable(currentRow.getCell(excelCell.index()))
                    .orElse(currentRow.createCell(excelCell.index()));
            currentCell.setCellValue(String.valueOf(cellValue));
            currentCell.setCellStyle(cellStyle);
        });
    }

    private void processContext(Sheet sheet, Map<String, String> context, int[] placeholderRowIndexes) {
        if (ArrayUtils.isEmpty(placeholderRowIndexes) || ObjectUtils.isEmpty(context)) return;
        String[] placeholders = context.keySet().stream().map(key -> String.format("${%s}", key)).toArray(String[]::new);
        String[] replacements = context.values().toArray(new String[0]);
        for (int rowIndex : placeholderRowIndexes) {
            Row currentRow = sheet.getRow(rowIndex);
            if (currentRow == null) {
                continue;
            }
            for (Cell currentCell : currentRow) {
                if (currentCell == null || currentCell.getCellType() != CellType.STRING) {
                    continue;
                }
                String cellValue = currentCell.getStringCellValue();
                if (isNotBlank(cellValue)) {
                    String replacedValue = replaceEach(cellValue, placeholders, replacements);
                    currentCell.setCellValue(replacedValue);
                }
            }
        }
    }

    public void insertDataToSheetSuggest(Workbook workbook, Map<String, List<String>> dataSheetDropdown) {
        if (ObjectUtils.isEmpty(dataSheetDropdown)) return;
        dataSheetDropdown.forEach((key, value) -> {
            int sheetNum = workbook.getSheetIndex(key);
            Sheet sheet = workbook.getSheetAt(sheetNum);
            AtomicInteger rowIndex = new AtomicInteger(0);
            if (ObjectUtils.isNotEmpty(value)) {
                value.forEach(val -> {
                    Row row = sheet.createRow(rowIndex.get());
                    Cell cell = row.createCell(0);
                    cell.setCellValue(val);
                    rowIndex.getAndIncrement();
                });
                rowIndex.set(0);
            }
        });
    }

}