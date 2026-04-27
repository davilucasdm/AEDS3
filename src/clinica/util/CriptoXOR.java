package clinica.util;

import java.util.Base64;

/**
 * Criptografia XOR simples para senhas de usuários.
 * A chave é aplicada ciclicamente sobre os bytes da senha.
 */
public class CriptoXOR {

    private static final byte[] CHAVE = {0x4A, 0x3F, 0x2C, 0x7B, 0x5E, 0x11, (byte)0x8D, (byte)0x92};

    /** Cifra uma string e retorna Base64 do resultado */
    public static String cifrar(String texto) {
        if (texto == null || texto.isEmpty()) return "";
        byte[] bytes = texto.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] resultado = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            resultado[i] = (byte) (bytes[i] ^ CHAVE[i % CHAVE.length]);
        }
        return Base64.getEncoder().encodeToString(resultado);
    }

    /** Decifra um Base64 gerado por cifrar() */
    public static String decifrar(String base64) {
        if (base64 == null || base64.isEmpty()) return "";
        byte[] bytes = Base64.getDecoder().decode(base64);
        byte[] resultado = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            resultado[i] = (byte) (bytes[i] ^ CHAVE[i % CHAVE.length]);
        }
        return new String(resultado, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Verifica se a senha em texto plano confere com o hash armazenado */
    public static boolean verificar(String senhaPlana, String hashArmazenado) {
        return cifrar(senhaPlana).equals(hashArmazenado);
    }
}
