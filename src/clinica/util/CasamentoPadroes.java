package clinica.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Casamento de padrões — KMP e Boyer-Moore.
 *
 * Ambos operam sobre Strings (texto e padrão).
 * Retornam List<Integer> com todas as posições (índices 0-based)
 * onde o padrão começa no texto.
 */
public class CasamentoPadroes {

    // ════════════════════════════════════════════════════════════════════════
    //  KMP — Knuth–Morris–Pratt
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Busca todas as ocorrências de {@code padrao} em {@code texto} usando KMP.
     *
     * Complexidade: O(n + m), onde n = |texto|, m = |padrao|.
     */
    public static List<Integer> kmp(String texto, String padrao) {
        List<Integer> ocorrencias = new ArrayList<>();
        if (texto == null || padrao == null || padrao.isEmpty()) return ocorrencias;

        int n = texto.length();
        int m = padrao.length();
        int[] falha = tabelaFalha(padrao);

        int j = 0; // índice no padrão
        for (int i = 0; i < n; i++) {
            while (j > 0 && texto.charAt(i) != padrao.charAt(j)) {
                j = falha[j - 1];
            }
            if (texto.charAt(i) == padrao.charAt(j)) j++;
            if (j == m) {
                ocorrencias.add(i - m + 1);
                j = falha[j - 1];
            }
        }
        return ocorrencias;
    }

    /**
     * Constrói a tabela de falha (função de prefixo) para KMP.
     */
    private static int[] tabelaFalha(String padrao) {
        int m = padrao.length();
        int[] f = new int[m];
        f[0] = 0;
        int k = 0;
        for (int i = 1; i < m; i++) {
            while (k > 0 && padrao.charAt(i) != padrao.charAt(k)) {
                k = f[k - 1];
            }
            if (padrao.charAt(i) == padrao.charAt(k)) k++;
            f[i] = k;
        }
        return f;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Boyer-Moore (heurísticas Bad Character + Good Suffix)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Busca todas as ocorrências de {@code padrao} em {@code texto} usando Boyer-Moore
     * com as heurísticas Bad Character e Good Suffix.
     *
     * Complexidade: O(n·m) pior caso, O(n/m) caso médio.
     */
    public static List<Integer> boyerMoore(String texto, String padrao) {
        List<Integer> ocorrencias = new ArrayList<>();
        if (texto == null || padrao == null || padrao.isEmpty()) return ocorrencias;

        int n = texto.length();
        int m = padrao.length();

        int[] badChar  = badCharTable(padrao);
        int[] goodSuff = goodSuffixTable(padrao);

        int s = 0; // deslocamento do padrão sobre o texto
        while (s <= n - m) {
            int j = m - 1;
            while (j >= 0 && padrao.charAt(j) == texto.charAt(s + j)) j--;

            if (j < 0) {
                // Ocorrência encontrada
                ocorrencias.add(s);
                s += goodSuff[0];
            } else {
                int bc = j - badChar[texto.charAt(s + j) & 0xFFFF];
                int gs = goodSuff[j + 1];
                s += Math.max(bc, gs);
                if (s <= 0) s = 1; // garante progresso mínimo
            }
        }
        return ocorrencias;
    }

    /**
     * Tabela Bad Character: último índice de cada caractere no padrão.
     * Usa janela de 65536 posições (BMP Unicode).
     */
    private static int[] badCharTable(String padrao) {
        int[] bc = new int[65536];
        java.util.Arrays.fill(bc, -1);
        for (int i = 0; i < padrao.length(); i++) {
            bc[padrao.charAt(i) & 0xFFFF] = i;
        }
        return bc;
    }

    /**
     * Tabela Good Suffix para Boyer-Moore.
     * Baseada no algoritmo completo (shifts[0..m]).
     */
    private static int[] goodSuffixTable(String padrao) {
        int m = padrao.length();
        int[] shift = new int[m + 1];
        int[] border = new int[m + 1];

        // Fase 1: sufixos de borda
        int i = m, j = m + 1;
        border[i] = j;
        while (i > 0) {
            while (j <= m && padrao.charAt(i - 1) != padrao.charAt(j - 1)) {
                if (shift[j] == 0) shift[j] = j - i;
                j = border[j];
            }
            i--; j--;
            border[i] = j;
        }

        // Fase 2: prefixos de borda
        j = border[0];
        for (i = 0; i <= m; i++) {
            if (shift[i] == 0) shift[i] = j;
            if (i == j) j = border[j];
        }

        // Garante shifts positivos
        for (i = 0; i <= m; i++) {
            if (shift[i] <= 0) shift[i] = 1;
        }
        return shift;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Utilitário: busca case-insensitive
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Aplica KMP case-insensitive (normaliza texto e padrão para minúsculas).
     */
    public static List<Integer> kmpIgnoreCase(String texto, String padrao) {
        return kmp(
            texto  == null ? null : texto.toLowerCase(),
            padrao == null ? null : padrao.toLowerCase()
        );
    }

    /**
     * Aplica Boyer-Moore case-insensitive.
     */
    public static List<Integer> bmIgnoreCase(String texto, String padrao) {
        return boyerMoore(
            texto  == null ? null : texto.toLowerCase(),
            padrao == null ? null : padrao.toLowerCase()
        );
    }
}
