# DECISOES.md — Log de Decisões Arquiteturais

- Nome: Davi Cavalcante França
- Matricula: 2671244

---

## 1. Herança das entidades — `SINGLE_TABLE` vs `JOINED`

**Decisão:** `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)`

**Por quê:**
`ContaCorrente` e `ContaPoupanca` diferem apenas em comportamento
(métodos `calcularRendimento` e `calcularTributo`), não em estrutura de
colunas. Ambas possuem os mesmos campos: `numero`, `titular`, `saldo`.
Com `SINGLE_TABLE`, tudo fica em uma tabela `contas` e a coluna `tipo`
funciona como discriminador — sem `JOIN` extra a cada consulta.

A alternativa `JOINED` criaria duas tabelas extras (`conta_corrente`,
`conta_poupanca`) com `JOIN` obrigatório, adicionando complexidade sem
ganho real neste caso.

**Tradeoff aceito:** se `ContaCorrente` precisar de colunas específicas
no futuro (ex.: `limite_cheque_especial`), a estratégia deverá ser
migrada para `JOINED`. Por ora, `SINGLE_TABLE` é mais simples e eficiente.

---

## 2. Autenticação por validação direta (BCrypt), sem token

**Decisão:** `POST /auth/login` valida nome + senha via `BCryptPasswordEncoder`
e devolve os dados do funcionário. Sem sessão HTTP, sem token, sem cookie.

**Por quê:**
O enunciado pede autenticação de gerente, mas não especifica o mecanismo.
Optou-se pela abordagem mais simples compatível com o escopo da atividade:

- O backend valida a senha com BCrypt e responde com nome, cargo e agência.
- O frontend armazena esses dados em memória e exibe o overlay de login
  enquanto o usuário não se autenticar com sucesso.
- Ao clicar em "Sair", o overlay é reexibido — nenhum estado precisa ser
  invalidado no servidor.
- Todas as rotas da API são públicas no nível do Spring Security
  (`anyRequest().permitAll()`). O controle de acesso é inteiramente
  responsabilidade da camada de apresentação.

**Por que manter `spring-boot-starter-security` sem JWT:**
A dependência é necessária exclusivamente para o `BCryptPasswordEncoder`.
Remover o starter e usar uma lib BCrypt avulsa seria possível, mas
mantê-lo deixa a porta aberta para evoluções futuras (ex.: Basic Auth,
sessão, ou JWT) sem alterar o `pom.xml`.

**Tradeoff aceito:** sem proteção de rotas no servidor, qualquer cliente
com acesso à rede pode chamar os endpoints sem autenticar. Para um sistema
acadêmico local isso é aceitável; em produção seria necessário proteger
as rotas (Basic Auth, sessão ou token).

---

## 3. Senha do gerente armazenada com BCrypt no Flyway

**Decisão:** `V2__insert_gerente.sql` insere o hash BCrypt diretamente no SQL.

**Por quê:**
Armazenar a senha em texto plano é um erro de segurança. A alternativa
mais comum seria um `DataInitializer` no Spring que usa
`passwordEncoder.encode()` em tempo de execução — mas isso cria um
problema: ao rodar a aplicação pela segunda vez, o Spring tentaria
inserir o gerente novamente (ou precisaria de lógica de "já existe").

Com o hash pré-calculado no Flyway (`V2`), a migration roda **uma única
vez** (`ON CONFLICT DO NOTHING`) e a senha nunca fica em texto claro em
nenhum log, código-fonte ou variável de ambiente.

**Como gerar um novo hash BCrypt (strength 10):**
```python
import bcrypt
print(bcrypt.hashpw(b'nova_senha', bcrypt.gensalt(10)).decode())
```

---

## 4. Número de conta gerado via `MAX(numero) + 1`

**Decisão:** `ContaRepository.findMaxNumero()` retorna o maior número
atual; o service soma 1 ao resultado.

**Por quê:**
A alternativa com `nextval('conta_numero_seq')` via `@Query` nativa
cria dependência de dialeto PostgreSQL, quebrando testes com H2.
A abordagem `MAX` é portável e funciona com qualquer banco JPA.

**Tradeoff aceito:** em cenário de alta concorrência, dois threads
poderiam obter o mesmo `MAX`. A constraint `UNIQUE` na coluna `numero`
faz o banco rejeitar a segunda inserção. Para produção real, a sequence
SQL nativa seria o caminho correto.

---

## 5. `ApiResponse<T>` como envelope padrão

**Decisão:** todos os endpoints retornam `{ sucesso, mensagem, dados }`.

**Por quê:**
Sem envelope, o frontend precisaria inspecionar o status HTTP e lidar
com formatos diferentes para erro e sucesso. Com o envelope padronizado:

```javascript
const r = await api('POST', '/contas', body);
// r.sucesso, r.mensagem, r.dados — sempre disponíveis
```

O `GlobalExceptionHandler` também usa `ApiResponse.erro(mensagem)`,
garantindo que erros tenham o mesmo formato que respostas de sucesso.

---

## 6. Frontend em HTML/CSS/JS puro (sem framework)

**Decisão:** Opção A (Web) com HTML, CSS e Fetch API. Sem React, Vue ou Angular.

**Por quê:**
- Zero dependências externas — `index.html` é aberto direto no navegador.
- A API REST é o foco da atividade; o frontend é instrumental.
- Fetch API nativa é suficiente para consumir todos os endpoints.

**Tradeoff aceito:** sem reatividade de framework, o DOM é atualizado
manualmente via `innerHTML`. Para produção, um framework seria recomendado.

---

## 7. Histórico de operações como entidade JPA separada

**Decisão:** `HistoricoOperacao` tem tabela própria (`historico_operacoes`),
em vez de uma `List<String>` dentro de `Conta`.

**Por quê:**
Na versão anterior (Java puro), o histórico era um `ArrayList<String>`
em memória — perdido a cada restart. Com JPA, precisamos de persistência.
Uma coluna JSON/texto seria difícil de consultar e paginar. A tabela
separada com `conta_id`, `descricao` e `data_hora` permite ordenar por
data, filtrar por conta e indexar eficientemente.

---

## 8. Spring Security com `permitAll()` em vez de ausência total de segurança

**Decisão:** manter o `SecurityFilterChain` com `anyRequest().permitAll()`
em vez de desabilitar o Spring Security completamente.

**Por quê:**
- Desabilitar o auto-configure de Security exigiria
  `@SpringBootApplication(exclude = SecurityAutoConfiguration.class)`,
  o que também removeria o `BCryptPasswordEncoder` do contexto.
- Com `permitAll()`, o `BCryptPasswordEncoder` fica disponível como `@Bean`
  injetável no `AuthService` sem nenhuma configuração extra.
- O CORS fica centralizado no `SecurityFilterChain`, que é o ponto certo
  no Spring MVC para interceptar preflight (`OPTIONS`) antes dos controllers.

---

## 9. Dificuldades encontradas

### 9.1. Remoção do JWT sem quebrar o contexto do Spring

Ao remover JWT, `Funcionario` deixou de implementar `UserDetails` e
`AuthService` perdeu o `AuthenticationManager`. Isso exigiu revisar toda
a cadeia de dependências no `SecurityConfig` — que antes declarava
`UserDetailsService`, `DaoAuthenticationProvider` e `AuthenticationManager`
como `@Bean`. Com a simplificação, o `SecurityConfig` ficou com apenas
três beans: `SecurityFilterChain`, `PasswordEncoder` e `CorsConfigurationSource`.

### 9.2. Herança JPA com Lombok

`@Data` em subclasses exige `@EqualsAndHashCode(callSuper = true)` para
que o Lombok inclua os campos da superclasse `Conta` no `equals`/`hashCode`.
Sem isso, duas contas com `saldo` diferente mas mesmo `titular` seriam
consideradas iguais.

### 9.3. Senha BCrypt no Flyway

Gerar o hash BCrypt antes de rodar a aplicação exigiu executar Python
localmente com a biblioteca `bcrypt`. Em um projeto real isso seria
automatizado em um script de setup ou pipeline de CI.

### 9.4. CORS com frontend local (`file://`)

O frontend abre como `file://`, que não envia `Origin` para o servidor.
`allowedOriginPatterns("*")` foi necessário em vez de `allowedOrigins("*")`
pois o segundo não funciona quando combinado com `allowCredentials(true)`.
Como a versão sem JWT não usa `allowCredentials`, `allowedOrigins("*")`
também funcionaria — porém `allowedOriginPatterns` foi mantido por maior
flexibilidade.
