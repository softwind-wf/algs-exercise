package com.ds.university.util;

import com.ds.university.vo.SectionVO;
import com.ds.university.vo.WeeklyScheduleVO;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** 课程表 Excel 导出：与页面周课表一致的「星期 x 时间段」网格 */
public final class ScheduleExcelWriter {

    private ScheduleExcelWriter() {
    }

    public static byte[] toXlsx(WeeklyScheduleVO week, String sheetName, String title)
            throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            List<String> days = week.getDays();
            List<String> periods = week.getPeriods();
            int colCount = 1 + days.size();

            CellStyle titleStyle = wb.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = baseStyle(wb);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle periodStyle = baseStyle(wb);
            periodStyle.setAlignment(HorizontalAlignment.CENTER);
            periodStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Font periodFont = wb.createFont();
            periodFont.setBold(true);
            periodStyle.setFont(periodFont);

            CellStyle cellStyle = baseStyle(wb);
            cellStyle.setAlignment(HorizontalAlignment.LEFT);
            cellStyle.setVerticalAlignment(VerticalAlignment.TOP);

            // 标题行
            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            if (colCount > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));
            }

            // 表头行：时间 \ 星期
            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(22);
            Cell corner = headerRow.createCell(0);
            corner.setCellValue("时间 \\ 星期");
            corner.setCellStyle(headerStyle);
            for (int i = 0; i < days.size(); i++) {
                String d = days.get(i);
                Cell c = headerRow.createCell(i + 1);
                c.setCellValue(week.getDayLabels().getOrDefault(d, d) + "（" + d + "）");
                c.setCellStyle(headerStyle);
            }

            // 数据行：每个时间段一行，格子里放该时段的全部课程
            int rowIdx = 2;
            for (String p : periods) {
                Row row = sheet.createRow(rowIdx);
                Cell pc = row.createCell(0);
                pc.setCellValue(p);
                pc.setCellStyle(periodStyle);

                int maxLines = 1;
                for (int i = 0; i < days.size(); i++) {
                    String d = days.get(i);
                    List<SectionVO> list = week.getCells().get(d + "|" + p);
                    Cell cell = row.createCell(i + 1);
                    cell.setCellStyle(cellStyle);
                    if (list != null && !list.isEmpty()) {
                        String text = blocks(list);
                        cell.setCellValue(text);
                        maxLines = Math.max(maxLines, countLines(text));
                    }
                }
                row.setHeightInPoints(Math.max(24, maxLines * 15 + 8));
                rowIdx++;
            }

            // 空课表提示
            if (days.isEmpty()) {
                Row note = sheet.createRow(2);
                Cell nc = note.createCell(0);
                nc.setCellValue("本学期暂无已选课程，可在「选课 / 退课」页面选课后查看。");
                nc.setCellStyle(cellStyle);
            }

            // 列宽
            sheet.setColumnWidth(0, 15 * 256);
            for (int i = 1; i < colCount; i++) {
                sheet.setColumnWidth(i, 34 * 256);
            }
            sheet.createFreezePane(1, 2);

            wb.write(out);
            return out.toByteArray();
        }
    }

    /** 一个格子里多门课程，块之间空一行，与页面 week-block 内容一致 */
    private static String blocks(List<SectionVO> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            SectionVO s = list.get(i);
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(s.getCourseId()).append('·').append(s.getCourseTitle());
            sb.append('\n').append("班").append(s.getSecId()).append(' ')
                    .append(s.getBuilding()).append(' ').append(s.getRoomNumber());
            if (s.getInstructorNames() != null && !s.getInstructorNames().isEmpty()) {
                sb.append('\n').append(s.getInstructorNames());
            }
        }
        return sb.toString();
    }

    private static int countLines(String text) {
        int n = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    private static CellStyle baseStyle(Workbook wb) {
        CellStyle cs = wb.createCellStyle();
        cs.setBorderTop(BorderStyle.THIN);
        cs.setBorderBottom(BorderStyle.THIN);
        cs.setBorderLeft(BorderStyle.THIN);
        cs.setBorderRight(BorderStyle.THIN);
        cs.setWrapText(true);
        return cs;
    }
}