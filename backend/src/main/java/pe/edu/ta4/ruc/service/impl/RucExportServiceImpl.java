package pe.edu.ta4.ruc.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.edu.ta4.ruc.model.RucJob;
import pe.edu.ta4.ruc.model.RucJobItem;
import pe.edu.ta4.ruc.service.RucExportService;
import pe.edu.ta4.shared.error.ApiException;

@Service
public class RucExportServiceImpl implements RucExportService {
    private static final String[] HEADERS = {
        "RUC", "Razón social", "Dirección fiscal", "Condición del contribuyente",
        "Fecha de inicio de actividades", "Estado del contribuyente", "Estado de consulta", "Observación",
        "Tipo de contribuyente", "Nombre comercial", "Fecha de inscripción", "Departamento", "Provincia", "Distrito",
        "Teléfono", "Actividad principal (CIIU)", "Actividades secundarias (CIIU)", "Buen contribuyente",
        "Agente de retención", "Agente de percepción"};
    private static final int[] WIDTHS = {14, 48, 70, 18, 20, 18, 18, 55, 28, 36, 20, 16, 16, 20, 16, 45, 45, 18, 50, 40};
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1900, 1, 1);

    @Override
    public byte[] export(RucJob job, List<RucJobItem> items) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(workbook);
            CellStyle date = workbook.createCellStyle();
            date.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle text = workbook.createCellStyle();
            text.setDataFormat(workbook.createDataFormat().getFormat("@"));

            writeResults(workbook.createSheet("Resultados"), items, header, date, text);
            writeSummary(workbook.createSheet("Resumen"), job, header);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw ApiException.unavailable("No se pudo generar el Excel de resultados.");
        }
    }

    private static void writeResults(Sheet sheet, List<RucJobItem> items, CellStyle header, CellStyle date, CellStyle text) {
        Row head = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = head.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(header);
            sheet.setColumnWidth(i, WIDTHS[i] * 256);
        }
        int rowIndex = 1;
        for (RucJobItem item : items) {
            Row row = sheet.createRow(rowIndex++);
            Cell ruc = row.createCell(0);
            ruc.setCellValue(item.getRuc());
            ruc.setCellStyle(text);
            setText(row, 1, item.getRazonSocial());
            setText(row, 2, item.getDireccionFiscal());
            setText(row, 3, item.getCondicionContribuyente());
            LocalDate start = item.getFechaInicioActividades();
            if (start != null && start.isBefore(EXCEL_EPOCH)) {
                // Excel no maneja fechas anteriores a 1900 (se verían como ######): se escribe como texto.
                Cell cell = row.createCell(4);
                cell.setCellValue(start.format(DAY));
                cell.setCellStyle(text);
            } else if (start != null) {
                Cell cell = row.createCell(4);
                cell.setCellValue(start);
                cell.setCellStyle(date);
            }
            setText(row, 5, item.getEstadoContribuyente());
            setText(row, 6, item.getStatus().name());
            setText(row, 7, item.getObservation());
            setText(row, 8, item.getTipoContribuyente());
            setText(row, 9, item.getNombreComercial());
            LocalDate registered = item.getFechaInscripcion();
            if (registered != null) {
                Cell cell = row.createCell(10);
                if (registered.isBefore(EXCEL_EPOCH)) {
                    cell.setCellValue(registered.format(DAY));
                    cell.setCellStyle(text);
                } else {
                    cell.setCellValue(registered);
                    cell.setCellStyle(date);
                }
            }
            setText(row, 11, item.getDepartamento());
            setText(row, 12, item.getProvincia());
            setText(row, 13, item.getDistrito());
            setText(row, 14, item.getTelefono());
            setText(row, 15, item.getActividadPrincipal());
            setText(row, 16, item.getActividadesSecundarias());
            setText(row, 17, item.getBuenContribuyente());
            setText(row, 18, item.getAgenteRetencion());
            setText(row, 19, item.getAgentePercepcion());
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(rowIndex - 1, 0), 0, HEADERS.length - 1));
    }

    private static void writeSummary(Sheet sheet, RucJob job, CellStyle header) {
        String[][] rows = {
            {"Trabajo", job.getId().toString()},
            {"Archivo de origen", job.getOriginalFilename()},
            {"Estado", job.getStatus().name()},
            {"RUC solicitados", String.valueOf(job.getTotal())},
            {"Con datos de SUNAT", String.valueOf(job.getFoundCount())},
            {"Sin datos", String.valueOf(job.getMissingCount())},
            {"No solicitados recibidos", String.valueOf(job.getUnexpectedCount())},
            {"Creado", job.getCreatedAt().format(STAMP)},
            {"Completado", job.getCompletedAt() == null ? "" : job.getCompletedAt().format(STAMP)},
            {"Observaciones", job.getSummary() == null ? "" : job.getSummary()}};
        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i);
            Cell label = row.createCell(0);
            label.setCellValue(rows[i][0]);
            label.setCellStyle(header);
            row.createCell(1).setCellValue(rows[i][1]);
        }
        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 80 * 256);
    }

    private static void setText(Row row, int column, String value) {
        if (value != null) row.createCell(column).setCellValue(value);
    }

    private static CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
