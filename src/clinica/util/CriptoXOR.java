package clinica.util;

import java.util.Base64;

/**
 * Criptografia XOR simples para senhas. A senha em texto plano é XOR'd byte a
 * byte com uma chave fixa e o resultado é codificado em Base64 para
 * armazenamento seguro em texto.
 */
public class CriptoXOR {

    // Chave XOR de 16 bytes — pode ser alterada, mas deve permanecer igual
    // em todo o ciclo de vida dos dados armazenados.
    private static final byte[] CHAVE = {
        0x4A, 0x37, 0x61, 0x2F, 0x58, 0x1C, 0x7E, 0x09,
        0x3B, 0x52, 0x44, 0x6D, 0x21, 0x70, 0x5F, 0x38
    };

    /**
     * Cifra uma senha em texto plano → Base64(XOR(senha)).
     */
    public static String cifrar(String senhaPlana) {
        if (senhaPlana == null) {
            return "";
        }
        byte[] bytes = senhaPlana.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] cifrado = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            cifrado[i] = (byte) (bytes[i] ^ CHAVE[i % CHAVE.length]);
        }
        return Base64.getEncoder().encodeToString(cifrado);
    }

    /**
     * Verifica se senhaPlana, quando cifrada, corresponde ao hash armazenado.
     */
    public static boolean verificar(String senhaPlana, String hashArmazenado) {
        if (senhaPlana == null || hashArmazenado == null) {
            return false;
        }
        return cifrar(senhaPlana).equals(hashArmazenado);
    }

    /**
     * Decifra um hash armazenado de volta ao texto plano (XOR é simétrico).
     */
    public static String decifrar(String hashBase64) {
        if (hashBase64 == null || hashBase64.isEmpty()) {
            return "";
        }
        byte[] cifrado = Base64.getDecoder().decode(hashBase64);
        byte[] decifrado = new byte[cifrado.length];
        for (int i = 0; i < cifrado.length; i++) {
            decifrado[i] = (byte) (cifrado[i] ^ CHAVE[i % CHAVE.length]);
        }
        return new String(decifrado, java.nio.charset.StandardCharsets.UTF_8);
    }
}
