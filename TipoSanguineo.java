package rotavital;

/**
 * Tipos sanguíneos ABO/Rh.
 *
 * TRADUÇÃO C++ -> Java:
 *   C++:  enum Tipo { O_NEG, O_POS, ... };  const char* NOME_TIPO[8] = {"O-", ...};
 *   Java: enum com o rótulo embutido. ordinal() faz o papel do valor inteiro
 *         do enum C++ e é usado como índice na matriz de compatibilidade.
 */
public enum TipoSanguineo {
    O_NEG("O-"), O_POS("O+"), A_NEG("A-"), A_POS("A+"),
    B_NEG("B-"), B_POS("B+"), AB_NEG("AB-"), AB_POS("AB+");

    private final String rotulo;

    TipoSanguineo(String rotulo) { this.rotulo = rotulo; }

    @Override
    public String toString() { return rotulo; }
}
