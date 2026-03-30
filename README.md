# Banco J — Sistema Bancário Full Stack

## Identificação

| Campo     | Valor                  |
| --------- | ---------------------- |
| Nome      | _(seu nome aqui)_      |
| Matrícula | _(sua matrícula aqui)_ |
| Turma     | _(sua turma aqui)_     |

---

## Visão Geral

Sistema bancário full stack com:

- **Backend** Spring Boot 3.3 + Spring Security (BCrypt)
- **Banco de dados** PostgreSQL com migrações Flyway
- **Frontend** HTML/CSS/JS puro consumindo a API REST
- **Docker** para orquestração do banco de dados

---

## Arquitetura do Sistema

```
┌─────────────────────────────────────────────────────┐
│                   FRONTEND (HTML/JS)                │
│              Fetch API                              │
└────────────────────┬────────────────────────────────┘
                     │ HTTP/REST (JSON)
┌────────────────────▼────────────────────────────────┐
│              BACKEND (Spring Boot 4.0)              │
│                                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │ Controller  │→ │   Service    │→ │ Repository │  │
│  │  (REST API) │  │ (Regras de   │  │ (JPA/Data) │  │
│  └─────────────┘  │  Negócio)    │  └─────┬──────┘  │
│                   └──────────────┘        │         │
│  ┌──────────────────────────────┐         │         │
│  │   BCryptPasswordEncoder      │         │         │
│  │   (validação no login)       │         │         │
│  └──────────────────────────────┘         │         │
└───────────────────────────────────────────┼─────────┘
                                            │ JPA/Hibernate
┌───────────────────────────────────────────▼──────────┐
│              PostgreSQL 16                           │
│         Gerenciado pelo Docker Compose               │
│         Migrações via Flyway                         │
└──────────────────────────────────────────────────────┘
```

### Divisão em camadas

| Camada         | Pacote        | Responsabilidade                                                                     |
| -------------- | ------------- | ------------------------------------------------------------------------------------ |
| **Controller** | `controller/` | Receber requisições HTTP, validar DTOs, delegar ao Service                           |
| **Service**    | `service/`    | Centralizar todas as regras de negócio (sacar, depositar, tributos…)                 |
| **Repository** | `repository/` | Abstração do banco via Spring Data JPA                                               |
| **Model**      | `model/`      | Entidades JPA — herança SINGLE_TABLE: `Conta`, `ContaCorrente`, `ContaPoupanca`      |
| **DTO**        | `dto/`        | Objetos de transferência (request com validação `@Valid`, response limpo)            |
| **Config**     | `config/`     | SecurityConfig: desabilita CSRF, permite todos os endpoints, configura CORS e BCrypt |
| **Exception**  | `exception/`  | Exceções de negócio + `GlobalExceptionHandler` (`@RestControllerAdvice`)             |

---

## Autenticação

A autenticação é feita pelo endpoint `POST /auth/login`, que valida nome e senha contra o banco usando **BCrypt**. Não há token: as rotas são todas públicas no nível do servidor. O controle de sessão fica no frontend — ao fazer login com sucesso, o overlay é removido e o nome/cargo do funcionário são exibidos; ao clicar em "Sair", o overlay é reexibido, bloqueando o acesso visual à interface.

---

## Escolha do PostgreSQL em vez do SQLite

O enunciado original sugeria SQLite, porém PostgreSQL foi adotado pelos seguintes motivos:

1. **Concorrência real** — PostgreSQL suporta múltiplas conexões simultâneas com isolamento transacional (MVCC). SQLite usa lock por arquivo, inadequado para uma API REST.
2. **Suporte oficial ao Flyway** — o módulo `flyway-database-postgresql` é mantido oficialmente.
3. **Compatibilidade com Spring Data JPA** — o dialeto `PostgreSQLDialect` é nativo ao Hibernate; o dialeto SQLite exige dependências externas.
4. **Docker** — a imagem `postgres:16-alpine` é leve (~80 MB) e elimina qualquer instalação local.

---

## Como Executar

### Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose

### 1. Subir o banco de dados

```bash
cd backend
docker compose up -d
```

Aguarde o health check:

```bash
docker compose ps   # Status deve ser "healthy"
```

### 2. Executar o backend

```bash
mvn spring-boot:run
```

O servidor sobe em `http://localhost:8080`.
O Flyway aplicará automaticamente as migrações `V1` (tabelas) e `V2` (gerente padrão).

### 3. Abrir o frontend

Abra o arquivo `frontend/index.html` diretamente no navegador.

> **Credenciais padrão:** Nome `Davi França` · Senha `13589!`

---

## Endpoints da API

### Autenticação

| Método | Endpoint      | Descrição                                                  |
| ------ | ------------- | ---------------------------------------------------------- |
| `POST` | `/auth/login` | Valida credenciais (BCrypt) e retorna dados do funcionário |

**Exemplo:**

```json
POST /auth/login
{
  "nome": "Davi França",
  "senha": "13589!"
}
```

**Resposta (sucesso):**

```json
{
  "sucesso": true,
  "mensagem": "Login realizado com sucesso.",
  "dados": {
    "nome": "Davi França",
    "cargo": "Gerente",
    "agencia": "001"
  }
}
```

**Resposta (credenciais inválidas — HTTP 400):**

```json
{
  "sucesso": false,
  "mensagem": "Credenciais inválidas.",
  "dados": null
}
```

---

### Contas

| Método   | Endpoint                      | Descrição                                |
| -------- | ----------------------------- | ---------------------------------------- |
| `POST`   | `/contas`                     | Criar nova conta                         |
| `GET`    | `/contas`                     | Listar todas as contas                   |
| `GET`    | `/contas/{numero}`            | Buscar conta por número                  |
| `DELETE` | `/contas/{numero}`            | Excluir conta                            |
| `POST`   | `/contas/depositar`           | Realizar depósito                        |
| `POST`   | `/contas/sacar`               | Realizar saque                           |
| `POST`   | `/contas/transferir`          | Transferir entre contas                  |
| `POST`   | `/contas/{numero}/rendimento` | Aplicar rendimento ou taxa de manutenção |
| `GET`    | `/contas/{numero}/historico`  | Histórico de operações                   |

**Criar conta:**

```json
POST /contas
{
  "titular": "João Silva",
  "saldoInicial": 1500.00,
  "tipo": "CORRENTE"
}
```

**Depósito / Saque:**

```json
POST /contas/depositar
{
  "numeroConta": 10001,
  "valor": 500.00
}
```

**Transferência:**

```json
POST /contas/transferir
{
  "contaOrigem": 10001,
  "contaDestino": 10002,
  "valor": 200.00
}
```

---

### Tributos

| Método | Endpoint           | Descrição                                |
| ------ | ------------------ | ---------------------------------------- |
| `GET`  | `/contas/tributos` | Calcular IOF (0,5%) das contas correntes |

---

### Endpoints Exclusivos e Funcionalidades Adicionais

| Método | Endpoint                            | Descrição                                                          |
| ------ | ----------------------------------- | ------------------------------------------------------------------ |
| `GET`  | `/contas/top-saldos`                | **Endpoint exclusivo** — ranking de contas por saldo (decrescente) |
| `GET`  | `/contas/filtrar?saldoMinimo=500.0` | **Funcionalidade adicional** — filtrar contas por saldo mínimo     |

---

## Modelo de Dados

```sql
-- Herança SINGLE_TABLE: todas as contas em uma tabela.
-- A coluna "tipo" é o discriminador (CORRENTE | POUPANCA).
contas (id, numero, titular, saldo, tipo)

-- Log de operações vinculado a cada conta.
historico_operacoes (id, conta_id, descricao, data_hora)

-- Funcionários autenticáveis (senha BCrypt).
funcionarios (id, nome, senha, cargo, agencia)
```

---

## Estrutura de Pastas

```
└── bancoj-v3/
    └── backend/
        └── src/
            └── main/
                └── java/
                    └── com/
                        └── bancoj/
                            └── backend/
                                └── api/
                                    └── controller/
                                        ├── AuthController.java
                                        ├── ContaController.java
                                └── application/
                                    └── dto/
                                        ├── ApiResponse.java
                                        ├── ContaResponse.java
                                        ├── CriarContaRequest.java
                                        ├── DepositoSaqueRequest.java
                                        ├── HistoricoResponse.java
                                        ├── LoginRequest.java
                                        ├── LoginResponse.java
                                        ├── TransferenciaRequest.java
                                        ├── TributoResponse.java
                                    └── service/
                                        ├── AuthService.java
                                        ├── ContaService.java
                                └── config/
                                    ├── SecurityConfig.java
                                └── domain/
                                    └── interfaces/
                                    └── model/
                                        ├── Conta.java
                                        ├── ContaCorrente.java
                                        ├── ContaPoupanca.java
                                        ├── Funcionario.java
                                        ├── HistoricoOperacao.java
                                └── exception/
                                    ├── ContaNaoEncontradaException.java
                                    ├── GlobalExceptionHandler.java
                                    ├── OperacaoInvalidaException.java
                                    ├── SaldoInsuficienteException.java
                                └── infrastructure/
                                    └── repository/
                                        ├── ContaRepository.java
                                        ├── FuncionarioRepository.java
                                        ├── HistoricoOperacaoRepository.java
                                    └── security/
                                ├── BackendApplication.java
                └── resources/
                    └── db/
                        └── migration/
                            ├── V1__create_tables.sql
                            ├── V2__insert_gerente.sql
                    └── static/
                    └── templates/
                    ├── application.properties
            └── test/
                └── java/
                    └── com/
                        └── bancoj/
                            └── backend/
                                ├── BackendApplicationTests.java
    └── frontend/
        ├── index.html
        ├── style.css
```

---

## Funcionalidades implementadas

- ✅ Criar conta (Corrente ou Poupança)
- ✅ Listar contas
- ✅ Depositar
- ✅ Sacar
- ✅ Transferir
- ✅ Aplicar rendimento / taxa de manutenção
- ✅ Histórico de transações por conta
- ✅ Calcular tributos (contas correntes)
- ✅ Autenticar funcionário (BCrypt)
- ✅ Docker Compose com PostgreSQL 16
- ✅ `GET /contas/top-saldos` — endpoint exclusivo
- ✅ `GET /contas/filtrar?saldoMinimo=X` — funcionalidade adicional
- ✅ Handler global de erros com `ApiResponse<T>` padronizado
