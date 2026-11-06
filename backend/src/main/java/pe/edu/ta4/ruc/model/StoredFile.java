package pe.edu.ta4.ruc.model;

/** Archivos que se conservan por trabajo (los originales no se modifican: RNF-RUC-03). */
public enum StoredFile {
    INPUT_EXCEL("input.xlsx"),
    INPUT_TXT("input.txt"),
    RESULT_ZIP("result.zip"),
    RESULT_TXT("result.txt");

    private final String fileName;

    StoredFile(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
