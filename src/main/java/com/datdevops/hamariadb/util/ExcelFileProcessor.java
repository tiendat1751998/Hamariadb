package com.datdevops.hamariadb.util;


import com.datdevops.hamariadb.dto.request.BatchTransferItemRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
public class ExcelFileProcessor {

    private static final int EXPECTED_COLUMNS = 5;

    public List<BatchTransferItemRequest> processExcelFile(MultipartFile file) throws IOException {
        List<BatchTransferItemRequest> items = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Skip header row
            if (rows.hasNext()) {
                rows.next();
            }

            int rowNum = 1;
            while (rows.hasNext()) {
                Row row = rows.next();
                BatchTransferItemRequest item = parseRow(row, rowNum);
                if (item != null) {
                    items.add(item);
                }
                rowNum++;
            }
        }

        log.info("Processed {} items from Excel file", items.size());
        return items;
    }

    private BatchTransferItemRequest parseRow(Row row, int rowNum) {
        try {
            if (isRowEmpty(row)) {
                return null;
            }

            BatchTransferItemRequest item = new BatchTransferItemRequest();
            item.setSequenceNumber(rowNum);

            // Column 0: To Account Number
            Cell accountCell = row.getCell(0);
            if (accountCell != null) {
                item.setToAccountNumber(getStringCellValue(accountCell));
            }

            // Column 1: To Bank Code
            Cell bankCell = row.getCell(1);
            if (bankCell != null) {
                item.setToBankCode(getStringCellValue(bankCell));
            }

            // Column 2: To Account Name
            Cell nameCell = row.getCell(2);
            if (nameCell != null) {
                item.setToAccountName(getStringCellValue(nameCell));
            }

            // Column 3: Amount
            Cell amountCell = row.getCell(3);
            if (amountCell != null) {
                item.setAmount(getNumericCellValue(amountCell));
            }

            // Column 4: Description
            Cell descCell = row.getCell(4);
            if (descCell != null) {
                item.setDescription(getStringCellValue(descCell));
            }

            // Validate required fields
            if (item.getToAccountNumber() == null || item.getToAccountNumber().trim().isEmpty()) {
                log.warn("Row {} skipped: Missing account number", rowNum);
                return null;
            }

            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Row {} skipped: Invalid amount", rowNum);
                return null;
            }

            return item;

        } catch (Exception e) {
            log.error("Error parsing row {}: {}", rowNum, e.getMessage());
            return null;
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int cellNum = 0; cellNum < EXPECTED_COLUMNS; cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getNumericCellValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String value = cell.getStringCellValue().trim().replace(",", "");
                    return new BigDecimal(value);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid numeric value in cell: {}", cell.getStringCellValue());
            return null;
        }
    }
}
