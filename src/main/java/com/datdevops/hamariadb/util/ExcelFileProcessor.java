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

/**
 * Lớp tiện ích để xử lý (đọc và phân tích) các file Excel.
 * Được sử dụng chủ yếu cho chức năng chuyển tiền theo lô (batch transfer).
 */
@Slf4j
@Component
public class ExcelFileProcessor {

    // Số cột dự kiến trong file Excel.
    private static final int EXPECTED_COLUMNS = 5;

    /**
     * Xử lý một file Excel được tải lên và chuyển đổi nó thành một danh sách các yêu cầu chuyển tiền.
     * @param file Đối tượng MultipartFile đại diện cho file Excel.
     * @return Một danh sách các đối tượng {@link BatchTransferItemRequest}.
     * @throws IOException nếu có lỗi khi đọc file.
     */
    public List<BatchTransferItemRequest> processExcelFile(MultipartFile file) throws IOException {
        List<BatchTransferItemRequest> items = new ArrayList<>();

        // Sử dụng try-with-resources để đảm bảo workbook được đóng sau khi sử dụng.
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            // Lấy sheet đầu tiên từ workbook.
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Bỏ qua dòng tiêu đề (dòng đầu tiên).
            if (rows.hasNext()) {
                rows.next();
            }

            int rowNum = 1; // Bắt đầu từ dòng số 1 (sau tiêu đề).
            while (rows.hasNext()) {
                Row row = rows.next();
                // Phân tích từng dòng để tạo đối tượng BatchTransferItemRequest.
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

    /**
     * Phân tích một dòng (Row) từ file Excel để tạo ra một đối tượng BatchTransferItemRequest.
     * @param row Dòng cần phân tích.
     * @param rowNum Số thứ tự của dòng (để ghi log lỗi).
     * @return Một đối tượng {@link BatchTransferItemRequest} hoặc null nếu dòng trống hoặc không hợp lệ.
     */
    private BatchTransferItemRequest parseRow(Row row, int rowNum) {
        try {
            // Bỏ qua nếu dòng trống.
            if (isRowEmpty(row)) {
                return null;
            }

            BatchTransferItemRequest item = new BatchTransferItemRequest();
            item.setSequenceNumber(rowNum);

            // Đọc dữ liệu từ các ô (cell) theo thứ tự cột.
            // Cột 0: Số tài khoản nhận
            item.setToAccountNumber(getStringCellValue(row.getCell(0)));
            // Cột 1: Mã ngân hàng nhận
            item.setToBankCode(getStringCellValue(row.getCell(1)));
            // Cột 2: Tên người nhận
            item.setToAccountName(getStringCellValue(row.getCell(2)));
            // Cột 3: Số tiền
            item.setAmount(getNumericCellValue(row.getCell(3)));
            // Cột 4: Nội dung
            item.setDescription(getStringCellValue(row.getCell(4)));

            // Xác thực các trường bắt buộc.
            if (item.getToAccountNumber() == null || item.getToAccountNumber().trim().isEmpty()) {
                log.warn("Row {} skipped: Missing account number", rowNum);
                return null; // Bỏ qua dòng nếu thiếu số tài khoản.
            }

            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Row {} skipped: Invalid amount", rowNum);
                return null; // Bỏ qua dòng nếu số tiền không hợp lệ.
            }

            return item;

        } catch (Exception e) {
            log.error("Error parsing row {}: {}", rowNum, e.getMessage());
            return null; // Trả về null nếu có lỗi khi phân tích dòng.
        }
    }

    /**
     * Kiểm tra xem một dòng có hoàn toàn trống không.
     * @param row Dòng cần kiểm tra.
     * @return true nếu dòng trống, ngược lại là false.
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int cellNum = 0; cellNum < EXPECTED_COLUMNS; cellNum++) {
            Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lấy giá trị chuỗi từ một ô (Cell), xử lý cả kiểu số và chuỗi.
     * @param cell Ô cần lấy giá trị.
     * @return Giá trị chuỗi hoặc null.
     */
    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;

        // Xử lý tùy theo kiểu dữ liệu của ô.
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Nếu là số, chuyển đổi thành chuỗi (dùng cho các mã như số tài khoản).
                DataFormatter formatter = new DataFormatter();
                return formatter.formatCellValue(cell);
            default:
                return null;
        }
    }

    /**
     * Lấy giá trị số (BigDecimal) từ một ô (Cell), xử lý cả kiểu số và chuỗi.
     * @param cell Ô cần lấy giá trị.
     * @return Giá trị BigDecimal hoặc null.
     */
    private BigDecimal getNumericCellValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    // Nếu là chuỗi, cố gắng chuyển đổi thành số.
                    // Loại bỏ các ký tự phân cách (ví dụ: ",") trước khi chuyển đổi.
                    String value = cell.getStringCellValue().trim().replace(",", "");
                    if (value.isEmpty()) return null;
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
