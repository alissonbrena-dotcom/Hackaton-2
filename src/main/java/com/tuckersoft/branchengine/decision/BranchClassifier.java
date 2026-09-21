package com.tuckersoft.branchengine.decision;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Clasificacion determinista por reglas propias. Las reglas se evaluan EN ORDEN y la
 * primera que se cumple gana: por eso "Stefan destruye la camara" es RUPTURA_CUARTA_PARED.
 */
public final class BranchClassifier {

    private static final Pattern ANY_LETTER = Pattern.compile("[a-z]");

    private static final List<String> FOURTH_WALL = List.of("netflix", "camara", "espectador", "videojuego");
    private static final List<String> SUSPICION = List.of("vigilan", "simbolo", "conspiracion");
    private static final List<String> REBELLION = List.of("rechaza", "destruye", "desobedece", "renuncia");

    private BranchClassifier() {
    }

    public static BranchType classify(String rawInput) {
        String text = normalize(rawInput);
        if (!ANY_LETTER.matcher(text).find()) return BranchType.ENTRADA_CORRUPTA;
        if (containsAny(text, FOURTH_WALL)) return BranchType.RUPTURA_CUARTA_PARED;
        if (containsAny(text, SUSPICION)) return BranchType.SOSPECHA;
        if (containsAny(text, REBELLION)) return BranchType.REBELDIA;
        return BranchType.OBEDIENCIA;
    }

    /** Minusculas y sin tildes: "CÁMARA", "cámara" y "camara" se comparan igual. */
    static String normalize(String rawInput) {
        return Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}
