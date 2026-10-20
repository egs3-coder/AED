package rotavital;

/**
 * Buscas das aulas 02 e 06.
 *
 * C++:  int buscaSequencialPorId(Bolsa arr[], int n, int id)
 * Java: o array já carrega o tamanho (arr.length), então o parâmetro n some.
 */
public final class Busca {
    private Busca() { }

    /** O(n). */
    public static int sequencialPorId(Bolsa[] arr, int id) {
        for (int i = 0; i < arr.length; i++)
            if (arr[i].getId() == id) return i;
        return -1;
    }

    /** Binária ITERATIVA: 1º índice com validade > limite (vetor ordenado). O(log n). */
    public static int primeiroAposIterativa(Bolsa[] arr, int limite) {
        int inicio = 0, fim = arr.length;          // intervalo semiaberto [inicio, fim)
        while (inicio < fim) {
            int meio = inicio + (fim - inicio) / 2;
            if (arr[meio].getValidade() <= limite) inicio = meio + 1;
            else fim = meio;
        }
        return inicio;
    }

    /** Binária RECURSIVA: mesmo resultado; O(log n) tempo e O(log n) de pilha. */
    public static int primeiroAposRecursiva(Bolsa[] arr, int inicio, int fim, int limite) {
        if (inicio >= fim) return inicio;           // caso base
        int meio = inicio + (fim - inicio) / 2;
        if (arr[meio].getValidade() <= limite)
            return primeiroAposRecursiva(arr, meio + 1, fim, limite);
        return primeiroAposRecursiva(arr, inicio, meio, limite);
    }
}
