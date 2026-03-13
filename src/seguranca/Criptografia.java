package seguranca;

public class Criptografia {
    private static final String CHAVE = "AEDS3";

    /**
     * Aplica a cifra XOR em um texto utilizando uma chave.
     * Como o XOR é simétrico, aplicar o mesmo método no texto cifrado com a mesma chave
     * resultará no texto original.
     * 
     * @param texto Texto original ou cifrado
     * @param chave Chave para a criptografia
     * @return Texto processado
     */
    public static String xor(String texto, String chave) {
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            // Aplica XOR entre o caractere do texto e o caractere da chave (ciclicamente)
            char c = (char) (texto.charAt(i) ^ chave.charAt(i % chave.length()));
            resultado.append(c);
        }
        return resultado.toString();
    }

    /**
     * Criptografa um texto usando XOR com a chave fixa interna "AEDS3".
     * 
     * @param texto Texto a ser criptografado
     * @return Texto criptografado
     */
    public static String criptografar(String texto) {
        return xor(texto, CHAVE);
    }
}
