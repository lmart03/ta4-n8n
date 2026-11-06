package pe.edu.ta4.ruc.service;

public interface ResultArchiveExtractor {
    /**
     * Saca de forma segura el TXT de resultados de un ZIP de SUNAT. Todo se lee en memoria (nunca se escribe
     * una entrada en disco), se rechazan nombres con rutas fuera del archivo, se limita el tamaño
     * descomprimido y se exige exactamente un {@code .txt}. Otras entradas se ignoran.
     * Lanza un 422 si el ZIP no cumple.
     */
    byte[] extractTxt(byte[] zip);
}
