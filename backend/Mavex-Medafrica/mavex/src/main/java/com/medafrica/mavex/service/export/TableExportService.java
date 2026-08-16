package com.medafrica.mavex.service.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Service générique d'export de données tabulaires vers PDF ou Excel.
 * Prend en entrée des en-têtes et des lignes déjà formatées en String,
 * sans connaissance d'un module métier particulier (Order, Shipment, ...).
 */
@Service
@Slf4j
public class TableExportService {

    public byte[] generatePdf(String title, List<String> headers, List<List<String>> rows) {
        try {
            String html = buildHtml(title, headers, rows);
            return renderPdf(html);
        } catch (Exception e) {
            log.error("Échec de la génération du PDF d'export '{}'", title, e);
            throw new IllegalStateException(
                "Impossible de générer l'export PDF \"" + title + "\"", e);
        }
    }

    public byte[] generateExcel(String sheetName, List<String> headers, List<List<String>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(
                new XSSFColor(new byte[]{ (byte) 0xF9, (byte) 0x73, (byte) 0x16 }, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> rowData = rows.get(r);
                for (int c = 0; c < rowData.size(); c++) {
                    row.createCell(c).setCellValue(rowData.get(c));
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Échec de la génération de l'export Excel '{}'", sheetName, e);
            throw new IllegalStateException(
                "Impossible de générer l'export Excel \"" + sheetName + "\"", e);
        }
    }

    private String buildHtml(String title, List<String> headers, List<List<String>> rows) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"/><style>")
            .append("body { font-family: Helvetica, Arial, sans-serif; }")
            .append("h1 { font-size: 18px; margin-bottom: 12px; }")
            .append("table { width: 100%; border-collapse: collapse; }")
            .append("th, td { border: 1px solid #cccccc; padding: 6px 8px; text-align: left; font-size: 11px; }")
            .append("th { background-color: #F97316; color: #ffffff; }")
            .append("</style></head><body>");

        html.append("<h1>").append(escapeHtml(title)).append("</h1>");

        html.append("<table><thead><tr>");
        for (String header : headers) {
            html.append("<th>").append(escapeHtml(header)).append("</th>");
        }
        html.append("</tr></thead><tbody>");

        for (List<String> row : rows) {
            html.append("<tr>");
            for (String cell : row) {
                html.append("<td>").append(escapeHtml(cell)).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table></body></html>");

        return html.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\"", "&quot;");
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de la conversion HTML → PDF de l'export", e);
        }
    }
}
