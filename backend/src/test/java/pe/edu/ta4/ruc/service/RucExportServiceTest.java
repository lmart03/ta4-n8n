package pe.edu.ta4.ruc.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import pe.edu.ta4.ruc.model.RucJob;
import pe.edu.ta4.ruc.model.RucJobItem;
import pe.edu.ta4.ruc.model.SunatRecord;
import pe.edu.ta4.ruc.service.impl.RucExportServiceImpl;

class RucExportServiceTest {
    private final RucExportService exporter = new RucExportServiceImpl();

    @Test
    void writesResultsAndSummarySheets() throws Exception {
        UUID jobId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.parse("2026-11-06T15:30:00Z");
        RucJob job = new RucJob(jobId, 2, "ruc.xlsx", now);
        RucJobItem found = new RucJobItem(UUID.randomUUID(), jobId, 2, "20100047218");
        found.markFound(new SunatRecord("20100047218", "BANCO DE CREDITO DEL PERU", "JR. UNO, LA MOLINA, LIMA, LIMA",
            "HABIDO", "ACTIVO", LocalDate.of(1889, 4, 9)));
        RucJobItem missing = new RucJobItem(UUID.randomUUID(), jobId, 3, "20100053455");
        missing.markNotFound("SUNAT no devolvió datos para este RUC.");

        RucJobItem modern = new RucJobItem(UUID.randomUUID(), jobId, 4, "20536557858");
        modern.markFound(new SunatRecord("20536557858", "HOMECENTERS PERUANOS S.A.", "AV. AVIACION 2405", "HABIDO", "ACTIVO",
            LocalDate.of(2010, 6, 22)));

        byte[] bytes = exporter.export(job, List.of(found, missing, modern));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("Resultados");
            assertThat(workbook.getSheetName(1)).isEqualTo("Resumen");

            var sheet = workbook.getSheet("Resultados");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("RUC");
            assertThat(sheet.getRow(0).getCell(3).getStringCellValue()).isEqualTo("Condición del contribuyente");

            var first = sheet.getRow(1);
            assertThat(first.getCell(0).getCellType()).isEqualTo(CellType.STRING);
            assertThat(first.getCell(0).getStringCellValue()).isEqualTo("20100047218");
            assertThat(first.getCell(1).getStringCellValue()).isEqualTo("BANCO DE CREDITO DEL PERU");
            assertThat(first.getCell(2).getStringCellValue()).isEqualTo("JR. UNO, LA MOLINA, LIMA, LIMA");
            assertThat(first.getCell(3).getStringCellValue()).isEqualTo("HABIDO");
            // El BCP inició en 1889: Excel no soporta fechas anteriores a 1900, así que va como texto dd/MM/yyyy.
            assertThat(first.getCell(4).getCellType()).isEqualTo(CellType.STRING);
            assertThat(first.getCell(4).getStringCellValue()).isEqualTo("09/04/1889");
            assertThat(first.getCell(6).getStringCellValue()).isEqualTo("ENCONTRADO");

            var second = sheet.getRow(2);
            assertThat(second.getCell(0).getStringCellValue()).isEqualTo("20100053455");
            assertThat(second.getCell(1)).isNull();
            assertThat(second.getCell(6).getStringCellValue()).isEqualTo("NO_ENCONTRADO");
            assertThat(second.getCell(7).getStringCellValue()).contains("SUNAT no devolvió");

            var recent = sheet.getRow(3);
            assertThat(DateUtil.isCellDateFormatted(recent.getCell(4))).isTrue();
            assertThat(recent.getCell(4).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2010, 6, 22));

            assertThat(workbook.getSheet("Resumen").getRow(0).getCell(1).getStringCellValue()).isEqualTo(jobId.toString());
        }
    }
}
