package com.library.util;

import com.library.dto.BorrowVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel 导出工具（借阅记录）
 */
public final class ExcelUtil {

    private static final String[] HEADERS = {"记录ID", "用户名", "书名", "分类", "借出时间", "应还时间", "归还时间", "状态"};
    private static final String[] STATUS = {"借出中", "已还", "逾期"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 有效状态：超期未标记（status=0 但 overdue=true）按逾期展示 */
    private static int effectiveStatus(BorrowVO vo) {
        if (Boolean.TRUE.equals(vo.getOverdue())) {
            return 2;
        }
        return vo.getStatus() == null ? 0 : vo.getStatus();
    }

    private ExcelUtil() {
    }

    /** 生成借阅记录 xlsx 字节数组 */
    public static byte[] exportBorrowRecords(List<BorrowVO> list) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("借阅记录");

            // 表头加粗
            CellStyle headStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headStyle.setFont(font);

            Row head = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            int rowIdx = 1;
            for (BorrowVO vo : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(vo.getId());
                row.createCell(1).setCellValue(vo.getUsername());
                row.createCell(2).setCellValue(vo.getBookTitle());
                row.createCell(3).setCellValue(vo.getCategoryName());
                row.createCell(4).setCellValue(vo.getBorrowTime() == null ? "" : vo.getBorrowTime().format(FMT));
                row.createCell(5).setCellValue(vo.getDueTime() == null ? "" : vo.getDueTime().format(FMT));
                row.createCell(6).setCellValue(vo.getReturnTime() == null ? "" : vo.getReturnTime().format(FMT));
                row.createCell(7).setCellValue(STATUS[effectiveStatus(vo)]);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
