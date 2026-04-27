package clinica.index;

import java.io.*;

/**
 * Hash Extensível persistido em dois arquivos binários:
 *   .dir  → profundidade global + endereços dos buckets
 *   .bkt  → páginas de bucket com capacidade BUCKET_CAP entradas
 *
 * Chave: int  |  Valor: long (posição do registro no arquivo de dados)
 */
public class HashExtensivel {

    private static final int BUCKET_CAP    = 4;
    // Bucket page (bytes): 4 localDepth + 4 count + 4*BUCKET_CAP keys + 8*BUCKET_CAP values
    private static final int BUCKET_BYTES  = 4 + 4 + (4 * BUCKET_CAP) + (8 * BUCKET_CAP);  // = 56

    private final String dirPath;
    private final String bktPath;

    public HashExtensivel(String basePath) throws IOException {
        this.dirPath = basePath + ".hdir";
        this.bktPath = basePath + ".hbkt";
        if (!new File(dirPath).exists()) inicializar();
    }

    // ─── Inicializa com profundidade 0 e 1 bucket vazio ────────────────────
    private void inicializar() throws IOException {
        try (DataOutputStream d = new DataOutputStream(new FileOutputStream(dirPath))) {
            d.writeInt(0);   // profundidade global
            d.writeInt(1);   // qtd buckets alocados
            d.writeInt(0);   // única entrada aponta para bucket 0
        }
        try (RandomAccessFile b = new RandomAccessFile(bktPath, "rw")) {
            escreverBucket(b, 0, 0, new int[BUCKET_CAP], new long[BUCKET_CAP], 0);
        }
    }

    // ─── Leitura do diretório ───────────────────────────────────────────────
    private int[] lerDiretorio() throws IOException {
        try (DataInputStream d = new DataInputStream(new FileInputStream(dirPath))) {
            int prof = d.readInt();
            int total = d.readInt();
            int qtdEntradas = 1 << prof;
            int[] addr = new int[qtdEntradas];
            for (int i = 0; i < qtdEntradas; i++) addr[i] = d.readInt();
            return addr;
        }
    }

    private int lerProfGlobal() throws IOException {
        try (DataInputStream d = new DataInputStream(new FileInputStream(dirPath))) {
            return d.readInt();
        }
    }

    private int lerQtdBuckets() throws IOException {
        try (DataInputStream d = new DataInputStream(new FileInputStream(dirPath))) {
            d.readInt();
            return d.readInt();
        }
    }

    private void escreverDiretorio(int profGlobal, int[] addr) throws IOException {
        try (DataOutputStream d = new DataOutputStream(new FileOutputStream(dirPath))) {
            d.writeInt(profGlobal);
            d.writeInt(addr.length);   // qtd buckets ≤ qtd entradas
            for (int a : addr) d.writeInt(a);
        }
    }

    // ─── Leitura/escrita de buckets ─────────────────────────────────────────
    private static class Bucket {
        int localDepth, count;
        int[]  keys;
        long[] vals;
        Bucket() { keys = new int[BUCKET_CAP]; vals = new long[BUCKET_CAP]; }
    }

    private Bucket lerBucket(RandomAccessFile raf, int id) throws IOException {
        raf.seek((long) id * BUCKET_BYTES);
        Bucket b = new Bucket();
        b.localDepth = raf.readInt();
        b.count      = raf.readInt();
        for (int i = 0; i < BUCKET_CAP; i++) b.keys[i] = raf.readInt();
        for (int i = 0; i < BUCKET_CAP; i++) b.vals[i] = raf.readLong();
        return b;
    }

    private void escreverBucket(RandomAccessFile raf, int id, int localDepth,
                                 int[] keys, long[] vals, int count) throws IOException {
        raf.seek((long) id * BUCKET_BYTES);
        raf.writeInt(localDepth);
        raf.writeInt(count);
        for (int i = 0; i < BUCKET_CAP; i++) raf.writeInt(i < count ? keys[i] : 0);
        for (int i = 0; i < BUCKET_CAP; i++) raf.writeLong(i < count ? vals[i] : -1L);
    }

    // ─── Hash ───────────────────────────────────────────────────────────────
    private int hash(int key, int prof) {
        return (key * 54435761) >>> (32 - prof) & ((1 << prof) - 1);
    }

    // ─── Busca ──────────────────────────────────────────────────────────────
    public long buscar(int key) throws IOException {
        int prof   = lerProfGlobal();
        int[] addr = lerDiretorio();
        int h      = hash(key, Math.max(prof, 0));
        if (h >= addr.length) h = 0;
        try (RandomAccessFile raf = new RandomAccessFile(bktPath, "r")) {
            Bucket b = lerBucket(raf, addr[h]);
            for (int i = 0; i < b.count; i++) {
                if (b.keys[i] == key) return b.vals[i];
            }
        }
        return -1L;
    }

    // ─── Inserção ───────────────────────────────────────────────────────────
    public void inserir(int key, long value) throws IOException {
        inserirInterno(key, value);
    }

    private void inserirInterno(int key, long value) throws IOException {
        int prof   = lerProfGlobal();
        int[] addr = lerDiretorio();
        int h = (prof == 0) ? 0 : hash(key, prof);
        if (h >= addr.length) h = 0;

        try (RandomAccessFile raf = new RandomAccessFile(bktPath, "rw")) {
            Bucket b = lerBucket(raf, addr[h]);

            // Atualiza se já existe
            for (int i = 0; i < b.count; i++) {
                if (b.keys[i] == key) { b.vals[i] = value; escreverBucket(raf, addr[h], b.localDepth, b.keys, b.vals, b.count); return; }
            }

            if (b.count < BUCKET_CAP) {
                b.keys[b.count] = key;
                b.vals[b.count] = value;
                b.count++;
                escreverBucket(raf, addr[h], b.localDepth, b.keys, b.vals, b.count);
            } else {
                // Split
                int novoId = lerQtdBuckets();
                escreverBucket(raf, novoId, 0, new int[BUCKET_CAP], new long[BUCKET_CAP], 0);

                int novaProf = b.localDepth + 1;
                if (novaProf > prof) {
                    // Dobra diretório
                    int novoTamanho = 1 << novaProf;
                    int[] novoAddr = new int[novoTamanho];
                    for (int i = 0; i < novoTamanho; i++) novoAddr[i] = addr[i / 2 < addr.length ? i / 2 : 0];
                    addr  = novoAddr;
                    prof  = novaProf;
                }

                // Redistribui entradas do bucket cheio + nova entrada
                int[] ks = new int[b.count + 1];
                long[] vs = new long[b.count + 1];
                System.arraycopy(b.keys, 0, ks, 0, b.count);
                System.arraycopy(b.vals, 0, vs, 0, b.count);
                ks[b.count] = key; vs[b.count] = value;

                int[] k0 = new int[BUCKET_CAP]; long[] v0 = new long[BUCKET_CAP]; int c0 = 0;
                int[] k1 = new int[BUCKET_CAP]; long[] v1 = new long[BUCKET_CAP]; int c1 = 0;

                for (int i = 0; i <= b.count; i++) {
                    int hh = hash(ks[i], novaProf);
                    int bit = hh & 1;
                    if (bit == 0 && c0 < BUCKET_CAP) { k0[c0] = ks[i]; v0[c0] = vs[i]; c0++; }
                    else if (c1 < BUCKET_CAP)         { k1[c1] = ks[i]; v1[c1] = vs[i]; c1++; }
                }

                escreverBucket(raf, addr[h], novaProf, k0, v0, c0);
                escreverBucket(raf, novoId,  novaProf, k1, v1, c1);

                // Atualiza ponteiros no diretório
                for (int i = 0; i < addr.length; i++) {
                    if (addr[i] == addr[h]) {
                        if ((i & 1) == 1) addr[i] = novoId;
                    }
                }
                // Atualiza contagem de buckets
                escreverDiretorio(prof, addr);
                // Re-grava qtdBuckets corretamente
                atualizarQtdBuckets(novoId + 1);
                return;
            }
        }
        escreverDiretorio(prof, addr);
    }

    private void atualizarQtdBuckets(int qtd) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(dirPath, "rw")) {
            raf.seek(4);
            raf.writeInt(qtd);
        }
    }

    // ─── Remoção ────────────────────────────────────────────────────────────
    public void remover(int key) throws IOException {
        int prof   = lerProfGlobal();
        int[] addr = lerDiretorio();
        int h      = hash(key, Math.max(prof, 0));
        if (h >= addr.length) h = 0;
        try (RandomAccessFile raf = new RandomAccessFile(bktPath, "rw")) {
            Bucket b = lerBucket(raf, addr[h]);
            for (int i = 0; i < b.count; i++) {
                if (b.keys[i] == key) {
                    b.keys[i] = b.keys[b.count - 1];
                    b.vals[i] = b.vals[b.count - 1];
                    b.count--;
                    escreverBucket(raf, addr[h], b.localDepth, b.keys, b.vals, b.count);
                    return;
                }
            }
        }
    }
}
