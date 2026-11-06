package pe.edu.ta4.ruc.model;

/** Un RUC leído del Excel de entrada, con su fila para poder señalar errores. */
public record RucEntry(int excelRow, String ruc) {
}
