package rotavital;

/**
 * Datas sintéticas: "dia" = dia do ano (1 = 01/01/2026).
 *
 * C++: const char* data(int dia) com 4 buffers estáticos rotativos, porque C
 *      não devolve strings novas com segurança.
 * Java: basta devolver uma String nova (o coletor de lixo cuida da memória).
 */
public final class Datas {
    private static final int[] DIAS_MES = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private Datas() { }

    public static String formatar(int dia) {
        int ano = 2026;
        while (dia > 365) { dia -= 365; ano++; }   // simplificação didática
        int mes = 0;
        while (dia > DIAS_MES[mes]) { dia -= DIAS_MES[mes]; mes++; }
        return String.format("%02d/%02d/%d", dia, mes + 1, ano);
    }
}
