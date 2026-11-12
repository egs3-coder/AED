package rotavital;

/**
 * Hemocomponentes: CH = concentrado de hemácias, PLQ = plaquetas,
 * PFC = plasma fresco congelado, CRIO = crioprecipitado.
 * Mesma ordem do enum Hemocomponente do HemoTrack (o ordinal entra na chave do hash).
 *
 * C++:  enum Componente { CH, PLQ, PFC, CRIO };  const char* NOME_COMP[4]
 * Java: enum; name() já devolve "CH", "PLQ", "PFC", "CRIO".
 */
public enum Componente {
    CH, PLQ, PFC, CRIO
}
