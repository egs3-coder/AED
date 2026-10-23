package rotavital;

/**
 * Bolsa de hemocomponente (dados sintéticos).
 *
 * C++:  struct Bolsa { int id; Tipo tipo; Componente comp; int validade; };
 *       Bolsa novaBolsa(...)  e  bool vemAntes(const Bolsa& a, const Bolsa& b)
 * Java: classe imutável (campos final) com construtor; vemAntes vira método
 *       de instância: a.venceAntesDe(b).
 *
 * Diferença importante: em C++ a struct é COPIADA por valor (Bolsa b = outra);
 * em Java a variável guarda uma REFERÊNCIA. Por isso a classe é imutável:
 * compartilhar a mesma bolsa entre lista, índice e vetores não gera efeito
 * colateral.
 */
public final class Bolsa {
    private final int id;
    private final TipoSanguineo tipo;
    private final Componente componente;
    private final int validade;   // dia do ano

    public Bolsa(int id, TipoSanguineo tipo, Componente componente, int validade) {
        this.id = id;
        this.tipo = tipo;
        this.componente = componente;
        this.validade = validade;
    }

    public int getId() { return id; }
    public TipoSanguineo getTipo() { return tipo; }
    public Componente getComponente() { return componente; }
    public int getValidade() { return validade; }

    /** Ordem FEFO: validade crescente; empate -> id crescente. O(1). */
    public boolean venceAntesDe(Bolsa outra) {
        if (validade != outra.validade) return validade < outra.validade;
        return id < outra.id;
    }
}
