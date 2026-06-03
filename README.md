# Clínica Médica — AED III · PUC Minas

Sistema de gerenciamento de dados em memória secundária com índices B+ e Hash Extensível.

---

## Documentação — Fase III

> **Relacionamento N:N com Tabela Intermediária**

[Documentacao_Fase3_AED3.pdf](docs/Documentacao_Fase3_AED3.pdf)

---

## Estrutura do Projeto

```
clinica/
├── src/clinica/
│   ├── Main.java                        ← ponto de entrada
│   ├── model/                           ← entidades
│   │   ├── Usuario.java
│   │   ├── Paciente.java
│   │   ├── Medico.java
│   │   ├── Especialidade.java
│   │   ├── Consulta.java
│   │   └── MedicoEspecialidade.java     ← tabela N:N
│   ├── dao/                             ← persistência em arquivos binários
│   │   ├── BaseDAO.java                 ← cabeçalho + exclusão lógica
│   │   ├── UsuarioDAO.java
│   │   ├── PacienteDAO.java
│   │   ├── MedicoDAO.java
│   │   ├── EspecialidadeDAO.java
│   │   ├── ConsultaDAO.java
│   │   └── MedicoEspecialidadeDAO.java
│   ├── index/                           ← estruturas em memória secundária
│   │   ├── HashExtensivel.java          ← busca por igualdade (chave primária)
│   │   └── ArvoreBMais.java             ← busca ordenada e por intervalo
│   ├── service/
│   │   └── ClinicaService.java          ← regras de negócio
│   ├── controller/
│   │   └── ClinicaHttpServer.java       ← API REST + arquivos estáticos
│   └── util/
│       ├── CriptoXOR.java               ← criptografia de senha (Fase 5)
│       └── JsonParser.java              ← parser JSON manual
├── web/                                 ← frontend SPA
│   ├── index.html
│   ├── css/style.css
│   └── js/app.js
├── docs/
│   └── Documentacao_Fase3_AED3.pdf     ← documentação técnica da Fase III
├── data/                                ← arquivos binários gerados em runtime
└── bin/                                 ← bytecode compilado
```

---

## Como Executar

### Pré-requisito
- Java 17+ (usa `--enable-preview` não necessário — apenas Java 17 LTS)

### Compilar e rodar
```bash
chmod +x run.sh
./run.sh
```

Acesse **http://localhost:8080**

Login padrão: `admin` / `admin123`

---

## Arquivos Binários Gerados

| Arquivo | Conteúdo |
|---|---|
| `data/pacientes.dat` | Registros de pacientes (629 bytes/reg) |
| `data/pacientes.hdir` + `.hbkt` | Índice Hash Extensível por ID |
| `data/pacientes.btree` | Índice Árvore B+ por ID |
| `data/medicos.dat` | Registros de médicos (375 bytes/reg) |
| `data/consultas.dat` | Registros de consultas (651 bytes/reg) |
| `data/medico_especialidade.dat` | Tabela N:N (13 bytes/reg) |
| `data/usuarios.dat` | Usuários com senha XOR cifrada |

### Formato do cabeçalho de cada `.dat`
```
Bytes 0-3  : int  numRegistrosAtivos
Bytes 4-7  : int  ultimoIdGerado
Bytes 8-11 : int  tamanhoRegistro
Bytes 12+  : registros de tamanho fixo
```

---

## Fases Implementadas

| Fase | Status | O que está pronto |
|---|---|---|
| Fase 1 | Concluída | Modelagem, CRUD de todas as tabelas, persistência binária com cabeçalho |
| Fase 2 | Concluída | Hash Extensível, Árvore B+, relacionamento 1:N, ordenação externa |
| Fase 3 | Concluída | Tabela N:N com chave composta, Hash duplo, listagem ordenada via B+ |
| Fase 4 | Pendente | Huffman, LZW, Boyer-Moore, KMP |
| Fase 5 | Pendente (estrutura pronta) | Login com XOR (CriptoXOR.java já implementado) |

---

## API REST

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/api/login` | Autenticação — retorna token Bearer |
| GET/POST/PUT/DELETE | `/api/pacientes[/id]` | CRUD pacientes |
| GET/POST/PUT/DELETE | `/api/medicos[/id]` | CRUD médicos |
| GET/POST/PUT/DELETE | `/api/especialidades[/id]` | CRUD especialidades |
| GET/POST/PUT/DELETE | `/api/consultas[/id]` | CRUD consultas |
| GET/POST/DELETE | `/api/vinculos` | Relacionamento N:N Médico↔Especialidade |
| GET | `/api/listar-ordenado?entidade=medicos` | Listagem ordenada via travessia da Árvore B+ |
| GET | `/api/listar-ordenado?entidade=consultas` | Listagem ordenada via travessia da Árvore B+ |
| GET/POST/DELETE | `/api/usuarios[/id]` | Gestão de usuários |
