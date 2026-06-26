# Sistema de Gerenciamento de Clinica Medica — AED III

Trabalho Pratico de AED III (Algoritmos e Estruturas de Dados III), PUC Minas, sob orientacao do Prof. Walisson Ferreira de Carvalho.

Aplicativo de gerenciamento de dados em memoria secundaria, com indices proprios (Hash Extensivel e Arvore B+), relacionamentos 1:N e N:N, compactacao (Huffman e LZW), casamento de padroes (KMP e Boyer-Moore) e criptografia XOR — tudo implementado do zero, sem uso de SGBDs ou bibliotecas externas de persistencia.

## Estrutura do projeto

```
src/clinica/
  controller/   -> ClinicaHttpServer.java (API REST)
  dao/          -> DAOs de cada entidade (persistencia em arquivos binarios)
  index/        -> HashExtensivel, ArvoreBMais, OrdenacaoExterna
  model/        -> Entidades (Paciente, Medico, Consulta, Especialidade, Usuario, ...)
  service/      -> ClinicaService (regras de negocio), CompactacaoService
  util/         -> CasamentoPadroes (KMP/BM), CriptoXOR, Huffman, LZW, JsonParser
web/
  index.html, css/style.css, js/app.js  -> frontend (SPA simples, sem frameworks)
data/   -> arquivos binarios gerados em runtime (.dat, .hdir, .hbkt, .btree)
```

## Como compilar e executar

Pre-requisito: JDK 17 ou superior.

```bash
# Compilar
find src -name "*.java" | xargs javac -d bin

# Executar
java -cp bin -DdataDir=data -DwebDir=web clinica.Main
```

O sistema fica disponivel em `http://localhost:8080`.

Login padrao: `admin` / `admin123`. Tambem e possivel criar uma conta nova diretamente na tela de login, pelo link "Criar conta".

## Fases implementadas

| Fase | Conteudo |
|---|---|
| 1 | Modelagem, CRUD de todas as tabelas, persistencia binaria com cabecalho |
| 2 | Hash Extensivel, Arvore B+, relacionamento 1:N, ordenacao externa |
| 3 | Tabela N:N Medico-Especialidade com chave composta, indexacao dupla, listagem ordenada via B+ |
| 4 | Compactacao Huffman e LZW (backup completo do diretorio de dados), KMP e Boyer-Moore (pesquisa textual) |
| 5 | Interface dedicada de pesquisa por padrao (KMP / Boyer-Moore) e criptografia XOR documentadas |

## Documentacao

A documentacao tecnica de cada fase, com o detalhamento dos algoritmos, trechos de codigo-fonte (com caminho do arquivo indicado) e evidencias reais de execucao, esta disponivel em:

- [`docs/Documentacao_Fase4_AED3.docx`](docs/Documentacao_Fase4_AED3.docx) — Compactacao (Huffman/LZW) e casamento de padroes
- [`docs/Documentacao_Fase5_AED3.docx`](docs/Documentacao_Fase5_AED3.docx) — KMP, Boyer-Moore (Bad Character + Good Suffix) e criptografia XOR

## Principais endpoints da API

| Metodo | Endpoint | Descricao |
|---|---|---|
| POST | `/api/login` | Autenticacao (retorna token) |
| POST | `/api/registrar` | Auto-cadastro de novo usuario (rota publica) |
| GET/POST/PUT/DELETE | `/api/pacientes`, `/api/medicos`, `/api/especialidades`, `/api/consultas` | CRUD das entidades |
| GET/POST/DELETE | `/api/vinculos` | Relacionamento N:N Medico <-> Especialidade |
| GET | `/api/ordenar` | Ordenacao externa por intercalacao |
| GET | `/api/listar-ordenado` | Listagem ordenada via travessia da Arvore B+ |
| POST/GET | `/api/compactar` | Gera (`POST`) ou restaura (`GET`) backup compactado (Huffman/LZW) |
| GET | `/api/pesquisar` | Casamento de padroes (KMP/BM) sobre o campo nome de Pacientes/Medicos |

## Autor

Davi Lucas — PUC Minas, AED III.
