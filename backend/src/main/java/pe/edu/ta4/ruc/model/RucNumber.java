package pe.edu.ta4.ruc.model;

import java.util.Set;

/** Reglas de validación del número de RUC peruano (11 dígitos, prefijo conocido y dígito verificador). */
public final class RucNumber {
    private static final int[] WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final Set<String> PREFIXES = Set.of("10", "15", "16", "17", "20");

    private RucNumber() {
    }

    public static boolean hasValidFormat(String ruc) {
        if (ruc == null || ruc.length() != 11) return false;
        for (int i = 0; i < 11; i++) {
            char c = ruc.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    public static boolean hasValidPrefix(String ruc) {
        return hasValidFormat(ruc) && PREFIXES.contains(ruc.substring(0, 2));
    }

    public static boolean hasValidCheckDigit(String ruc) {
        if (!hasValidFormat(ruc)) return false;
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (ruc.charAt(i) - '0') * WEIGHTS[i];
        }
        int digit = (11 - (sum % 11)) % 10;
        return digit == ruc.charAt(10) - '0';
    }
}
