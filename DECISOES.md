# DECISOES.md — Log de Decisões Arquiteturais

## Identificação

| Campo     | Valor                  |
|-----------|------------------------|
| Nome      | *(seu nome aqui)*      |
| Matrícula | *(sua matrícula aqui)* |

---

## 1. Herança das entidades — `SINGLE_TABLE` vs `JOINED`

**Decisão:** `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)`

**Por quê:**
`ContaCorrente` e `ContaPoupanca` diferem apenas em comportamento (método `calcularRendimento` e `calcularTributo`), não em estrutura de colunas. Ambas têm os mesmos campos: `numero`, `titular`, `saldo`. Com `SINGLE_TABLE`, tudo fica em uma tabela `contas` e uma coluna `tipo` funciona como discriminador.

A alternativa `JOINED` criaria duas tabelas extras (`conta_corrente`, `conta_poupanca`) com `JOIN` a cada consulta, adicionando complexidade sem benefício real neste caso — os campos são idênticos.

**Tradeoff aceito:** Se no futuro `ContaCorrente` precisar de colunas específicas (ex.: `limite_cheque_especial`), a estratégia deverá ser migrada para `JOINED`. Por ora, `SINGLE_TABLE` é mais simples e eficiente.

---

## 2. Autenticação com JWT em vez de sessão HTTP

**Decisão:** Spring Security + JWT stateless (`SessionCreationPolicy.STATELESS`)

**Por quê:**
A aplicação tem um frontend desacoplado (HTML puro, arquivo local) que consome a API REST. Sessões HTTP dependem de cookies e estado no servidor — o que viola o princípio REST e torna difícil escalar horizontalmente. JWT é autocontido: o token carrega as informações do usuário e a API não precisa manter sessão.

**Fluxo:**
1. `POST /auth/login` valida senha com BCrypt e emite um JWT assinado com HMAC-SHA256.
2. Toda requisição subsequente inclui `Authorization: Bearer <token>`.
3. `JwtAuthenticationFilter` intercepta e valida o token antes de cada controller.

**Tradeoff aceito:** JWT não pode ser invalidado antes do vencimento sem uma blacklist. Para este sistema acadêmico, o risco é aceitável dado que a expiração é de 24h.

---

## 3. Senha do gerente armazenada com BCrypt no Flyway

**Decisão:** Migration `V2__insert_gerente.sql` insere o hash BCrypt diretamente.

**Por quê:**
Armazenar a senha em texto plano no SQL de migration é um erro grave de segurança. A alternativa mais comum seria um `DataInitializer` no Spring que usa `PasswordEncoder.encode()` em tempo de execução. Porém, isso cria um problema: ao rodar a aplicação pela segunda vez, o Spring tentaria inserir novamente (ou precisaria de lógica de "já existe").

Com BCrypt pré-calculado no Flyway (`V2`), a migration roda uma única vez (`ON CONFLICT DO NOTHING`) e a senha nunca fica exposta em texto claro em nenhum log ou código-fonte.

**Como gerar um novo hash BCrypt (strength 10):**
```python
import bcrypt
print(bcrypt.hashpw(b'minha_senha', bcrypt.gensalt(10)).decode())
```

---

## 4. Número de conta gerado via `MAX(numero) + 1` em vez de sequence SQL

**Decisão:** `ContaRepository.findMaxNumero()` retorna o maior número atual, o service soma 1.

**Por quê:**
Tentamos usar `nextval('conta_numero_seq')` via query nativa, mas isso gera um `@Query` dependente de dialeto (PostgreSQL-specific), quebrando os testes de unidade com H2. A abordagem com `MAX` é portável e funciona com qualquer banco JPA.

**Tradeoff aceito:** Em ambiente com alta concorrência, dois threads poderiam obter o mesmo `MAX` simultaneamente. Para mitigar, a coluna `numero` tem constraint `UNIQUE` — o banco rejeitaria a segunda inserção com erro de violação de unicidade. Para produção real, a sequence SQL nativa seria o caminho correto.

---

## 5. `ApiResponse<T>` como envelope padrão

**Decisão:** Todos os endpoints retornam `ApiResponse<T>` com campos `sucesso`, `mensagem` e `dados`.

**Por quê:**
Sem um envelope, o frontend precisaria inspecionar o status HTTP para saber se a operação funcionou, e lidar com formatos diferentes de erro e sucesso. Com o envelope, o frontend sempre faz:
```javascript
const r = await api('POST', '/contas', body);
// r.sucesso, r.mensagem, r.dados — sempre disponíveis
```
O `GlobalExceptionHandler` também usa `ApiResponse.erro(mensagem)`, garantindo que erros tenham o mesmo formato que respostas de sucesso.

---

## 6. Frontend em HTML/CSS/JS puro (sem framework)

**Decisão:** Opção A (Web) com HTML, CSS e Fetch API. Sem React, Vue ou Angular.

**Por quê:**
- Zero dependências externas — o arquivo `index.html` é aberto direto no navegador.
- Curva de aprendizado inexistente para avaliar o projeto.
- A API REST é o foco da atividade; o frontend é instrumental.
- Fetch API nativa é suficiente para consumir todos os endpoints.

**Tradeoff aceito:** Sem reatividade de framework, o DOM é atualizado manualmente (innerHTML). Para uma aplicação real de produção, um framework seria recomendado.

---

## 7. Histórico de operações como entidade separada

**Decisão:** `HistoricoOperacao` é uma entidade JPA com tabela própria, em vez de uma lista de Strings dentro de `Conta`.

**Por quê:**
Na versão anterior (Java puro), o histórico era um `ArrayList<String>` em memória — perdido a cada restart. Com JPA, precisamos de persistência real. Strings em uma coluna (ex.: JSON ou texto delimitado) seriam difíceis de consultar e paginariar. Uma tabela separada com `conta_id`, `descricao` e `data_hora` permite:
- Ordenar por data.
- Filtrar por conta.
- Paginar (futuro).
- Indexar.

---

## 8. Dificuldades encontradas

### 8.1. Herança JPA com Lombok
`@Data` em subclasses com `@EqualsAndHashCode(callSuper = true)` era necessário para evitar que o Lombok gerasse `equals`/`hashCode` ignorando os campos da superclasse `Conta`. Sem `callSuper = true`, duas contas com `saldo` diferente mas mesmo `titular` seriam consideradas iguais.

### 8.2. Senha BCrypt no Flyway
Gerar o hash BCrypt antecipadamente (antes de rodar a aplicação) exigiu usar Python localmente com `bcrypt`. Em um projeto real, isso seria automatizado em um script de setup.

### 8.3. CORS com frontend local
O frontend abre como `file://`, que não envia `Origin` para o servidor. O `CorsConfigurationSource` foi configurado com `allowedOriginPatterns("*")` em vez de `allowedOrigins("*")` porque este último não funciona junto com `allowCredentials(true)`.

### 8.4. `UserDetails` no modelo `Funcionario`
Implementar `UserDetails` diretamente na entidade JPA acopla o modelo ao Spring Security. Uma alternativa mais limpa seria um adaptador separado (`FuncionarioUserDetails`). Para o escopo desta atividade, o acoplamento foi aceito para reduzir a quantidade de classes.
