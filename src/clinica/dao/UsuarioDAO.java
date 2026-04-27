package clinica.dao;

import clinica.index.HashExtensivel;
import clinica.model.Usuario;
import clinica.util.CriptoXOR;

import java.io.*;

public class UsuarioDAO extends BaseDAO<Usuario> {

    private static final int T_LOGIN = 50;
    private static final int T_SENHA = 100;  // Base64 de XOR pode ser maior
    private static final int T_ROLE  = 20;
    // total = 4+1+50+100+20 = 175

    private final HashExtensivel hashId;

    public UsuarioDAO(String dataDir) throws IOException {
        super(dataDir + "/usuarios.dat", 175);
        hashId = new HashExtensivel(dataDir + "/usuarios_hash");
    }

    @Override
    public int criar(Usuario u) throws IOException {
        // Cifra senha antes de gravar
        u.setSenha(CriptoXOR.cifrar(u.getSenha()));
        int id = super.criar(u);
        hashId.inserir(id, offsetReg(totalSlots() - 1));
        return id;
    }

    public Usuario buscarPorLogin(String login) throws IOException {
        for (Usuario u : listarTodos()) {
            if (u.getLogin() != null && u.getLogin().equals(login)) return u;
        }
        return null;
    }

    public boolean autenticar(String login, String senhaPlana) throws IOException {
        Usuario u = buscarPorLogin(login);
        if (u == null) return false;
        return CriptoXOR.verificar(senhaPlana, u.getSenha());
    }

    /** Cria usuário admin padrão se não existir nenhum usuário */
    public void criarAdminSeNecessario() throws IOException {
        if (listarTodos().isEmpty()) {
            Usuario admin = new Usuario(0, true, "admin", "admin123", "ADMIN");
            criar(admin);
        }
    }

    @Override
    protected byte[] serializar(Usuario u) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(tamanhoReg);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(u.getId());
        dos.writeBoolean(u.isAtivo());
        writeStr(dos, u.getLogin(), T_LOGIN);
        writeStr(dos, u.getSenha(), T_SENHA);
        writeStr(dos, u.getRole(),  T_ROLE);
        dos.flush();
        return baos.toByteArray();
    }

    @Override
    protected Usuario desserializar(byte[] bytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        int     id    = dis.readInt();
        boolean ativ  = dis.readBoolean();
        String login  = readStr(dis, T_LOGIN);
        String senha  = readStr(dis, T_SENHA);
        String role   = readStr(dis, T_ROLE);
        return new Usuario(id, ativ, login, senha, role);
    }

    @Override protected int     getId(Usuario u)             { return u.getId(); }
    @Override protected void    atribuirId(Usuario u, int i) { u.setId(i); }
    @Override protected boolean isAtivo(Usuario u)           { return u.isAtivo(); }
    @Override protected void    setAtivo(Usuario u, boolean a){ u.setAtivo(a); }
}
