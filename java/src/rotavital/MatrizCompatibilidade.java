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

    // Plasma (e crioprecipitado): lógica invertida (leva anticorpos). AB = doador universal; Rh ignorado.
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
        if (comp == Componente.PFC || comp == Componente.CRIO)
            return PLASMA[doador.ordinal()][receptor.ordinal()] == 1;
        return HEMACIAS[doador.ordinal()][receptor.ordinal()] == 1;
    }

    /**
     * Doadores aceitos = COLUNA do receptor na matriz (8 linhas). O(8) = O(1).
     * C++:  int doadoresCompativeis(Componente comp, Tipo receptor, Tipo saida[])
     *       (preenche o vetor de saída e devolve quantos)
     * Java: devolve um array já do tamanho certo (não há parâmetro de saída).
     */
    public static TipoSanguineo[] doadoresCompativeis(Componente comp, TipoSanguineo receptor) {
        TipoSanguineo[] tipos = TipoSanguineo.values();
        TipoSanguineo[] tmp = new TipoSanguineo[tipos.length];
        int n = 0;
        for (int doador = 0; doador < tipos.length; doador++)   // C++: for (int doador = 0; doador < N_TIPOS; doador++)
            if (compativel(comp, tipos[doador], receptor)) tmp[n++] = tipos[doador];
        TipoSanguineo[] saida = new TipoSanguineo[n];
        for (int i = 0; i < n; i++) saida[i] = tmp[i];
        return saida;
    }
}
