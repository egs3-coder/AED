package rotavital;

/**
 * Ordenações das aulas 07 e 08, por ordem FEFO, com contador de comparações.
 *
 * C++:  funções soltas + global long long g_comparacoes; std::swap(a, b)
 * Java: classe com o contador como atributo (cada instância conta o seu);
 *       swap() próprio porque Java não tem passagem por referência de
 *       elementos: trocamos posições do array.
 *       Não usamos Arrays.sort/Collections.sort de propósito.
 */
public class Ordenacao {
    private long comparacoes = 0;

    public long getComparacoes() { return comparacoes; }
    public void zerar() { comparacoes = 0; }

    private boolean menor(Bolsa a, Bolsa b) {
        comparacoes++;
        return a.venceAntesDe(b);
    }

    private static void swap(Bolsa[] arr, int i, int j) {
        Bolsa t = arr[i]; arr[i] = arr[j]; arr[j] = t;
    }

    public void bubbleSort(Bolsa[] arr) {                         // O(n^2); O(n) se já ordenado
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean trocou = false;
            for (int j = 0; j < n - i - 1; j++)
                if (menor(arr[j + 1], arr[j])) { swap(arr, j, j + 1); trocou = true; }
            if (!trocou) break;
        }
    }

    public void selectionSort(Bolsa[] arr) {                      // O(n^2) sempre
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < n; j++)
                if (menor(arr[j], arr[minIdx])) minIdx = j;
            swap(arr, minIdx, i);
        }
    }

    public void insertionSort(Bolsa[] arr) {                      // O(n^2); O(n) quase ordenado
        for (int i = 1; i < arr.length; i++) {
            Bolsa chave = arr[i];
            int j = i - 1;
            while (j >= 0 && menor(chave, arr[j])) { arr[j + 1] = arr[j]; j--; }
            arr[j + 1] = chave;
        }
    }

    public void shellSort(Bolsa[] arr) {                          // gaps n/2: pior O(n^2)
        int n = arr.length;
        for (int gap = n / 2; gap > 0; gap /= 2)
            for (int i = gap; i < n; i++) {
                Bolsa temp = arr[i];
                int j;
                for (j = i; j >= gap && menor(temp, arr[j - gap]); j -= gap)
                    arr[j] = arr[j - gap];
                arr[j] = temp;
            }
    }

    public void quickSort(Bolsa[] arr) { quickSort(arr, 0, arr.length - 1); }

    private void quickSort(Bolsa[] arr, int low, int high) {      // médio O(n log n), pior O(n^2)
        if (low < high) {
            int pi = particionar(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }

    private int particionar(Bolsa[] arr, int low, int high) {    // Lomuto, pivô = último
        Bolsa pivo = arr[high];
        int i = low - 1;
        for (int j = low; j <= high - 1; j++)
            if (menor(arr[j], pivo)) { i++; swap(arr, i, j); }
        swap(arr, i + 1, high);
        return i + 1;
    }

    public void mergeSort(Bolsa[] arr) { mergeSort(arr, 0, arr.length - 1); }

    private void mergeSort(Bolsa[] arr, int esq, int dir) {      // O(n log n) sempre
        if (esq >= dir) return;
        int meio = esq + (dir - esq) / 2;
        mergeSort(arr, esq, meio);
        mergeSort(arr, meio + 1, dir);
        intercalar(arr, esq, meio, dir);
    }

    private void intercalar(Bolsa[] arr, int esq, int meio, int dir) {
        Bolsa[] aux = new Bolsa[dir - esq + 1];   // C++: new Bolsa[n] ... delete[] aux;
        int i = esq, j = meio + 1, k = 0;
        while (i <= meio && j <= dir) {
            if (menor(arr[j], arr[i])) aux[k++] = arr[j++];
            else aux[k++] = arr[i++];             // empate: esquerda -> estável
        }
        while (i <= meio) aux[k++] = arr[i++];
        while (j <= dir) aux[k++] = arr[j++];
        System.arraycopy(aux, 0, arr, esq, aux.length);   // C++: for (k...) arr[esq+k] = aux[k];
    }

    public static boolean estaOrdenado(Bolsa[] arr) {
        for (int i = 1; i < arr.length; i++)
            if (arr[i].venceAntesDe(arr[i - 1])) return false;
        return true;
    }
}
