package com.hit.spring.office.excel;

import com.hit.spring.office.annotation.ExcelCellStyle;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

@Slf4j
public abstract class CellStyleCreator {

    protected final Workbook workbook;

    protected CellStyleCreator(Workbook workbook) {
        this.workbook = workbook;
    }

    public abstract CellStyle create(ExcelCellStyle excelCellStyle);

    protected CellStyle createCellStyle(ExcelCellStyle excelCellStyle) {
        CellStyle cellStyle = workbook.createCellStyle();
        this.setFont(cellStyle, excelCellStyle);
        this.setBorderStyle(cellStyle, excelCellStyle);
        this.setAlignment(cellStyle, excelCellStyle.horizontalAlignment(), excelCellStyle.verticalAlignment());
        this.setDataFormat(cellStyle, excelCellStyle.dataFormat());
        // Set other properties
        cellStyle.setWrapText(excelCellStyle.wrapText());
        return cellStyle;
    }

    protected String createStyleKey(ExcelCellStyle excelCellStyle) {
        // Generate a unique key based on all attributes of the annotation
        StringBuilder key = new StringBuilder();
        key.append("ExcelCellStyle{");
        key.append("wrapText=").append(excelCellStyle.wrapText()).append(",");
        key.append("fontName=").append(excelCellStyle.fontName()).append(",");
        key.append("fontSize=").append(excelCellStyle.fontSize()).append(",");
        key.append("bold=").append(excelCellStyle.bold()).append(",");
        key.append("italic=").append(excelCellStyle.italic()).append(",");
        key.append("horizontalAlignment=").append(excelCellStyle.horizontalAlignment()).append(",");
        key.append("verticalAlignment=").append(excelCellStyle.verticalAlignment()).append(",");
        key.append("borderTop=").append(excelCellStyle.borderTop()).append(",");
        key.append("borderRight=").append(excelCellStyle.borderRight()).append(",");
        key.append("borderBottom=").append(excelCellStyle.borderBottom()).append(",");
        key.append("borderLeft=").append(excelCellStyle.borderLeft()).append(",");
        key.append("dataFormat=").append(excelCellStyle.dataFormat());
        key.append("}");
        return key.toString();
    }

    protected void setFont(CellStyle cellStyle, ExcelCellStyle excelCellStyle) {
        Font font = workbook.createFont();
        font.setFontName(excelCellStyle.fontName());
        font.setFontHeightInPoints(excelCellStyle.fontSize());
        font.setBold(excelCellStyle.bold());
        font.setItalic(excelCellStyle.italic());
        cellStyle.setFont(font);
    }

    protected void setAlignment(CellStyle cellStyle, ExcelCellStyle.HorizontalAlignment horizontalAlignment,
                                ExcelCellStyle.VerticalAlignment verticalAlignment) {
        switch (horizontalAlignment) {
            case LEFT:
                cellStyle.setAlignment(HorizontalAlignment.LEFT);
                break;
            case CENTER:
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                break;
            case RIGHT:
                cellStyle.setAlignment(HorizontalAlignment.RIGHT);
                break;
            default:
                cellStyle.setAlignment(HorizontalAlignment.GENERAL);
        }
        switch (verticalAlignment) {
            case TOP:
                cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
                break;
            case BOTTOM:
                cellStyle.setVerticalAlignment(VerticalAlignment.BOTTOM);
                break;
            case CENTER:
            default:
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        }
    }

    protected void setBorderStyle(CellStyle cellStyle, ExcelCellStyle excelCellStyle) {
        cellStyle.setBorderTop(this.convertBorderStyle(excelCellStyle.borderTop()));
        cellStyle.setBorderRight(this.convertBorderStyle(excelCellStyle.borderRight()));
        cellStyle.setBorderBottom(this.convertBorderStyle(excelCellStyle.borderBottom()));
        cellStyle.setBorderLeft(this.convertBorderStyle(excelCellStyle.borderLeft()));
    }

    protected void setDataFormat(CellStyle cellStyle, ExcelCellStyle.DataFormat format) {
        DataFormat dataFormat = workbook.createDataFormat();
        short dataFormatCode = switch (format) {
            case NUMBER -> dataFormat.getFormat("#,##0");
            case DATE -> dataFormat.getFormat("dd/MM/yyyy");
            case DATETIME -> dataFormat.getFormat("dd/MM/yyyy HH:mm:ss");
            case PERCENTAGE -> dataFormat.getFormat("0.00%");
            case CURRENCY -> dataFormat.getFormat("#,##0.00 ₫");
            case TEXT -> dataFormat.getFormat("@");
            default -> dataFormat.getFormat("General");
        };
        cellStyle.setDataFormat(dataFormatCode);
    }

    private BorderStyle convertBorderStyle(ExcelCellStyle.BorderStyle borderStyle) {
        return switch (borderStyle) {
            case THIN -> BorderStyle.THIN;
            case MEDIUM -> BorderStyle.MEDIUM;
            case THICK -> BorderStyle.THICK;
            case DOUBLE -> BorderStyle.DOUBLE;
            default -> BorderStyle.NONE;
        };
    }

}
