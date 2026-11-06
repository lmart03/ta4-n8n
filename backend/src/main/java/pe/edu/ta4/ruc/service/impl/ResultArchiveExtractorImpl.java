package pe.edu.ta4.ruc.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Service;
import pe.edu.ta4.ruc.RucProperties;
import pe.edu.ta4.ruc.service.ResultArchiveExtractor;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.ProblemIssue;

@Service
public class ResultArchiveExtractorImpl implements ResultArchiveExtractor {
    private static final int BUFFER = 8192;

    private final RucProperties properties;

    public ResultArchiveExtractorImpl(RucProperties properties) {
        this.properties = properties;
    }

    @Override
    public byte[] extractTxt(byte[] zip) {
        byte[] txt = null;
        int txtCount = 0;
        int entries = 0;
        long remaining = properties.maxResultTxtBytes();

        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                entries++;
                if (entries > properties.maxZipEntries()) {
                    throw reject("ZIP_TOO_MANY_ENTRIES", "El ZIP tiene demasiadas entradas (máximo " + properties.maxZipEntries() + ").");
                }
                if (isUnsafe(entry.getName())) {
                    throw reject("ZIP_UNSAFE_ENTRY", "El ZIP contiene una entrada con una ruta no permitida: '" + entry.getName() + "'.");
                }
                if (entry.isDirectory()) continue;

                // Se lee todo con tope: así una entrada ignorada tampoco puede inflarse sin límite.
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                long read = copyLimited(in, out, remaining);
                remaining -= read;
                if (remaining < 0) {
                    throw reject("ZIP_TOO_LARGE", "El contenido descomprimido supera el máximo permitido (" + properties.maxResultTxtBytes() + " bytes).");
                }
                if (entry.getName().toLowerCase(Locale.ROOT).endsWith(".txt")) {
                    txtCount++;
                    txt = out.toByteArray();
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (ZipException e) {
            throw reject("NOT_A_ZIP", "El archivo no es un ZIP válido.");
        } catch (IOException e) {
            throw reject("NOT_A_ZIP", "No se pudo leer el ZIP.");
        }

        if (entries == 0) throw reject("NOT_A_ZIP", "El archivo no es un ZIP válido o está vacío.");
        if (txtCount == 0) throw reject("NO_TXT_IN_ZIP", "El ZIP no contiene el TXT de resultados de SUNAT.");
        if (txtCount > 1) throw reject("MULTIPLE_TXT_IN_ZIP", "El ZIP contiene más de un TXT; se esperaba solo el de SUNAT.");
        return txt;
    }

    /** Nombres absolutos, con {@code ..}, con letra de unidad o con barra invertida: posible zip-slip. */
    private static boolean isUnsafe(String name) {
        if (name.startsWith("/") || name.contains("\\") || name.contains(":")) return true;
        for (String part : name.split("/")) {
            if (part.equals("..")) return true;
        }
        return false;
    }

    /** Copia hasta {@code limit + 1} bytes y devuelve cuántos leyó; más que {@code limit} indica que se pasó. */
    private static long copyLimited(InputStream in, ByteArrayOutputStream out, long limit) throws IOException {
        byte[] buffer = new byte[BUFFER];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > limit) return total;
            out.write(buffer, 0, read);
        }
        return total;
    }

    private static ApiException reject(String code, String detail) {
        return ApiException.invalidResultFile(detail, List.of(ProblemIssue.of(code, detail)));
    }
}
