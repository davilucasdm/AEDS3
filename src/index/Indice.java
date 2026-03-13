package index;

import java.io.IOException;

public interface Indice {
    /**
     * Insere uma nova chave e seu endereço correspondente no arquivo de dados.
     * 
     * @param chave ID ou valor de busca
     * @param endereco Posição (byte) no arquivo RandomAccessFile
     * @throws IOException Caso ocorra erro de acesso ao arquivo
     */
    void inserir(int chave, long endereco) throws IOException;

    /**
     * Busca o endereço de uma chave no índice.
     * 
     * @param chave ID ou valor de busca
     * @return O endereço (long) se encontrado, ou -1 caso contrário
     * @throws IOException Caso ocorra erro de acesso ao arquivo
     */
    long buscar(int chave) throws IOException;

    /**
     * Remove uma chave do índice.
     * 
     * @param chave ID ou valor de busca
     * @throws IOException Caso ocorra erro de acesso ao arquivo
     */
    void excluir(int chave) throws IOException;
}
