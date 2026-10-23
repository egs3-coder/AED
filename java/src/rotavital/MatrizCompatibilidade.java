package rotavital;

/**
 * Matriz de compatibilidade ABO/Rh [doador][receptor].
 * DIDÁTICA: não substitui protocolo clínico/imuno-hematológico.
 *
 * C++:  const int COMPAT_HEMACIAS[8][8] = {...};  bool compativel(...)
 * Java: static final int[][] + método estático. A consulta continua O(1):
 *       dois acessos por índice (ordinal() do enum), sem laço.
 */
public final class MatrizCompatibilidade {

    // Hemácias (e, didaticamente, plaquetas): doador não pode levar antígeno
    // que o receptor não tenha. O- = doador universal.
    private static final int[][] HEMACIAS = {
        //         receptor: O-  O+  A-  A+  B-  B+ AB- AB+
        /* doador O-  */   { 1,  1,  1,  1,  1,  1,  1,  1 },
        /* doador O+  */   { 0,  1,  0,  1,  0,  1,  0,  1 },
        /* doador A-  */   { 0,  0,  1,  1,  0,  0,  1,  1 },
        /* doador A+  */   { 0,  0,  0,  1,  0,  0,  0,  1 },
        /* doador B-  */   { 0,  0,  0,  0,  1,  1,  1,  1 },
        /* doador B+  */   { 0,  0,  0,  0,  0,  1,  0,  1 },
        /* doador AB- */   { 0,  0,  0,  0,  0,  0,  1,  1 },
        /* doador AB+ */   { 0,  0,  0,  0,  0,  0,  0,  1 },
    };

    // Plasma: lógica invertida (leva anticorpos). AB = doador universal; Rh ignorado.
    private static final int[][] PLASMA = {
        //         receptor: O-  O+  A-  A+  B-  B+ AB- AB+
        /* doador O-  */   { 1,  1,  0,  0,  0,  0,  0,  0 },
        /* doador O+  */   { 1,  1,  0,  0,  0,  0,  0,  0 },
        /* doador A-  */   { 1,  1,  1,  1,  0,  0,  0,  0 },
        /* doador A+  */   { 1,  1,  1,  1,  0,  0,  0,  0 },
        /* doador B-  */   { 1,  1,  0,  0,  1,  1,  0,  0 },
        /* doador B+  */   { 1,  1,  0,  0,  1,  1,  0,  0 },
        /* doador AB- */   { 1,  1,  1,  1,  1,  1,  1,  1 },
        /* doador AB+ */   { 1,  1,  1,  1,  1,  1,  1,  1 },
    };

    private MatrizCompatibilidade() { }

    public static boolean compativel(Componente comp, TipoSanguineo doador, TipoSanguineo receptor) {
        // C++: COMPAT_PLASMA[doador][receptor]  ->  Java: PLASMA[doador.ordinal()][receptor.ordinal()]
        if (comp == Componente.PFC) return PLASMA[doador.ordinal()][receptor.ordinal()] == 1;
        return HEMACIAS[doador.ordinal()][receptor.ordinal()] == 1;
    }
}
