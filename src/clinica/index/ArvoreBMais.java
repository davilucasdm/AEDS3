package clinica.index;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Árvore B+ persistida em arquivo binário.
 * Grau mínimo t=2 → max 3 chaves, max 4 filhos por nó.
 *
 * Formato do arquivo:
 *   Bytes  0- 7: offset do nó raiz (long, -1 = vazio)
 *   Bytes  8-15: próximo offset disponível para novo nó (long)
 *   A partir do byte 16: nós com NODE_SIZE bytes cada
 *
 * Estrutura do nó (NODE_SIZE = 80 bytes):
 *   0     : isLeaf (byte)
 *   1-4   : numKeys (int)
 *   5-16  : keys[3] (int[3])
 *   17-40 : values/children data (8*3=24 bytes para valores + 8 next = 32, ou 8*4=32 children)
 *            Para folha: values[3] (long[3]) + next (long)
 *            Para interno: children[4] (long[4])
 *   41-79 : padding (reservado para expansão)
 */
public class ArvoreBMais {

    private static final int T          = 2;       // grau mínimo
    private static final int MAX_KEYS   = 2 * T - 1; // 3
    private static final int MAX_CHILDS = 2 * T;     // 4
    private static final int NODE_SIZE  = 80;

    private final String path;

    public ArvoreBMais(String basePath) throws IOException {
        this.path = basePath + ".btree";
        if (!new File(this.path).exists()) {
            try (RandomAccessFile raf = new RandomAccessFile(this.path, "rw")) {
                raf.writeLong(-1L); // raiz = nenhuma
                raf.writeLong(16L); // próximo offset = logo após header
            }
        }
    }

    // ─── Cabeçalho ──────────────────────────────────────────────────────────
    private long lerRaiz() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) { return raf.readLong(); }
    }
    private long lerProxOffset() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) { raf.readLong(); return raf.readLong(); }
    }
    private void gravarRaiz(long r) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) { raf.writeLong(r); }
    }
    private long alocarNo() throws IOException {
        long pos;
        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
            raf.seek(8); pos = raf.readLong(); raf.seek(8); raf.writeLong(pos + NODE_SIZE);
        }
        return pos;
    }

    // ─── Estrutura de nó (in-memory) ────────────────────────────────────────
    private static class No {
        long   pos;
        boolean isLeaf;
        int    numKeys;
        int[]  keys     = new int[MAX_KEYS];
        long[] values   = new long[MAX_KEYS];   // folha: posições no arquivo de dados
        long[] children = new long[MAX_CHILDS]; // interno: endereços de filhos
        long   next     = -1L;                  // folha: próxima folha

        No(long pos) { this.pos = pos; for (int i=0;i<MAX_CHILDS;i++) children[i]=-1L; }
    }

    private No lerNo(long pos) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            raf.seek(pos);
            No n = new No(pos);
            n.isLeaf  = raf.readByte() == 1;
            n.numKeys = raf.readInt();
            for (int i = 0; i < MAX_KEYS;   i++) n.keys[i]     = raf.readInt();
            for (int i = 0; i < MAX_KEYS;   i++) n.values[i]   = raf.readLong();
            for (int i = 0; i < MAX_CHILDS; i++) n.children[i] = raf.readLong();
            n.next = raf.readLong();
            return n;
        }
    }

    private void gravarNo(No n) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
            raf.seek(n.pos);
            raf.writeByte(n.isLeaf ? 1 : 0);
            raf.writeInt(n.numKeys);
            for (int i = 0; i < MAX_KEYS;   i++) raf.writeInt(n.keys[i]);
            for (int i = 0; i < MAX_KEYS;   i++) raf.writeLong(n.values[i]);
            for (int i = 0; i < MAX_CHILDS; i++) raf.writeLong(n.children[i]);
            raf.writeLong(n.next);
        }
    }

    // ─── Busca ──────────────────────────────────────────────────────────────
    public long buscar(int key) throws IOException {
        long raiz = lerRaiz();
        if (raiz == -1L) return -1L;
        return buscarRec(lerNo(raiz), key);
    }

    private long buscarRec(No n, int key) throws IOException {
        int i = 0;
        while (i < n.numKeys && key > n.keys[i]) i++;
        if (n.isLeaf) {
            if (i < n.numKeys && n.keys[i] == key) return n.values[i];
            return -1L;
        }
        return buscarRec(lerNo(n.children[i]), key);
    }

    // ─── Inserção ───────────────────────────────────────────────────────────
    public void inserir(int key, long value) throws IOException {
        long raiz = lerRaiz();
        if (raiz == -1L) {
            long pos = alocarNo();
            No r = new No(pos); r.isLeaf = true; r.numKeys = 1;
            r.keys[0] = key; r.values[0] = value;
            gravarNo(r); gravarRaiz(pos); return;
        }
        No r = lerNo(raiz);
        if (r.numKeys == MAX_KEYS) {
            long novaPosRaiz = alocarNo();
            No novaRaiz = new No(novaPosRaiz);
            novaRaiz.isLeaf = false; novaRaiz.numKeys = 0;
            novaRaiz.children[0] = raiz;
            gravarNo(novaRaiz); gravarRaiz(novaPosRaiz);
            splitChild(novaRaiz, 0); novaRaiz = lerNo(novaPosRaiz);
            inserirNaoCheio(novaRaiz, key, value);
        } else {
            inserirNaoCheio(r, key, value);
        }
    }

    private void inserirNaoCheio(No n, int key, long value) throws IOException {
        int i = n.numKeys - 1;
        if (n.isLeaf) {
            // Atualiza se já existe
            for (int j = 0; j < n.numKeys; j++) {
                if (n.keys[j] == key) { n.values[j] = value; gravarNo(n); return; }
            }
            while (i >= 0 && key < n.keys[i]) { n.keys[i+1]=n.keys[i]; n.values[i+1]=n.values[i]; i--; }
            n.keys[i+1] = key; n.values[i+1] = value; n.numKeys++;
            gravarNo(n);
        } else {
            while (i >= 0 && key < n.keys[i]) i--;
            i++;
            No filho = lerNo(n.children[i]);
            if (filho.numKeys == MAX_KEYS) {
                splitChild(n, i); n = lerNo(n.pos);
                if (key > n.keys[i]) i++;
            }
            inserirNaoCheio(lerNo(n.children[i]), key, value);
        }
    }

    private void splitChild(No pai, int i) throws IOException {
        No y = lerNo(pai.children[i]);
        long novaPosZ = alocarNo();
        No z = new No(novaPosZ); z.isLeaf = y.isLeaf;

        if (y.isLeaf) {
            // Divide folha: z recebe metade direita, encadeia folhas
            int metade = T;
            z.numKeys = MAX_KEYS - metade;
            for (int j = 0; j < z.numKeys; j++) {
                z.keys[j]   = y.keys[metade + j];
                z.values[j] = y.values[metade + j];
            }
            z.next = y.next; y.next = novaPosZ;
            y.numKeys = metade;
            // Chave separadora = menor chave de z
            int sepKey = z.keys[0];
            // Insere separador no pai
            for (int j = pai.numKeys; j > i; j--) {
                pai.keys[j]       = pai.keys[j-1];
                pai.children[j+1] = pai.children[j];
            }
            pai.keys[i]       = sepKey;
            pai.children[i+1] = novaPosZ;
            pai.numKeys++;
        } else {
            // Divide nó interno
            z.numKeys = T - 1;
            for (int j = 0; j < z.numKeys; j++)   z.keys[j]       = y.keys[T + j];
            for (int j = 0; j <= z.numKeys; j++)   z.children[j]   = y.children[T + j];
            y.numKeys = T - 1;
            // Sobe chave mediana ao pai
            for (int j = pai.numKeys; j > i; j--) {
                pai.keys[j]       = pai.keys[j-1];
                pai.children[j+1] = pai.children[j];
            }
            pai.keys[i]       = y.keys[T - 1];
            pai.children[i+1] = novaPosZ;
            pai.numKeys++;
        }
        gravarNo(y); gravarNo(z); gravarNo(pai);
    }

    // ─── Remoção lógica: marca valor como -1 ────────────────────────────────
    public void remover(int key) throws IOException {
        long raiz = lerRaiz();
        if (raiz == -1L) return;
        removerRec(lerNo(raiz), key);
    }
    private void removerRec(No n, int key) throws IOException {
        int i = 0;
        while (i < n.numKeys && key > n.keys[i]) i++;
        if (n.isLeaf) {
            if (i < n.numKeys && n.keys[i] == key) { n.values[i] = -1L; gravarNo(n); }
        } else {
            removerRec(lerNo(n.children[i < n.numKeys && key >= n.keys[i] ? i+1 : i]), key);
        }
    }

    // ─── Travessia em ordem (todas folhas) ──────────────────────────────────
    /** Retorna pares [key, value] em ordem crescente de chave */
    public List<long[]> listarOrdenado() throws IOException {
        List<long[]> lista = new ArrayList<>();
        long raiz = lerRaiz();
        if (raiz == -1L) return lista;
        // Desce até a folha mais à esquerda
        No n = lerNo(raiz);
        while (!n.isLeaf) n = lerNo(n.children[0]);
        // Percorre encadeamento de folhas
        while (n != null) {
            for (int i = 0; i < n.numKeys; i++) {
                if (n.values[i] != -1L)
                    lista.add(new long[]{n.keys[i], n.values[i]});
            }
            n = (n.next != -1L) ? lerNo(n.next) : null;
        }
        return lista;
    }

    /** Retorna valores cujas chaves estão em [min, max] */
    public List<Long> buscarIntervalo(int min, int max) throws IOException {
        List<Long> res = new ArrayList<>();
        for (long[] par : listarOrdenado()) {
            int k = (int) par[0];
            if (k > max) break;
            if (k >= min) res.add(par[1]);
        }
        return res;
    }
}
