package pe.edu.ta4.ruc.dto;

/** Archivo listo para descargar: el controller solo le pone las cabeceras HTTP. */
public record FileDownload(String filename, String contentType, byte[] content) {
}
