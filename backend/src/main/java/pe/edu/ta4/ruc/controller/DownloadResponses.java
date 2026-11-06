package pe.edu.ta4.ruc.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pe.edu.ta4.ruc.dto.FileDownload;

/** Convierte un {@link FileDownload} en respuesta HTTP con las cabeceras estándar. */
final class DownloadResponses {
    private DownloadResponses() {
    }

    /**
     * {@code Content-Disposition} con el nombre (RFC 6266 / 8187) y {@code Cache-Control: no-store} (RFC 9111):
     * los resultados tributarios no deben quedar en cachés intermedias.
     */
    static ResponseEntity<byte[]> attachment(FileDownload file) {
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
            .cacheControl(CacheControl.noStore())
            .body(file.content());
    }
}
