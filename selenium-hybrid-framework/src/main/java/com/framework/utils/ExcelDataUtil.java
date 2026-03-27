package com.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * ExcelDataUtil - Reads test data from Excel (.xlsx) files.
 * Used for data-driven testing with TestNG @DataProvider.
 *
 * Excel format:
 *   Row 0 = Header row (column names)
 *   Row 1+ = Data rows
 */
public class ExcelDataUtil {

    private static final Logger logger = LogManager.getLogger(ExcelDataUtil.class);
    private static final String TEST_DATA_PATH = "src/test/resources/testdata/";

    private ExcelDataUtil() {}

    /**
     * Returns all rows from a sheet as a list of maps (column -> value).
     */
    public static List<Map<String, String>> getTestData(String fileName, String sheetName) {
        List<Map<String, String>> dataList = new ArrayList<>();
        String filePath = TEST_DATA_PATH + fileName;

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                logger.error("Sheet '{}' not found in '{}'", sheetName, fileName);
                return dataList;
            }

            Row headerRow = sheet.getRow(0);
            int colCount = headerRow.getLastCellNum();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row dataRow = sheet.getRow(i);
                if (dataRow == null) continue;

                Map<String, String> rowData = new LinkedHashMap<>();
                for (int j = 0; j < colCount; j++) {
                    String header = getCellValue(headerRow.getCell(j));
                    String value  = getCellValue(dataRow.getCell(j));
                    rowData.put(header, value);
                }
                dataList.add(rowData);
            }
            logger.info("Loaded {} rows from sheet '{}' in '{}'",
                    dataList.size(), sheetName, fileName);

        } catch (IOException e) {
            logger.error("Failed to read Excel file '{}': {}", filePath, e.getMessage());
        }
        return dataList;
    }

    /**
     * Returns data as Object[][] for TestNG @DataProvider.
     */
    public static Object[][] getDataProvider(String fileName, String sheetName) {
        List<Map<String, String>> dataList = getTestData(fileName, sheetName);
        Object[][] data = new Object[dataList.size()][1];
        for (int i = 0; i < dataList.size(); i++) {
            data[i][0] = dataList.get(i);
        }
        return data;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default      -> "";
        };
    }
}
