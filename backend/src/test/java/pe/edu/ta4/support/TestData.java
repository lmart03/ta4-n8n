package pe.edu.ta4.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Datos de prueba compartidos: fixtures reales de SUNAT, Excel y ZIP armados al vuelo. */
public final class TestData {
    public static final String SUNAT_RESULT = "fixtures/RM20261106104906643.txt";
    public static final String SUNAT_INPUT = "fixtures/consulta_20_ruc_formato_sunat.txt";

    private static final int[] WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private TestData() {
    }

    public static byte[] resource(String path) {
        try (InputStream in = TestData.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Falta el recurso de prueba " + path);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Los 20 RUC reales del archivo que se envía a SUNAT, en su orden. */
    public static List<String> twentyRucs() {
        List<String> rucs = new ArrayList<>();
        for (String line : new String(resource(SUNAT_INPUT), StandardCharsets.UTF_8).split("\\R")) {
            if (!line.isBlank()) rucs.add(line.substring(0, line.indexOf('|')));
        }
        return rucs;
    }

    /** Un RUC sintético con dígito verificador correcto (empieza con 20). */
    public static String validRuc(int n) {
        String base = String.format("20%08d", n);
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) sum += (base.charAt(i) - '0') * WEIGHTS[i];
        return base + (11 - (sum % 11)) % 10;
    }

    /**
     * Excel con la hoja y cabecera indicadas. Cada valor es una fila: {@code String} (texto), {@code Double}
     * (celda numérica) o {@code null} (fila vacía).
     */
    public static byte[] workbook(String sheetName, String header, Object... values) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(sheetName);
            sheet.createRow(0).createCell(0).setCellValue(header);
            sheet.getRow(0).createCell(1).setCellValue("empresa_referencial");
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) continue;
                var row = sheet.createRow(i + 1);
                if (values[i] instanceof Double number) row.createCell(0).setCellValue(number);
                else row.createCell(0).setCellValue(values[i].toString());
                row.createCell(1).setCellValue("Empresa " + (i + 1));
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] workbookOf(List<String> rucs) {
        return workbook("RUC", "ruc", rucs.toArray());
    }

    /** ZIP con las entradas dadas, en orden: pares nombre → contenido. */
    public static byte[] zip(Object... namesAndContents) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                zip.putNextEntry(new ZipEntry((String) namesAndContents[i]));
                Object content = namesAndContents[i + 1];
                zip.write(content instanceof byte[] bytes ? bytes : content.toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] sunatZip() {
        return zip("RM20261106104906643.txt", resource(SUNAT_RESULT));
    }

    /** El TXT de SUNAT sin algunas líneas de datos (por número de RUC), para simular RUC ausentes. */
    public static byte[] sunatResultWithout(String... rucs) {
        StringBuilder text = new StringBuilder();
        for (String line : new String(resource(SUNAT_RESULT), StandardCharsets.UTF_8).split("\\R", -1)) {
            boolean drop = false;
            for (String ruc : rucs) drop |= line.startsWith(ruc + "|");
            if (!drop) text.append(line).append("\r\n");
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }
}
