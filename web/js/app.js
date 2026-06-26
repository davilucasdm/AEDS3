// ── API Client ──────────────────────────────────────────────────────────────
const API = {
  base: '',
  token: localStorage.getItem('token') || '',

  headers() {
    return { 'Content-Type': 'application/json', 'Authorization': `Bearer ${this.token}` };
  },

  async get(path) {
    const r = await fetch(this.base + path, { headers: this.headers() });
    return r.json();
  },
  async post(path, body) {
    const r = await fetch(this.base + path, { method: 'POST', headers: this.headers(), body: JSON.stringify(body) });
    return r.json();
  },
  async put(path, body) {
    const r = await fetch(this.base + path, { method: 'PUT', headers: this.headers(), body: JSON.stringify(body) });
    return r.json();
  },
  async del(path, body) {
    const opts = { method: 'DELETE', headers: this.headers() };
    if (body) opts.body = JSON.stringify(body);
    const r = await fetch(this.base + path, opts);
    return r.json();
  }
};

// ── Estado global ────────────────────────────────────────────────────────────
const State = {
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  page: 'dashboard',
  counts: { pacientes: 0, medicos: 0, consultas: 0, especialidades: 0 }
};

// ── Toast ────────────────────────────────────────────────────────────────────
function toast(msg, type = 'success') {
  const area = document.getElementById('toast-area');
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.innerHTML = `<span>${type === 'success' ? '✓' : '✕'}</span> ${msg}`;
  area.appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

// ── Navegação entre telas de Login / Registro ────────────────────────────────
function mostrarTelaRegistro() {
  document.getElementById('login-screen').style.display = 'none';
  document.getElementById('registro-screen').style.display = 'flex';
  document.getElementById('registro-error').style.display = 'none';
}
function mostrarTelaLogin() {
  document.getElementById('registro-screen').style.display = 'none';
  document.getElementById('login-screen').style.display = 'flex';
  document.getElementById('login-error').style.display = 'none';
}

// ── Login ────────────────────────────────────────────────────────────────────
async function doLogin() {
  const login = document.getElementById('inp-login').value.trim();
  const senha = document.getElementById('inp-senha').value;
  const errEl = document.getElementById('login-error');
  errEl.style.display = 'none';

  try {
    const res = await API.post('/api/login', { login, senha });
    if (res.ok) {
      entrarNoApp(res);
    } else {
      errEl.textContent = res.erro || res.msg || 'Credenciais inválidas';
      errEl.style.display = 'block';
    }
  } catch (e) {
    errEl.textContent = 'Erro de conexão com o servidor';
    errEl.style.display = 'block';
  }
}

// ── Registro ──────────────────────────────────────────────────────────────────
async function doRegistrar() {
  const login = document.getElementById('reg-login').value.trim();
  const senha = document.getElementById('reg-senha').value;
  const role  = document.getElementById('reg-role').value;
  const errEl = document.getElementById('registro-error');
  errEl.style.display = 'none';

  if (login.length < 3) {
    errEl.textContent = 'Login deve ter ao menos 3 caracteres';
    errEl.style.display = 'block';
    return;
  }
  if (senha.length < 4) {
    errEl.textContent = 'Senha deve ter ao menos 4 caracteres';
    errEl.style.display = 'block';
    return;
  }

  try {
    const res = await API.post('/api/registrar', { login, senha, role });
    if (res.ok) {
      toast('Conta criada com sucesso!');
      entrarNoApp(res);
    } else {
      errEl.textContent = res.erro || res.msg || 'Não foi possível criar a conta';
      errEl.style.display = 'block';
    }
  } catch (e) {
    errEl.textContent = 'Erro de conexão com o servidor';
    errEl.style.display = 'block';
  }
}

// ── Helper comum: entra no app após login ou registro ────────────────────────
function entrarNoApp(res) {
  API.token = res.token;
  localStorage.setItem('token', res.token);
  localStorage.setItem('user', JSON.stringify({ login: res.login, role: res.role }));
  State.user = { login: res.login, role: res.role };
  document.getElementById('login-screen').style.display = 'none';
  document.getElementById('registro-screen').style.display = 'none';
  document.getElementById('app').style.display = 'flex';
  initApp();
}

function doLogout() {
  API.token = '';
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  location.reload();
}

// ── App ──────────────────────────────────────────────────────────────────────
async function initApp() {
  document.getElementById('user-name').textContent = State.user?.login || '—';
  document.getElementById('user-avatar').textContent = (State.user?.login?.[0] || 'U').toUpperCase();

  // Carrega contagens para stats
  try {
    const [pac, med, con, esp] = await Promise.all([
      API.get('/api/pacientes'), API.get('/api/medicos'),
      API.get('/api/consultas'), API.get('/api/especialidades')
    ]);
    State.counts = {
      pacientes: Array.isArray(pac) ? pac.length : 0,
      medicos:   Array.isArray(med) ? med.length : 0,
      consultas: Array.isArray(con) ? con.length : 0,
      especialidades: Array.isArray(esp) ? esp.length : 0
    };
    updateBadges();
  } catch(e) {}

  navigate('dashboard');
}

function updateBadges() {
  ['pacientes','medicos','consultas','especialidades'].forEach(k => {
    const el = document.querySelector(`[data-badge="${k}"]`);
    if (el) el.textContent = State.counts[k];
  });
}

function navigate(page) {
  State.page = page;
  document.querySelectorAll('.nav-item').forEach(el => el.classList.toggle('active', el.dataset.page === page));
  document.getElementById('topbar-title').textContent = pageTitles[page] || page;
  const content = document.getElementById('content');
  content.innerHTML = '<div style="color:var(--text3);padding:40px;text-align:center">Carregando...</div>';
  pages[page]?.();
}

const pageTitles = {
  dashboard: 'Dashboard',
  pacientes: 'Pacientes',
  medicos: 'Médicos',
  especialidades: 'Especialidades',
  consultas: 'Consultas',
  pesquisar: 'Pesquisar por padrão (KMP / BM)',
  backup: 'Backup & Compactação',
  usuarios: 'Usuários'
};

// ── Dashboard ────────────────────────────────────────────────────────────────
const pages = {
  async dashboard() {
    const [pac, med, con, esp] = await Promise.all([
      API.get('/api/pacientes'), API.get('/api/medicos'),
      API.get('/api/consultas'), API.get('/api/especialidades')
    ]);
    const pacs = Array.isArray(pac) ? pac : [];
    const meds = Array.isArray(med) ? med : [];
    const cons = Array.isArray(con) ? con : [];
    const esps = Array.isArray(esp) ? esp : [];

    const agendadas  = cons.filter(c => c.status === 'AGENDADA').length;
    const realizadas = cons.filter(c => c.status === 'REALIZADA').length;

    State.counts = { pacientes: pacs.length, medicos: meds.length, consultas: cons.length, especialidades: esps.length };
    updateBadges();

    const recentCons = cons.slice(-8).reverse();

    document.getElementById('content').innerHTML = `
      <div class="page-header">
        <div><div class="page-title">Bem-vindo, ${State.user?.login}</div>
          <div class="page-sub">Visão geral do sistema</div></div>
      </div>
      <div class="stats-row">
        <div class="stat-card blue"  onclick="navigate('pacientes')" style="cursor:pointer">
          <div class="stat-num">${pacs.length}</div><div class="stat-label">Pacientes</div></div>
        <div class="stat-card green" onclick="navigate('medicos')" style="cursor:pointer">
          <div class="stat-num">${meds.length}</div><div class="stat-label">Médicos</div></div>
        <div class="stat-card amber" onclick="navigate('consultas')" style="cursor:pointer">
          <div class="stat-num">${cons.length}</div><div class="stat-label">Consultas</div></div>
        <div class="stat-card teal"  onclick="navigate('especialidades')" style="cursor:pointer">
          <div class="stat-num">${esps.length}</div><div class="stat-label">Especialidades</div></div>
        <div class="stat-card purple">
          <div class="stat-num">${agendadas}</div><div class="stat-label">Agendadas</div></div>
        <div class="stat-card green">
          <div class="stat-num">${realizadas}</div><div class="stat-label">Realizadas</div></div>
      </div>

      <div class="page-header" style="margin-top:8px">
        <div class="page-title" style="font-size:16px">Últimas consultas</div>
        <button class="btn btn-primary btn-sm" onclick="navigate('consultas')">Ver todas</button>
      </div>
      <div class="table-wrap card">
        <table>
          <thead><tr>
            <th>#</th><th>Data</th><th>Horário</th><th>Status</th>
            <th>Paciente ID</th><th>Médico ID</th><th>Valor</th>
          </tr></thead>
          <tbody>
            ${recentCons.length === 0
              ? `<tr><td colspan="7"><div class="empty"><div class="icon">📋</div><p>Nenhuma consulta registrada</p></div></td></tr>`
              : recentCons.map(c => `
                <tr>
                  <td class="td-id">#${c.id}</td>
                  <td class="td-mono">${c.data || '—'}</td>
                  <td class="td-mono">${c.horario || '—'}</td>
                  <td>${statusBadge(c.status)}</td>
                  <td class="td-id">#${c.idPaciente}</td>
                  <td class="td-id">#${c.idMedico}</td>
                  <td class="td-mono">R$ ${Number(c.valor || 0).toFixed(2)}</td>
                </tr>`).join('')
            }
          </tbody>
        </table>
      </div>`;
  },

  // ── Pacientes ──────────────────────────────────────────────────────────────
  async pacientes() {
    const data = await API.get('/api/pacientes');
    const list = Array.isArray(data) ? data : [];
    State.counts.pacientes = list.length; updateBadges();

    document.getElementById('content').innerHTML = `
      <div class="page-header">
        <div><div class="page-title">Pacientes</div>
          <div class="page-sub">${list.length} registros</div></div>
        <button class="btn btn-primary" onclick="openModal('paciente')">+ Novo paciente</button>
      </div>
      <div class="toolbar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input id="search-pac" placeholder="Buscar por nome..." oninput="filterPacientes(this.value)">
        </div>
        <select id="search-pac-algo" class="btn btn-ghost" title="Algoritmo de casamento de padrões">
          <option value="KMP">KMP</option>
          <option value="BM">Boyer-Moore</option>
        </select>
        <button class="btn btn-ghost btn-sm" onclick="ordenarExterna('pacientes','nome','asc')" title="Ordenar A→Z">↑ Nome</button>
        <button class="btn btn-ghost btn-sm" onclick="ordenarExterna('pacientes','nome','desc')" title="Ordenar Z→A">↓ Nome</button>
      </div>
      <div id="pac-table" class="table-wrap card">${renderPacientes(list)}</div>`;
    window._pacientes = list;
  },

  // ── Médicos ────────────────────────────────────────────────────────────────
  async medicos() {
    const [meds, esps] = await Promise.all([API.get('/api/medicos'), API.get('/api/especialidades')]);
    const list = Array.isArray(meds) ? meds : [];
    window._especialidades = Array.isArray(esps) ? esps : [];
    State.counts.medicos = list.length; updateBadges();

    document.getElementById('content').innerHTML = `
      <div class="page-header">
        <div><div class="page-title">Médicos</div>
          <div class="page-sub">${list.length} registros</div></div>
        <button class="btn btn-primary" onclick="openModal('medico')">+ Novo médico</button>
      </div>
      <div class="toolbar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input id="search-med" placeholder="Buscar por nome..." oninput="filterMedicos(this.value)">
        </div>
        <select id="search-med-algo" class="btn btn-ghost" title="Algoritmo de casamento de padrões">
          <option value="KMP">KMP</option>
          <option value="BM">Boyer-Moore</option>
        </select>
        <button class="btn btn-ghost btn-sm" onclick="ordenarExterna('medicos','nome','asc')">↑ Nome</button>
        <button class="btn btn-ghost btn-sm" onclick="ordenarExterna('medicos','nome','desc')">↓ Nome</button>
      </div>
      <div id="med-table" class="table-wrap card">${renderMedicos(list)}</div>`;
    window._medicos = list;
  },

  // ── Especialidades ────────────────────────────────────────────────────────
  async especialidades() {
    const data = await API.get('/api/especialidades');
    const list = Array.isArray(data) ? data : [];
    State.counts.especialidades = list.length; updateBadges();

    document.getElementById('content').innerHTML = `
      <div class="page-header">
        <div><div class="page-title">Especialidades</div>
          <div class="page-sub">${list.length} registros</div></div>
        <button class="btn btn-primary" onclick="openModal('especialidade')">+ Nova especialidade</button>
      </div>
      <div class="table-wrap card">${renderEspecialidades(list)}</div>`;
    window._especialidades = list;
  },

  // ── Consultas ─────────────────────────────────────────────────────────────
  async consultas() {
    const [cons, pacs, meds] = await Promise.all([
      API.get('/api/consultas'), API.get('/api/pacientes'), API.get('/api/medicos')
    ]);
    const list = Array.isArray(cons) ? cons : [];
    window._pacientes    = Array.isArray(pacs) ? pacs : [];
    window._medicos      = Array.isArray(meds) ? meds : [];
    window._consultas    = list;
    State.counts.consultas = list.length; updateBadges();

    document.getElementById('content').innerHTML = `
      <div class="page-header">
        <div><div class="page-title">Consultas</div>
          <div class="page-sub">${list.length} registros</div></div>
        <button class="btn btn-primary" onclick="openModal('consulta')">+ Nova consulta</button>
      </div>
      <div class="toolbar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input id="search-con" placeholder="Filtrar por status..." oninput="filterConsultas(this.value)">
        </div>
        <select onchange="filterConsultaStatus(this.value)" class="btn btn-ghost">
          <option value="">Todos os status</option>
          <option>AGENDADA</option><option>REALIZADA</option><option>CANCELADA</option>
        </select>
        <button class="btn btn-ghost btn-sm" onclick="ordenarExterna('consultas','data','asc')">↑ Data</button>
        <button class="btn btn-ghost btn-sm" onclick="ordenarExterna('consultas','data','desc')">↓ Data</button>
        <button class="btn btn-ghost btn-sm" onclick="ordenarExterna('consultas','valor','asc')">↑ Valor</button>
        <button class="btn btn-ghost btn-sm" onclick="ordenarExterna('consultas','valor','desc')">↓ Valor</button>
      </div>
      <div id="con-table" class="table-wrap card">${renderConsultas(list)}</div>`;
  },

  // ── Pesquisar por padrão (KMP / BM) — Fase V ──────────────────────────────
  async pesquisar() {
    document.getElementById('content').innerHTML = `
      <div class="page-header">
        <div><div class="page-title">Pesquisar por padrão (KMP / BM)</div>
          <div class="page-sub">Casamento de padrões sobre o campo "nome" de Pacientes ou Médicos</div></div>
      </div>

      <div class="card" style="padding:20px;margin-bottom:18px">
        <div class="form-row cols-3">
          <div class="form-group">
            <label>Tabela / Entidade</label>
            <select id="pq-entidade">
              <option value="pacientes">Pacientes (campo: nome)</option>
              <option value="medicos">Médicos (campo: nome)</option>
            </select>
          </div>
          <div class="form-group">
            <label>Algoritmo</label>
            <select id="pq-algoritmo">
              <option value="KMP">KMP (Knuth–Morris–Pratt)</option>
              <option value="BM">Boyer–Moore</option>
            </select>
          </div>
          <div class="form-group">
            <label>Padrão (string a buscar)</label>
            <input id="pq-padrao" placeholder="ex: silva" onkeydown="if(event.key==='Enter')executarPesquisaPadrao()">
          </div>
        </div>
        <button class="btn btn-primary" onclick="executarPesquisaPadrao()">Pesquisar</button>
        <div class="form-hint" style="margin-top:10px">
          A busca ignora maiúsculas/minúsculas. KMP usa a tabela de falha (função de prefixo);
          Boyer–Moore usa as heurísticas Bad Character e Good Suffix, comparando da direita para a esquerda.
        </div>
      </div>

      <div id="pq-meta" style="margin-bottom:10px;color:var(--text3);font-size:13px"></div>
      <div id="pq-resultado" class="table-wrap card"></div>`;
  },


  async backup() {
    document.getElementById('content').innerHTML = `
      <div class="page-header">
        <div><div class="page-title">Backup &amp; Compactação</div>
          <div class="page-sub">Compacte ou restaure todos os arquivos de dados do sistema</div></div>
      </div>

      <div class="card" style="padding:20px;margin-bottom:18px">
        <div style="font-weight:600;margin-bottom:12px">Gerar backup compactado</div>
        <div style="display:flex;gap:10px;flex-wrap:wrap">
          <button class="btn btn-primary" onclick="compactar('HUFFMAN')">Compactar (Huffman)</button>
          <button class="btn btn-primary" onclick="compactar('LZW')">Compactar (LZW)</button>
        </div>
        <div id="backup-resultado" style="margin-top:16px"></div>
      </div>

      <div class="card" style="padding:20px">
        <div style="font-weight:600;margin-bottom:12px">Restaurar a partir de um backup</div>
        <div style="display:flex;gap:10px;flex-wrap:wrap;align-items:center">
          <select id="restore-arquivo" style="background:var(--bg3);border:1px solid var(--border);border-radius:7px;padding:8px 12px;color:var(--text);font-family:inherit">
            <option value="backup_huffman.hbak">backup_huffman.hbak</option>
            <option value="backup_lzw.hbak">backup_lzw.hbak</option>
          </select>
          <button class="btn btn-ghost" onclick="restaurar()">Restaurar</button>
        </div>
        <div id="restore-resultado" style="margin-top:16px"></div>
      </div>`;
  },

  // ── Usuários ──────────────────────────────────────────────────────────────
  async usuarios() {
    const data = await API.get('/api/usuarios');
    const list = Array.isArray(data) ? data : [];

    document.getElementById('content').innerHTML = `
      <div class="page-header">
        <div><div class="page-title">Usuários</div>
          <div class="page-sub">${list.length} registros</div></div>
        <button class="btn btn-primary" onclick="openModal('usuario')">+ Novo usuário</button>
      </div>
      <div class="table-wrap card">
        <table>
          <thead><tr><th>#</th><th>Login</th><th>Perfil</th><th style="text-align:right">Ações</th></tr></thead>
          <tbody>
          ${list.length === 0
            ? `<tr><td colspan="4"><div class="empty"><div class="icon">👤</div><p>Nenhum usuário</p></div></td></tr>`
            : list.map(u => `
              <tr>
                <td class="td-id">#${u.id}</td>
                <td class="td-name">${u.login}</td>
                <td>${roleBadge(u.role)}</td>
                <td class="actions">
                  <button class="btn btn-danger-ghost btn-sm btn-icon" onclick="deletarUsuario(${u.id})">🗑</button>
                </td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  }
};

// ── Render helpers ────────────────────────────────────────────────────────────
function renderPacientes(list) {
  if (!list.length) return `<div class="empty"><div class="icon">👥</div><p>Nenhum paciente cadastrado</p></div>`;
  return `<table>
    <thead><tr><th>#</th><th>Nome</th><th>CPF</th><th>Nascimento</th><th>Telefones</th><th>Email</th><th style="text-align:right">Ações</th></tr></thead>
    <tbody>${list.map(p => `
      <tr>
        <td class="td-id">#${p.id}</td>
        <td class="td-name">${p.nome}</td>
        <td class="td-mono">${p.cpf || '—'}</td>
        <td class="td-mono">${p.dataNascimento || '—'}</td>
        <td>${(p.telefones || '').split(';').filter(Boolean).map(t=>`<span class="badge badge-blue">${t.trim()}</span>`).join(' ')}</td>
        <td>${p.email || '—'}</td>
        <td class="actions">
          <button class="btn btn-ghost btn-sm btn-icon" onclick='editarPaciente(${JSON.stringify(p)})'>✏️</button>
          <button class="btn btn-danger-ghost btn-sm btn-icon" onclick="deletarPaciente(${p.id})">🗑</button>
        </td>
      </tr>`).join('')}
    </tbody></table>`;
}

function renderMedicos(list) {
  if (!list.length) return `<div class="empty"><div class="icon">👨‍⚕️</div><p>Nenhum médico cadastrado</p></div>`;
  return `<table>
    <thead><tr><th>#</th><th>Nome</th><th>CRM</th><th>Email</th><th>Telefones</th><th style="text-align:right">Ações</th></tr></thead>
    <tbody>${list.map(m => `
      <tr>
        <td class="td-id">#${m.id}</td>
        <td class="td-name">${m.nome}</td>
        <td class="td-mono">${m.crm || '—'}</td>
        <td>${m.email || '—'}</td>
        <td>${(m.telefones || '').split(';').filter(Boolean).map(t=>`<span class="badge badge-teal">${t.trim()}</span>`).join(' ')}</td>
        <td class="actions">
          <button class="btn btn-ghost btn-sm" onclick="verEspecialidades(${m.id},'${escHtml(m.nome)}')">Especialidades</button>
          <button class="btn btn-ghost btn-sm btn-icon" onclick='editarMedico(${JSON.stringify(m)})'>✏️</button>
          <button class="btn btn-danger-ghost btn-sm btn-icon" onclick="deletarMedico(${m.id})">🗑</button>
        </td>
      </tr>`).join('')}
    </tbody></table>`;
}

function renderEspecialidades(list) {
  if (!list.length) return `<div class="empty"><div class="icon">🩺</div><p>Nenhuma especialidade cadastrada</p></div>`;
  return `<table>
    <thead><tr><th>#</th><th>Nome</th><th>Descrição</th><th style="text-align:right">Ações</th></tr></thead>
    <tbody>${list.map(e => `
      <tr>
        <td class="td-id">#${e.id}</td>
        <td class="td-name">${e.nome}</td>
        <td>${e.descricao || '—'}</td>
        <td class="actions">
          <button class="btn btn-ghost btn-sm btn-icon" onclick='editarEsp(${JSON.stringify(e)})'>✏️</button>
          <button class="btn btn-danger-ghost btn-sm btn-icon" onclick="deletarEsp(${e.id})">🗑</button>
        </td>
      </tr>`).join('')}
    </tbody></table>`;
}

function renderConsultas(list) {
  if (!list.length) return `<div class="empty"><div class="icon">📋</div><p>Nenhuma consulta registrada</p></div>`;
  const pacMap = {};
  const medMap = {};
  (window._pacientes || []).forEach(p => pacMap[p.id] = p.nome);
  (window._medicos   || []).forEach(m => medMap[m.id] = m.nome);
  return `<table>
    <thead><tr><th>#</th><th>Data</th><th>Hora</th><th>Paciente</th><th>Médico</th><th>Status</th><th>Valor</th><th style="text-align:right">Ações</th></tr></thead>
    <tbody>${list.map(c => `
      <tr>
        <td class="td-id">#${c.id}</td>
        <td class="td-mono">${c.data || '—'}</td>
        <td class="td-mono">${c.horario || '—'}</td>
        <td class="td-name">${pacMap[c.idPaciente] || `#${c.idPaciente}`}</td>
        <td>${medMap[c.idMedico] || `#${c.idMedico}`}</td>
        <td>${statusBadge(c.status)}</td>
        <td class="td-mono">R$ ${Number(c.valor || 0).toFixed(2)}</td>
        <td class="actions">
          <button class="btn btn-ghost btn-sm btn-icon" onclick='editarConsulta(${JSON.stringify(c)})'>✏️</button>
          <button class="btn btn-danger-ghost btn-sm btn-icon" onclick="deletarConsulta(${c.id})">🗑</button>
        </td>
      </tr>`).join('')}
    </tbody></table>`;
}

function statusBadge(s) {
  const map = { AGENDADA: 'badge-blue', REALIZADA: 'badge-green', CANCELADA: 'badge-danger' };
  return `<span class="badge ${map[s] || 'badge-amber'}">${s || '—'}</span>`;
}
function roleBadge(r) {
  return `<span class="badge ${r === 'ADMIN' ? 'badge-purple' : 'badge-teal'}">${r}</span>`;
}
function escHtml(s) { return (s||'').replace(/'/g,"\\'"); }

// ── Filtros (pesquisa textual via KMP/Boyer-Moore — Fase IV) ─────────────────
async function filterPacientes(q) {
  const tabela = document.getElementById('pac-table');
  if (!q) { tabela.innerHTML = renderPacientes(window._pacientes || []); return; }
  const algo = document.getElementById('search-pac-algo')?.value || 'KMP';
  try {
    const res = await API.get(`/api/pesquisar?entidade=pacientes&algoritmo=${algo}&q=${encodeURIComponent(q)}`);
    tabela.innerHTML = renderPacientes(Array.isArray(res) ? res : []);
  } catch (e) {
    // fallback local caso a API falhe
    const f = (window._pacientes || []).filter(p => p.nome?.toLowerCase().includes(q.toLowerCase()));
    tabela.innerHTML = renderPacientes(f);
  }
}
async function filterMedicos(q) {
  const tabela = document.getElementById('med-table');
  if (!q) { tabela.innerHTML = renderMedicos(window._medicos || []); return; }
  const algo = document.getElementById('search-med-algo')?.value || 'KMP';
  try {
    const res = await API.get(`/api/pesquisar?entidade=medicos&algoritmo=${algo}&q=${encodeURIComponent(q)}`);
    tabela.innerHTML = renderMedicos(Array.isArray(res) ? res : []);
  } catch (e) {
    const f = (window._medicos || []).filter(m => m.nome?.toLowerCase().includes(q.toLowerCase()));
    tabela.innerHTML = renderMedicos(f);
  }
}
function filterConsultas(q) {
  const f = (window._consultas || []).filter(c => !q || c.status?.toLowerCase().includes(q.toLowerCase()));
  document.getElementById('con-table').innerHTML = renderConsultas(f);
}
function filterConsultaStatus(s) {
  const f = s ? (window._consultas || []).filter(c => c.status === s) : window._consultas;
  document.getElementById('con-table').innerHTML = renderConsultas(f || []);
}

// ── Pesquisa por padrão dedicada (KMP / BM) — Fase V ──────────────────────────
async function executarPesquisaPadrao() {
  const entidade  = document.getElementById('pq-entidade').value;
  const algoritmo = document.getElementById('pq-algoritmo').value;
  const padrao    = document.getElementById('pq-padrao').value.trim();
  const meta = document.getElementById('pq-meta');
  const area = document.getElementById('pq-resultado');

  if (!padrao) {
    meta.textContent = 'Informe um padrão para pesquisar.';
    area.innerHTML = '';
    return;
  }

  meta.textContent = `Buscando "${padrao}" com ${algoritmo === 'BM' ? 'Boyer–Moore' : 'KMP'}...`;
  area.innerHTML = '';

  try {
    const t0 = performance.now();
    const res = await API.get(`/api/pesquisar?entidade=${entidade}&algoritmo=${algoritmo}&q=${encodeURIComponent(padrao)}`);
    const t1 = performance.now();
    const lista = Array.isArray(res) ? res : [];

    meta.textContent = `${lista.length} registro(s) encontrado(s) — algoritmo ${algoritmo === 'BM' ? 'Boyer–Moore' : 'KMP'} — ${(t1 - t0).toFixed(1)} ms`;

    if (entidade === 'medicos') {
      window._medicos = window._medicos || [];
      area.innerHTML = renderMedicos(lista);
    } else {
      window._pacientes = window._pacientes || [];
      area.innerHTML = renderPacientes(lista);
    }
  } catch (e) {
    meta.textContent = '';
    area.innerHTML = `<div class="empty"><div class="icon">⚠️</div><p>Erro ao comunicar com o servidor</p></div>`;
  }
}


// ── Modal genérico ────────────────────────────────────────────────────────────
let _editId = null;

function openModal(type, data) {
  _editId = data?.id || null;
  const modal = document.getElementById('modal');
  const head  = document.getElementById('modal-head-title');
  const body  = document.getElementById('modal-body');
  const foot  = document.getElementById('modal-foot');

  const forms = {
    paciente: () => {
      head.textContent = _editId ? 'Editar paciente' : 'Novo paciente';
      body.innerHTML = `
        <div class="form-row cols-2">
          <div class="form-group" style="grid-column:1/-1">
            <label>Nome completo</label><input id="f-nome" value="${data?.nome||''}">
          </div>
          <div class="form-group"><label>CPF</label><input id="f-cpf" placeholder="000.000.000-00" value="${data?.cpf||''}"></div>
          <div class="form-group"><label>Data de nascimento</label><input id="f-dnasc" type="date" value="${data?.dataNascimento||''}"></div>
          <div class="form-group" style="grid-column:1/-1"><label>E-mail</label><input id="f-email" type="email" value="${data?.email||''}"></div>
          <div class="form-group" style="grid-column:1/-1"><label>Telefones (separados por ;)</label><input id="f-tels" placeholder="(31)99999-0001;(31)99999-0002" value="${data?.telefones||''}"></div>
          <div class="form-group" style="grid-column:1/-1"><label>Endereço</label><input id="f-end" value="${data?.endereco||''}"></div>
        </div>`;
      foot.innerHTML = `<button class="btn btn-ghost" onclick="closeModal()">Cancelar</button>
        <button class="btn btn-primary" onclick="salvarPaciente()">Salvar</button>`;
    },
    medico: () => {
      head.textContent = _editId ? 'Editar médico' : 'Novo médico';
      body.innerHTML = `
        <div class="form-row cols-2">
          <div class="form-group" style="grid-column:1/-1"><label>Nome completo</label><input id="f-nome" value="${data?.nome||''}"></div>
          <div class="form-group"><label>CRM</label><input id="f-crm" placeholder="CRM/MG 12345" value="${data?.crm||''}"></div>
          <div class="form-group"><label>E-mail</label><input id="f-email" value="${data?.email||''}"></div>
          <div class="form-group" style="grid-column:1/-1"><label>Telefones (separados por ;)</label><input id="f-tels" value="${data?.telefones||''}"></div>
        </div>`;
      foot.innerHTML = `<button class="btn btn-ghost" onclick="closeModal()">Cancelar</button>
        <button class="btn btn-primary" onclick="salvarMedico()">Salvar</button>`;
    },
    especialidade: () => {
      head.textContent = _editId ? 'Editar especialidade' : 'Nova especialidade';
      body.innerHTML = `
        <div class="form-row">
          <div class="form-group"><label>Nome</label><input id="f-nome" value="${data?.nome||''}"></div>
          <div class="form-group"><label>Descrição</label><textarea id="f-desc">${data?.descricao||''}</textarea></div>
        </div>`;
      foot.innerHTML = `<button class="btn btn-ghost" onclick="closeModal()">Cancelar</button>
        <button class="btn btn-primary" onclick="salvarEsp()">Salvar</button>`;
    },
    consulta: () => {
      const pacs = (window._pacientes || []).map(p => `<option value="${p.id}" ${data?.idPaciente==p.id?'selected':''}>${p.nome}</option>`).join('');
      const meds = (window._medicos   || []).map(m => `<option value="${m.id}" ${data?.idMedico  ==m.id?'selected':''}>${m.nome}</option>`).join('');
      head.textContent = _editId ? 'Editar consulta' : 'Nova consulta';
      body.innerHTML = `
        <div class="form-row cols-2">
          <div class="form-group"><label>Paciente</label><select id="f-pac"><option value="">—</option>${pacs}</select></div>
          <div class="form-group"><label>Médico</label><select id="f-med"><option value="">—</option>${meds}</select></div>
          <div class="form-group"><label>Data</label><input id="f-data" type="date" value="${data?.data||''}"></div>
          <div class="form-group"><label>Horário</label><input id="f-hor" type="time" value="${data?.horario||''}"></div>
          <div class="form-group"><label>Valor (R$)</label><input id="f-valor" type="number" step="0.01" value="${data?.valor||''}"></div>
          <div class="form-group"><label>Status</label>
            <select id="f-status">
              <option ${data?.status=='AGENDADA' ?'selected':''}>AGENDADA</option>
              <option ${data?.status=='REALIZADA'?'selected':''}>REALIZADA</option>
              <option ${data?.status=='CANCELADA'?'selected':''}>CANCELADA</option>
            </select></div>
          <div class="form-group" style="grid-column:1/-1"><label>Sintomas (separados por ;)</label><input id="f-sint" value="${data?.sintomas||''}"></div>
          <div class="form-group" style="grid-column:1/-1"><label>Observações</label><textarea id="f-obs">${data?.observacoes||''}</textarea></div>
        </div>`;
      foot.innerHTML = `<button class="btn btn-ghost" onclick="closeModal()">Cancelar</button>
        <button class="btn btn-primary" onclick="salvarConsulta()">Salvar</button>`;
    },
    usuario: () => {
      head.textContent = 'Novo usuário';
      body.innerHTML = `
        <div class="form-row">
          <div class="form-group"><label>Login</label><input id="f-login"></div>
          <div class="form-group"><label>Senha</label><input id="f-senha" type="password"></div>
          <div class="form-group"><label>Perfil</label>
            <select id="f-role"><option>RECEPCIONISTA</option><option>MEDICO</option><option>ADMIN</option></select></div>
        </div>`;
      foot.innerHTML = `<button class="btn btn-ghost" onclick="closeModal()">Cancelar</button>
        <button class="btn btn-primary" onclick="salvarUsuario()">Salvar</button>`;
    }
  };

  forms[type]?.();
  modal.classList.add('open');
}

function closeModal() {
  document.getElementById('modal').classList.remove('open');
  _editId = null;
}

// ── CRUD Paciente ─────────────────────────────────────────────────────────────
async function salvarPaciente() {
  const body = { nome: v('f-nome'), cpf: v('f-cpf'), dataNascimento: v('f-dnasc'), email: v('f-email'), telefones: v('f-tels'), endereco: v('f-end') };
  const res = _editId ? await API.put(`/api/pacientes/${_editId}`, body) : await API.post('/api/pacientes', body);
  if (res.ok) { toast(_editId ? 'Paciente atualizado!' : 'Paciente criado!'); closeModal(); pages.pacientes(); }
  else toast(res.msg || res.erro || 'Erro', 'error');
}
function editarPaciente(p) { openModal('paciente', p); }
async function deletarPaciente(id) {
  if (!confirm('Excluir paciente?')) return;
  const res = await API.del(`/api/pacientes/${id}`);
  if (res.ok) { toast('Paciente removido!'); pages.pacientes(); } else toast(res.msg||res.erro,'error');
}

// ── CRUD Médico ───────────────────────────────────────────────────────────────
async function salvarMedico() {
  const body = { nome: v('f-nome'), crm: v('f-crm'), email: v('f-email'), telefones: v('f-tels') };
  const res = _editId ? await API.put(`/api/medicos/${_editId}`, body) : await API.post('/api/medicos', body);
  if (res.ok) { toast(_editId ? 'Médico atualizado!' : 'Médico criado!'); closeModal(); pages.medicos(); }
  else toast(res.msg || res.erro || 'Erro', 'error');
}
function editarMedico(m) { openModal('medico', m); }
async function deletarMedico(id) {
  if (!confirm('Excluir médico?')) return;
  const res = await API.del(`/api/medicos/${id}`);
  if (res.ok) { toast('Médico removido!'); pages.medicos(); } else toast(res.msg||res.erro,'error');
}

// ── Especialidades de um médico (N:N) ─────────────────────────────────────────
async function verEspecialidades(idMedico, nomeMedico) {
  const esps = await API.get(`/api/vinculos?medico=${idMedico}`);
  const todas = window._especialidades || [];
  const modal = document.getElementById('modal');
  document.getElementById('modal-head-title').textContent = `Especialidades — ${nomeMedico}`;
  document.getElementById('modal-body').innerHTML = `
    <div style="margin-bottom:14px">
      <div style="font-size:13px;color:var(--text3);margin-bottom:10px">Especialidades vinculadas:</div>
      <div id="esp-vinc" style="display:flex;flex-wrap:wrap;gap:8px;min-height:32px">
        ${(Array.isArray(esps)?esps:[]).map(e => `
  <span class="badge badge-purple" style="font-size:12px;padding:5px 10px">${e.nome}
    <button onclick="desvincularEsp(${idMedico},${e.id},'${escHtml(nomeMedico)}')"
            style="background:none;border:none;color:inherit;cursor:pointer;margin-left:4px">✕</button>
  </span>`).join('') || '<span style="color:var(--text3);font-size:13px">Nenhuma</span>'}
      </div>
    </div>
    <div style="font-size:13px;color:var(--text3);margin-bottom:8px">Adicionar especialidade:</div>
    <div style="display:flex;gap:10px">
      <select id="sel-esp" style="flex:1;background:var(--bg3);border:1px solid var(--border);border-radius:7px;padding:8px 12px;color:var(--text);font-family:inherit">
        <option value="">—</option>
        ${todas.map(e=>`<option value="${e.id}">${e.nome}</option>`).join('')}
      </select>
      <button class="btn btn-primary" onclick="vincularEsp(${idMedico},'${escHtml(nomeMedico)}')">Vincular</button>
    </div>`;
  document.getElementById('modal-foot').innerHTML = `<button class="btn btn-ghost" onclick="closeModal()">Fechar</button>`;
  modal.classList.add('open');
}

async function vincularEsp(idMedico, nomeMedico) {
  const idEsp = parseInt(document.getElementById('sel-esp').value);
  if (!idEsp) return;
  const res = await API.post('/api/vinculos', { idMedico, idEspecialidade: idEsp });
  if (res.ok) { toast('Especialidade vinculada!'); verEspecialidades(idMedico, nomeMedico); }
  else toast(res.msg||res.erro,'error');
}
async function desvincularEsp(idMedico, idEsp, nomeMedico) {
  const res = await API.del('/api/vinculos', { idMedico, idEspecialidade: idEsp });
  if (res.ok) { toast('Desvinculado!'); verEspecialidades(idMedico, nomeMedico); }
  else toast(res.msg||res.erro,'error');
}

// ── CRUD Especialidade ────────────────────────────────────────────────────────
async function salvarEsp() {
  const body = { nome: v('f-nome'), descricao: v('f-desc') };
  const res = _editId ? await API.put(`/api/especialidades/${_editId}`, body) : await API.post('/api/especialidades', body);
  if (res.ok) { toast(_editId ? 'Especialidade atualizada!' : 'Especialidade criada!'); closeModal(); pages.especialidades(); }
  else toast(res.msg || res.erro || 'Erro', 'error');
}
function editarEsp(e) { openModal('especialidade', e); }
async function deletarEsp(id) {
  if (!confirm('Excluir especialidade?')) return;
  const res = await API.del(`/api/especialidades/${id}`);
  if (res.ok) { toast('Especialidade removida!'); pages.especialidades(); } else toast(res.msg||res.erro,'error');
}

// ── CRUD Consulta ─────────────────────────────────────────────────────────────
async function salvarConsulta() {
  const body = {
    idPaciente: parseInt(v('f-pac')), idMedico: parseInt(v('f-med')),
    data: v('f-data'), horario: v('f-hor'), valor: parseFloat(v('f-valor')||0),
    sintomas: v('f-sint'), status: v('f-status'), observacoes: v('f-obs')
  };
  const res = _editId ? await API.put(`/api/consultas/${_editId}`, body) : await API.post('/api/consultas', body);
  if (res.ok) { toast(_editId ? 'Consulta atualizada!' : 'Consulta criada!'); closeModal(); pages.consultas(); }
  else toast(res.msg || res.erro || 'Erro', 'error');
}
function editarConsulta(c) { openModal('consulta', c); }
async function deletarConsulta(id) {
  if (!confirm('Excluir consulta?')) return;
  const res = await API.del(`/api/consultas/${id}`);
  if (res.ok) { toast('Consulta removida!'); pages.consultas(); } else toast(res.msg||res.erro,'error');
}

// ── CRUD Usuário ──────────────────────────────────────────────────────────────
async function salvarUsuario() {
  const body = { login: v('f-login'), senha: v('f-senha'), role: v('f-role') };
  const res = await API.post('/api/usuarios', body);
  if (res.ok) { toast('Usuário criado!'); closeModal(); pages.usuarios(); }
  else toast(res.msg || res.erro || 'Erro', 'error');
}
async function deletarUsuario(id) {
  if (!confirm('Excluir usuário?')) return;
  const res = await API.del(`/api/usuarios/${id}`);
  if (res.ok) { toast('Usuário removido!'); pages.usuarios(); } else toast(res.msg||res.erro,'error');
}

// ── Ordenação Externa ─────────────────────────────────────────────────────────
async function ordenarExterna(entidade, campo, ordem) {
  toast(`Ordenando ${entidade} por ${campo} (${ordem === 'asc' ? 'A→Z / ↑' : 'Z→A / ↓'})...`, 'success');
  try {
    const res = await API.get(`/api/ordenar?entidade=${entidade}&campo=${campo}&ordem=${ordem}`);
    if (res.ok) {
      toast(`Ordenação concluída! Arquivo: ${res.arquivo.split('/').pop()}`, 'success');
    } else {
      toast(res.msg || res.erro || 'Erro na ordenação', 'error');
    }
  } catch (e) {
    toast('Erro ao comunicar com o servidor', 'error');
  }
}

// ── Compactação / Backup (Fase IV) ────────────────────────────────────────────
async function compactar(algoritmo) {
  const area = document.getElementById('backup-resultado');
  area.innerHTML = `<div style="color:var(--text3);font-size:13px">Compactando com ${algoritmo}...</div>`;
  try {
    const res = await API.post('/api/compactar', { algoritmo });
    if (res.algoritmo) {
      const taxaPct = (res.taxaGeral * 100).toFixed(1);
      area.innerHTML = `
        <div style="background:var(--successBg);border:1px solid var(--success);border-radius:8px;padding:14px 16px;font-size:13px">
          <div style="font-weight:600;color:var(--success);margin-bottom:6px">✓ Backup gerado: ${res.arquivoGerado}</div>
          <div>Original: ${res.totalOriginalBytes} bytes → Compactado: ${res.totalCompactadoBytes} bytes</div>
          <div>Taxa de compressão: <b>${taxaPct}%</b></div>
        </div>`;
      toast(`Compactação ${algoritmo} concluída! Taxa: ${taxaPct}%`);
    } else {
      area.innerHTML = `<div style="color:var(--danger);font-size:13px">${res.erro || res.msg || 'Erro na compactação'}</div>`;
      toast(res.erro || res.msg || 'Erro na compactação', 'error');
    }
  } catch (e) {
    area.innerHTML = `<div style="color:var(--danger);font-size:13px">Erro de conexão com o servidor</div>`;
  }
}

async function restaurar() {
  const arquivo = document.getElementById('restore-arquivo').value;
  const area = document.getElementById('restore-resultado');
  area.innerHTML = `<div style="color:var(--text3);font-size:13px">Restaurando...</div>`;
  try {
    const res = await API.get(`/api/compactar?arquivo=${encodeURIComponent(arquivo)}`);
    if (res.ok) {
      area.innerHTML = `
        <div style="background:var(--successBg);border:1px solid var(--success);border-radius:8px;padding:14px 16px;font-size:13px">
          <div style="font-weight:600;color:var(--success);margin-bottom:6px">✓ ${res.restaurados.length} arquivo(s) restaurado(s)</div>
          <div style="color:var(--text2)">${res.restaurados.join(', ')}</div>
        </div>`;
      toast('Restauração concluída!');
    } else {
      area.innerHTML = `<div style="color:var(--danger);font-size:13px">${res.erro || res.msg || 'Erro na restauração'}</div>`;
      toast(res.erro || res.msg || 'Erro na restauração', 'error');
    }
  } catch (e) {
    area.innerHTML = `<div style="color:var(--danger);font-size:13px">Erro de conexão com o servidor</div>`;
  }
}

// ── Utilitário ────────────────────────────────────────────────────────────────
function v(id) { const el = document.getElementById(id); return el ? el.value.trim() : ''; }

// ── Init ──────────────────────────────────────────────────────────────────────
window.addEventListener('DOMContentLoaded', () => {
  document.getElementById('inp-senha').addEventListener('keydown', e => { if (e.key === 'Enter') doLogin(); });
  document.getElementById('reg-senha').addEventListener('keydown', e => { if (e.key === 'Enter') doRegistrar(); });

  if (API.token && State.user) {
    document.getElementById('login-screen').style.display = 'none';
    document.getElementById('app').style.display = 'flex';
    initApp();
  }
});
