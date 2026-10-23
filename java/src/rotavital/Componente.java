package rotavital;

/**
 * Hemocomponentes: CH = concentrado de hemácias, PFC = plasma fresco
 * congelado, PLQ = plaquetas.
 *
 * C++:  enum Componente { CH, PFC, PLQ };  const char* NOME_COMP[3]
 * Java: enum; name() já devolve "CH", "PFC", "PLQ".
 */
public enum Componente {
    CH, PFC, PLQ
}
