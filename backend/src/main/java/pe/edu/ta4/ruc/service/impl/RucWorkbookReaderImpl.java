package pe.edu.ta4.ruc.service.impl;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import pe.edu.ta4.ruc.RucProperties;
import pe.edu.ta4.ruc.model.RucEntry;
import pe.edu.ta4.ruc.model.RucNumber;
import pe.edu.ta4.ruc.service.RucWorkbookReader;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.ProblemIssue;

@Service
public class RucWorkbookReaderImpl implements RucWorkbookReader {
    private static final String SHEET_NAME = "RUC";
    private static final String RUC_COLUMN = "ruc";

    private final RucProperties properties;

    public RucWorkbookReaderImpl(RucProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<RucEntry> read(byte[] content) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            return readSheet(pickSheet(workbook));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.invalidWorkbook("No se pudo leer el archivo como Excel .xlsx.",
                List.of(ProblemIssue.of("NOT_A_WORKBOOK", "El archivo no es un Excel .xlsx válido.")));
        }
    }

    private static Sheet pickSheet(XSSFWorkbook workbook) {
        Sheet named = workbook.getSheet(SHEET_NAME);
        if (named != null) return named;
        if (workbook.getNumberOfSheets() == 0) {
            throw ApiException.invalidWorkbook("El Excel no tiene hojas.",
                List.of(ProblemIssue.of("MISSING_SHEET", "El libro no contiene ninguna hoja.")));
        }
        return workbook.getSheetAt(0);
    }

    private List<RucEntry> readSheet(Sheet sheet) {
        Row header = sheet.getRow(sheet.getFirstRowNum());
        int rucColumn = findRucColumn(header);
        if (rucColumn < 0) {
            throw ApiException.invalidWorkbook("Falta la columna 'ruc'.",
                List.of(ProblemIssue.of("#/header", "MISSING_RUC_COLUMN", "La primera fila debe tener una columna llamada 'ruc'.")));
        }

        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        List<RucEntry> entries = new ArrayList<>();
        List<ProblemIssue> issues = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isBlank(row, formatter)) continue;
            int excelRow = i + 1;
            String ruc = readRuc(row.getCell(rucColumn), formatter);
            String pointer = "#/rows/" + excelRow + "/ruc";

            if (ruc.isEmpty()) {
                issues.add(ProblemIssue.of(pointer, "RUC_EMPTY", "Fila " + excelRow + ": el RUC está vacío."));
            } else if (!RucNumber.hasValidFormat(ruc)) {
                issues.add(ProblemIssue.of(pointer, "RUC_INVALID_FORMAT",
                    "Fila " + excelRow + ": «" + shorten(ruc) + "» no es un RUC válido: debe tener exactamente 11 dígitos."));
            } else if (!RucNumber.hasValidPrefix(ruc)) {
                issues.add(ProblemIssue.of(pointer, "RUC_INVALID_PREFIX", "Fila " + excelRow + ": el RUC debe empezar con 10, 15, 16, 17 o 20."));
            } else if (!RucNumber.hasValidCheckDigit(ruc)) {
                issues.add(ProblemIssue.of(pointer, "RUC_INVALID_CHECK_DIGIT", "Fila " + excelRow + ": el dígito verificador del RUC " + ruc + " no coincide."));
            } else if (!seen.add(ruc)) {
                issues.add(ProblemIssue.of(pointer, "RUC_DUPLICATE", "Fila " + excelRow + ": el RUC " + ruc + " está duplicado."));
            } else {
                entries.add(new RucEntry(excelRow, ruc));
            }
        }

        if (entries.size() + issues.size() == 0) {
            issues.add(ProblemIssue.of("#/rows", "NO_RUC", "El Excel no tiene ningún RUC debajo del encabezado."));
        } else if (entries.size() > properties.maxRows()) {
            // Solo se avisa del exceso cuando los RUC válidos ya lo superan; si el exceso viene de filas con
            // problemas (por ejemplo una nota debajo de la tabla), arreglar esas filas ya lo resuelve.
            issues.add(ProblemIssue.of("#/rows", "TOO_MANY_RUC",
                "El Excel tiene " + entries.size() + " RUC válidos; el máximo es " + properties.maxRows() + "."));
        }
        if (!issues.isEmpty()) {
            throw ApiException.invalidWorkbook(
                issues.size() == 1 ? "El Excel tiene 1 problema." : "El Excel tiene " + issues.size() + " problemas.", issues);
        }
        return List.copyOf(entries);
    }

    /** Recorta el valor para citarlo en un mensaje sin que una celda enorme lo desborde. */
    private static String shorten(String value) {
        return value.length() <= 24 ? value : value.substring(0, 24) + "…";
    }

    private static int findRucColumn(Row header) {
        if (header == null) return -1;
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        for (int i = 0; i < header.getLastCellNum(); i++) {
            if (RUC_COLUMN.equals(formatter.formatCellValue(header.getCell(i)).trim().toLowerCase(Locale.ROOT))) return i;
        }
        return -1;
    }

    private static boolean isBlank(Row row, DataFormatter formatter) {
        for (int i = row.getFirstCellNum(); i >= 0 && i < row.getLastCellNum(); i++) {
            if (!formatter.formatCellValue(row.getCell(i)).isBlank()) return false;
        }
        return true;
    }

    /** Acepta RUC como texto o como número entero; una fórmula no es un valor confiable. */
    private static String readRuc(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (!Double.isFinite(value) || Math.rint(value) != value) return "";
            return BigDecimal.valueOf(value).toPlainString();
        }
        if (cell.getCellType() == CellType.FORMULA) return "";
        return formatter.formatCellValue(cell).trim();
    }
}
