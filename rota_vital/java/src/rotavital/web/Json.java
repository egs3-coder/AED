package rotavital.web;

import java.util.List;

/**
 * Mini-gerador de JSON feito à mão (sem bibliotecas externas), para o
 * projeto continuar compilando só com "javac", como a versão C++ com "g++".
 */
final class Json {
    private Json() { }

    static String str(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /** Monta um objeto a partir de pares chave, valor-já-em-JSON. */
    static String obj(Object... pares) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < pares.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append(str(pares[i].toString())).append(':').append(valor(pares[i + 1]));
        }
        return sb.append('}').toString();
    }

    static String arr(List<String> itensJson) {
        return "[" + String.join(",", itensJson) + "]";
    }

    private static String valor(Object v) {
        if (v == null) return "null";
        if (v instanceof Bruto) return ((Bruto) v).json;
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return str(v.toString());
    }

    /** Marca um texto que já é JSON (objeto/array) para não ser escapado. */
    static Bruto bruto(String json) { return new Bruto(json); }

    static final class Bruto {
        final String json;
        Bruto(String json) { this.json = json; }
    }
}
