# Plano de backend — FinControl

Backend primeiro. O frontend Angular entra depois que este plano for aceito. Cada fase termina com contrato OpenAPI, regra no domínio e teste. A validação é por fase: o que estiver errado se ajusta antes da fase seguinte. A skill `.agents/skills/fincontrol-backend/SKILL.md` é o roteiro de implementação. Como chamá-la em cada fase: `docs/como-chamar-a-skill.md`. O banco sobe com `infra/docker-compose.yml` (MySQL e Portainer).

Data: 21/11/2026

---

## Decisões deste plano para confirmar

A diretriz hexagonal que você enviou entra inteira, com três pontos em que este plano escolhe um lado. São estes os itens para validar.

| Tema | Neste plano | Alternativa que fica de fora até você pedir |
|---|---|---|
| Banco | **MySQL 8.4**, `DECIMAL(15,2)`, Flyway | MongoDB com `Decimal128`. O domínio usa `BigDecimal` nos dois casos. O exemplo de Mongo na diretriz não vira o banco do v1: lançamento, conta e categoria se referenciam, e transferência precisa gravar os dois lados na mesma transação. |
| Código | Um módulo Maven `backend`, pacotes `domain`, `application`, `infrastructure` | Vários módulos Maven (`adapter-in-rest`, `adapter-out-persistence`). O limite entre camadas fica no ArchUnit, não na quantidade de `pom.xml`. |
| Extrato | Só **OFX**, de **qualquer banco**, por cliente | QIF, CSV e trava “somente Nubank”. |
| IA | Provedor cadastrado em `application.yml`: Ollama local, Claude (Anthropic) ou endpoint compatível com a API OpenAI | Provedor escolhido com `if` no domínio. Não há starter Spring AI chamado “Cursor”; um endpoint OpenAI-compatível cobre um gateway externo sem código novo. |
| Teste | JUnit 6, Mockito, AssertJ, JaCoCo 90%, ArchUnit e PMD. Sem banco no `mvn test` | Testcontainers, H2 e qualquer teste que suba MySQL |
| Segurança | Mend.io no `pom.xml` e, quando existir, no `package.json`: zero CVE crítica ou alta. SonarQube for IDE no código novo. ArchUnit no `mvn verify` | Entregar com alerta crítico ou alto aberto, ou versão já marcada como insegura |

O restante da diretriz vale como está: contract-first, domínio sem Spring e sem banco, `BigDecimal`, idempotência nas escritas, exceção de negócio semântica, controller sem regra.

---

## 1. Stack

| Peça | Escolha |
|---|---|
| Java | 25 (compila com `--release 21` se a imagem exigir) |
| Framework | Spring Boot 4.0.x, Spring Security 7, JWT próprio |
| Contrato | OpenAPI 3.1 em `src/main/resources/openapi/`, interfaces geradas antes do controller |
| Banco | MySQL 8.4 no Docker, serviço `mysql` de `infra/docker-compose.yml`, junto com o Portainer. InnoDB, `utf8mb4`, Flyway |
| Dinheiro | `BigDecimal` escala 2, `HALF_UP` no domínio; `DECIMAL(15,2)` na tabela |
| IA | Spring AI 2.x. O caso de uso depende de uma porta. O adaptador é o único que conhece `ChatClient` |
| Teste | JUnit 6 (o que o Boot 4 traz), AssertJ, Mockito. JaCoCo 0.8.16 exige 90%. ArchUnit 1.5.1 exige a hexagonal. PMD exige complexidade e tamanho. Sem Testcontainers |
| Segurança | SCA com Mend.io (Package Checker do IntelliJ e, no pipeline, Qodana ou Mend CLI). Estática com SonarQube for IDE. Versão estável e corrigida ao incluir ou subir biblioteca |

Senha com Argon2id. O access token é um JWT RS256 de 15 minutos. A geração, as claims (`iss`, `sub`, `role`, `clienteId`, `jti`, `iat`, `nbf`, `exp`) e o uso em `/login`, `/logout`, `/refresh_token` e `/me` estão na seção 16. O refresh não é JWT. `clienteId` vem só do token.

---

## 2. Pastas

```
backend/
  pom.xml
  src/main/java/br/com/fincontrol/
    domain/
      model/            Cliente, Usuario, Conta, Lancamento, Dinheiro, RegraClassificacao
      service/          CalculoSaldo, Conciliacao, GeradorParcelas, ClassificadorExtrato
      exception/        MesFechadoException, RegraDeNegocioException, ...
      port/
        in/             casos de uso (interfaces)
        out/            repositórios e AnaliseFinanceiraPort
    application/
      service/          orquestração e @Transactional
    infrastructure/
      in/web/           controllers que implementam a interface gerada do OpenAPI
      out/persistence/  JPA, entidades de tabela, Flyway
      out/security/     emissão e leitura do JWT, cookie de refresh
      out/external/     parser OFX e adaptador Spring AI
  src/main/resources/
    openapi/            um yaml por domínio — fonte da verdade
    db/migration/
    prompts/analise-financeira.st
    application.yml
  src/test/java/...
```

Dependência permitida: `infrastructure` → `application` → `domain`. O `domain` não importa Spring, JPA, Jackson nem cliente HTTP. O ArchUnit quebra o build se isso acontecer. O detalhe das dependências Maven está na seção 13.

DTO de borda (gerado do OpenAPI) não é entidade de domínio. O controller traduz um no outro.

---

## 3. Quem entra no sistema

Dois clientes isolados, cada um com o próprio login. Um terceiro login, o Admin Master, só cria cliente e administrador e consulta a auditoria. O Master não lê lançamento, extrato nem saldo.

| Login | Cliente | O que vê |
|---|---|---|
| O seu e-mail | O seu | Conta Nubank, conta Flash do benefício, regra de salário vinda do Itaú, os seus OFX |
| O e-mail da sua mãe | O dela | Conta Banco do Brasil, as regras que ela cadastrar, os OFX dela |
| Master | nenhum (`clienteId` nulo) | Cadastro dos dois clientes e a trilha de auditoria |

Dentro de um cliente podem existir outros usuários (administrador, colaborador, leitor). Um leitor do seu cliente vê as suas contas e não as dela.

Papéis: `MASTER`, `ADMIN`, `COLABORADOR`, `LEITOR`. Escritas de lançamento, conta, orçamento e importação: admin e colaborador. Leitor só consulta. Usuários do cliente: só admin. Clientes e auditoria: só master.

---

## 4. Regras que ficam no domínio

Métodos curtos, nome de negócio, invariante na entidade. Nada disso roda no controller nem no repositório.

- **Dinheiro.** Valor sempre positivo, escala 2. O sinal é a natureza (`RECEITA` ou `DESPESA`). Proibido `double`.
- **Saldo da conta.** `saldoInicial` na data do ajuste, mais créditos, menos débitos, mais transferências. Lançamento estornado fica de fora. Item de extrato já conciliado não entra de novo.
- **Parcela.** Total dividido em N (1 a 60). Centavos que sobram ficam na primeira parcela. Dia 31 cai no último dia do mês curto. Chave natural: `grupoParcela` + número.
- **Edição.** Lançamento de mês aberto pode mudar valor, data, categoria, conta e descrição. Mês fechado recusa com `MesFechadoException`. Estornado não se edita, não se exclui e não se estorna de novo.
- **Estorno.** Motivo obrigatório. Grava quem e quando. Sai de todo somatório.
- **Fechamento de mês.** Uma linha por cliente e mês. Bloqueia criar, editar, estornar, excluir, anexar, transferir e ajustar saldo com data naquele mês.
- **Transferência.** Duas contas do mesmo cliente, contas diferentes, um valor. Não entra em receita nem despesa do dashboard.
- **Conciliação.** Mesma conta, valor exato, data até 5 dias. Grava os dois lados na mesma transação. Desfazer limpa os dois. O mesmo par não concilia duas vezes.
- **Classificação do OFX.** Regra do cliente, em ordem de prioridade. A primeira que casa define a categoria. Sem regra, o item fica sem categoria.
- **Salário (só no seu cliente, como dado, não como `if` de banco).** Crédito cuja origem é a conta Itaú cadastrada e que cai numa das faixas: dia 15 (ou o próximo dia útil) no valor R$ 4.800,00; do dia 28 ao dia 2 do mês seguinte, valor de R$ 3.750,00 a R$ 3.850,00. Isso é receita na categoria Salário, na conta Nubank. Outro Pix da mesma conta Itaú não vira salário. No cliente da sua mãe essa regra não existe.
- **Benefício Flash (só no seu cliente).** A empresa deposita cerca de R$ 1.570,00 no fim do mês numa conta da própria Flash, não no Nubank e não no cartão de crédito. A conta é do tipo `BENEFICIO`. O crédito é receita na categoria Benefícios, gerado por recorrência no último dia do mês, com valor previsto de R$ 1.570,00. Se o depósito real vier entre R$ 1.520,00 e R$ 1.620,00, o lançamento é ajustado para o valor recebido. Esse crédito não nasce do OFX do Nubank.
- **Gasto coberto pela Flash.** Comida, abastecimento e qualquer outro gasto aceito no cartão flex são despesa na conta Flash, não na conta Nubank. Comida cai em Alimentação, abastecimento em Transporte, e o restante na categoria que couber. O saldo disponível da Flash é o crédito do benefício menos essas despesas. O saldo do Nubank não muda.
- **Idempotência.** Reenviar o mesmo comando não duplica. Ver seção 7.

Tipo de despesa do lançamento: `FIXA` ou `VARIAVEL`. Investimento é categoria, não um terceiro tipo.

---

## 5. Tabelas

Ordem do Flyway. Migração aplicada não se edita.

| Versão | Tabelas |
|---|---|
| V1 | Banco, usuários `fincontrol_app` e `fincontrol_migrate`. Senha só por ambiente. |
| V2 | `cliente`, `usuario` |
| V3 | `categoria`, `conta` |
| V4 | `lancamento`, `transferencia`, `parcela`, `fechamento_mes` |
| V5 | `orcamento`, `meta`, `meta_aporte`, `recorrencia` |
| V6 | `extrato`, `extrato_item`, `regra_classificacao` |
| V7 | `anexo`, `refresh_token`, `access_token_revogado`, `idempotency_key` |
| V8 | `auditoria` (append-only; a aplicação só faz `INSERT` e `SELECT`) |
| V9 | Categorias padrão e views de dashboard |

`conta` guarda `banco`, `codigo_banco` (`BANKID` do OFX, quando já importou), `identificador_conta` (o `ACCTID`, para reconhecer o próximo arquivo) e `tipo` (`CORRENTE`, `POUPANCA`, `CARTAO_CREDITO`, `BENEFICIO`). `BENEFICIO` não tem limite de cartão. A Flash do seu cliente é uma conta `BENEFICIO`.

`extrato_item.fitid` é `VARCHAR(255)`. Único em (`cliente_id`, `conta_id`, `fitid`), não só dentro de um arquivo. O Nubank reexporta o mesmo período com o mesmo UUID; outro banco pode usar número longo. Dois clientes podem ter o mesmo `FITID` sem colidir.

`regra_classificacao` pertence ao cliente: prioridade, texto contido no `MEMO`, natureza, categoria, e os campos opcionais da faixa de salário (conta de origem, dia inicial, dia final, valor mínimo, valor máximo).

Toda tabela de dados tem `cliente_id`. Repositório sem esse filtro não existe. Auditoria pode ter `cliente_id` nulo na ação global do Master.

---

## 6. Contrato antes do código

Arquivos em `src/main/resources/openapi/`. Prefixo `/api/v1`. O controller implementa a interface gerada. Divergência não compila.

Erro único, RFC 9457, com `type`, `title`, `status`, `detail`, `instance` e `codigoErro`. `detail` em português, sem stack, sem SQL, sem nome de classe. Validação devolve `erros` por campo.

Cada `operationId` do OpenAPI ganha pedidos no Postman e no Bruno, com o caminho feliz e os erros desta tabela. O formato das coleções está na seção 15.

| Situação | HTTP | codigoErro |
|---|---|---|
| Campo inválido | 400 | `VALIDACAO` |
| Sem token | 401 | `NAO_AUTENTICADO` |
| Perfil sem permissão | 403 | `ACESSO_NEGADO` |
| Id inexistente ou de outro cliente | 404 | `NAO_ENCONTRADO` |
| Mês fechado | 409 | `MES_FECHADO` |
| Regra de negócio (estorno repetido, OFX sem transação, arquivo que não é OFX) | 422 | código específico |
| Provedor de IA desligado ou fora | 503 | `IA_DESABILITADA` ou `IA_INDISPONIVEL` |
| Resposta do modelo fora do schema | 422 | `IA_RESPOSTA_INVALIDA` |

---

## 7. Idempotência

| Operação | Chave | Segunda vez |
|---|---|---|
| Criar lançamento, transferência, ajuste de saldo | Cabeçalho `Idempotency-Key` por cliente, guardado em `idempotency_key` com o hash do corpo e a resposta | Devolve a mesma resposta. Corpo diferente com a mesma chave responde 409 |
| Importar OFX | `cliente_id` + `conta_id` + `fitid` | Item já visto é ignorado e entra na contagem de duplicados |
| Rendimento do `<BALLIST>` (Nubank manda rendimento fora do `STMTTRN`) | `rendimento:{conta}:{início}:{fim}` | Não duplica |
| Gerar recorrência | `recorrencia_id` + mês | Job diário pode rodar de novo |
| Gerar parcela | `grupo_parcela` + número | Não cria a parcela outra vez |
| Conciliar | único (`extrato_item`, `lancamento`) | Não vincula o mesmo par duas vezes |

---

## 8. OFX de qualquer banco

O parser em `infrastructure/out/external` lê OFX SGML 1.x e XML 2.x. Não olha o nome do banco para aceitar ou recusar.

De cada `<STMTTRN>` saem `FITID`, data (oito primeiros dígitos de `DTPOSTED`, sem converter fuso), valor, natureza e texto (`MEMO` ou `NAME`). Item sem `FITID` não grava; a resposta lista o aviso.

`ORG`, `FID` e `BANKID` servem para sugerir a conta do cliente. Uma conta daquele banco já vem selecionada. Várias: a pessoa escolhe. Arquivo de outro banco que a conta escolhida: 422, nada é gravado. `ACCTID` atualiza o identificador da conta para o próximo arquivo e não vai para a IA.

O rendimento em `<BALLIST>` só entra quando o bloco existir (o OFX do Nubank traz `RENDIMENTO LIQUIDO`). Vira receita na categoria Rendimentos, com a chave sintética da seção 7. Banco que não manda esse bloco simplesmente não tem esse item.

`<LEDGERBAL>` volta na resposta da importação para a tela comparar com o saldo calculado. Não altera o saldo sozinho.

Categoria sai das `regra_classificacao` do cliente, depois que o item foi lido. As regras do seu Nubank (Pix enviado, fatura, débito por estabelecimento, boleto, salário do Itaú) são carga inicial **desse** cliente. O cliente da sua mãe começa sem elas.

O arquivo de prova do seu cliente é `NU_7224833363_01OUT2026_22OUT2026.ofx`: 38 movimentos, o crédito de R$ 4.800,00 em 15/10 classificado como Salário, o crédito de R$ 1.200,00 sem essa classificação, e a segunda importação com zero itens novos. Um OFX de outro banco entra no teste de parser com os campos padrão, sem exigir o formato de `MEMO` do Nubank.

---

## 9. Análise por IA

A porta `AnaliseFinanceiraPort` fica no domínio. O caso de uso manda só o agregado do mês: totais por categoria, variação contra os três meses anteriores e os maiores lançamentos. Não manda lista completa, `ACCTID`, agência, conta, CPF, CNPJ nem comprovante.

A resposta é um record validado: resumo, insights e ações sugeridas. É sugestão. Não grava lançamento e não gera auditoria. Schema inválido: uma nova tentativa; se persistir, 422. Provedor fora: 503, nunca 200 vazio.

Cadastro no `application.yml`, sem chave no arquivo versionado:

```yaml
spring:
  ai:
    model:
      chat: ${IA_PROVEDOR:ollama}   # ollama | anthropic | openai | none
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        options:
          model: ${IA_MODELO:qwen3.5:9b}
          temperature: 0.2
          num-ctx: ${IA_NUM_CTX:8192}
    anthropic:
      api-key: ${ANTHROPIC_API_KEY:}
      chat:
        options:
          model: ${IA_MODELO_ANTHROPIC:claude-sonnet-4-5}
          temperature: 0.2
    openai:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${IA_MODELO_OPENAI:gpt-4.1-mini}
          temperature: 0.2
```

- `ollama`: modelo local, processo nativo no Windows, fora do Docker, para usar a GPU.
- `anthropic`: Claude, com a chave no ambiente.
- `openai`: API OpenAI ou outro endpoint compatível, trocando `OPENAI_BASE_URL` e a chave. É o encaixe para um provedor externo que fale esse protocolo, inclusive um gateway que você apontar no lugar do Cursor.
- `none`: a análise responde 503 `IA_DESABILITADA`.

Cada provedor tem a própria variável de modelo. O teste mocka `ChatModel` e não sobe Ollama nem chama rede.

---

## 10. Fases

Cada fase só começa com o contrato da anterior estável. O critério de aceite é o que você confere.

### Fase 0 — Fundação

Módulo, Boot, `infra/docker-compose.yml` com MySQL 8.4 e Portainer, Flyway, gerador OpenAPI, `ApiExceptionHandler`, JaCoCo, ArchUnit e PMD.

Aceite: dentro do WSL, `docker compose -f infra/docker-compose.yml up -d` sobe o MySQL em `localhost:3306` e o Portainer em `https://localhost:9443`, ligado ao Docker dessa distro. `mvn verify` passa sem Docker. O ArchUnit recusa Spring no `domain`. O JaCoCo recusa cobertura de instrução abaixo de 90%. O PMD recusa método longo ou complexo demais. O Package Checker (Mend.io) não mostra CVE crítica ou alta no `pom.xml`. O SonarQube for IDE não deixa issue nova de severidade alta no código desta fase. A aplicação, fora do teste, sobe e aplica as migrações nesse MySQL.

### Fase 1 — Login e clientes

Contrato `auth.yaml` e `master.yaml`. Quatro rotas de sessão, mais o bloqueio progressivo. Master cria cliente e o primeiro administrador. Dois clientes de prova: o seu e o da sua mãe, cada um com o próprio e-mail.

| Método | Rota | Função |
|---|---|---|
| `POST` | `/api/v1/auth/login` | E-mail e senha. Devolve o access token e o `refreshToken` no corpo; o mesmo valor vai no header `X-Refresh-Token` e no cookie `HttpOnly`. |
| `POST` | `/api/v1/auth/logout` | Exige `Authorization: Bearer`. Revoga o refresh e o `jti` da sessão. |
| `POST` | `/api/v1/auth/refresh_token` | Gira o refresh: o valor usado morre e nasce outro. |
| `GET` | `/api/v1/auth/me` | Devolve o usuário do token: id, nome, e-mail, perfil e `clienteId`. |

A coleção Postman e a coleção Bruno desta fase cobrem o sucesso e o erro de cada uma, como na seção 15.

Aceite: o caso de uso chamado com o token do seu cliente pede à porta só o seu `clienteId`. O caso de uso do Master não chama porta de lançamento. Senha não volta na resposta, no `/me` nem na auditoria. O JWT emitido traz `role`, `jti` e as demais claims da seção 16. Isso é teste com Mockito, sem banco. Com a aplicação no ar, `newman` e `bru` decodificam o access token e conferem essas claims.

### Fase 2 — Categorias e contas

CRUD nos dois clientes. Categorias padrão na criação do cliente, inclusive Salário, Rendimentos e Benefícios. Conta com banco, tipo e saldo inicial. O tipo `BENEFICIO` existe para carteira que não é conta corrente nem cartão de crédito.

Aceite: conta do Nubank e a conta Flash ficam no seu cliente. A conta do Banco do Brasil fica no dela, sem Flash. Leitor não cria. Despesa lançada na Flash não altera o saldo do Nubank.

### Fase 3 — Lançamentos, saldo, mês e estorno

Criar, editar, estornar, excluir o que não está estornado, parcelas, fechamento e reabertura. Saldo por conta numa consulta só. Cabeçalho de idempotência.

Aceite: parcela com resto de centavo bate com o domínio; mês fechado responde 409 em todas as escritas; estorno sai do saldo; a mesma `Idempotency-Key` não duplica.

### Fase 4 — Transferência, orçamento, meta, recorrência

Transferência atômica. Orçamento por categoria e mês, alerta calculado no backend a partir de 80%. Meta e aporte. Recorrência com job diário idempotente. Pausa não gera retroativo ao retomar.

No seu cliente, a recorrência do benefício Flash gera, no último dia do mês, uma receita de R$ 1.570,00 na conta Flash, categoria Benefícios. Comida, abastecimento e os outros gastos cobertos pelo cartão são despesas lançadas nessa mesma conta. O job não cria essas despesas: só o depósito.

Aceite: transferência não altera o total de receitas e despesas. Rodar o job duas vezes no mesmo mês não duplica ocorrência. O depósito da Flash não aparece no saldo do Nubank. Uma despesa de Alimentação ou Transporte na Flash reduz só o saldo da Flash.

### Fase 5 — OFX e regras

Contrato de importação. Parser genérico, deduplicação por `FITID`, regras por cliente, carga da regra de salário no seu cliente. Resposta com importados, duplicados, sem categoria, avisos e saldo informado pelo banco.

Aceite: o OFX do Nubank passa no critério da seção 8 e não cria o depósito nem os gastos da Flash. Um segundo cliente importa um OFX de outro banco na conta dele, sem herdar a regra de salário nem a conta Flash. Arquivo que não é OFX responde 422.

### Fase 6 — Conciliação e comprovante

Sugestão por conta, valor e 5 dias. Conciliar e desfazer. Anexo PNG, JPEG ou PDF até 1 MB, binário fora do banco (`storage_key`).

Aceite: conciliar grava os dois lados; desfazer limpa os dois; o valor conciliado não dobra o saldo; mês fechado bloqueia o anexo.

### Fase 7 — Auditoria

Aspecto nas escritas, na mesma transação. Diff em alteração, snapshot de e-mail e perfil, mascaramento de senha e token. Consulta só do Master, com filtro e paginação por cursor. Análise por IA não grava linha.

Aceite: criar lançamento no seu cliente gera um registro visível só para o Master. Falha de negócio não deixa registro de sucesso.

### Fase 8 — Análise

`POST /api/v1/analise` com o mês e o tipo (presuntiva, comparativa, comportamental) mais a pergunta. Trocar `IA_PROVEDOR` não muda Java.

Aceite: com `none`, 503. Com porta mockada, o record volta validado. O prompt de teste não contém `ACCTID` nem documento. Nenhuma linha nova em `auditoria`.

---

## 11. Fora deste plano

- Telas Angular e PrimeNG. O contrato desta lista é o que o frontend vai consumir depois.
- QIF e CSV.
- Fatura de cartão lançada linha a linha. “Pagamento de fatura” no OFX da conta corrente entra como despesa. Compras do cartão continuam lançamento manual até existir um OFX de cartão.
- Plano comercial, limite de clientes, MongoDB.
- Rateio e rebate. Não estão no protótipo. Se entrarem, viram regra de domínio nova, com teste próprio, numa fase posterior.
- Testcontainers e teste de integração que sobe MySQL. O schema é aplicado quando a aplicação sobe no Compose, não na suíte.

---

## 12. Definição de pronto de cada fase

- OpenAPI atualizado antes do controller.
- Regra no `domain`, caso de uso transacional na `application`, adaptador na `infrastructure`.
- `BigDecimal` e `LocalDate`. Nenhum `double` e nenhuma data de negócio em `String` no domínio.
- `clienteId` do token em toda leitura e escrita.
- Teste de domínio da regra nova. Isolamento de cliente testado no caso de uso: a porta recebe o `clienteId` do token.
- `mvn verify` verde: JaCoCo em 90% ou mais, ArchUnit e PMD sem violação. Sem Testcontainers.
- Mend.io sem CVE crítica ou alta nas dependências desta fase. SonarQube for IDE sem issue nova de severidade alta no código entregue.
- Erro novo passa pelo handler único, com `codigoErro`.
- Migração Flyway nova, sem editar as anteriores.
- Coleção Postman e coleção Bruno atualizadas no mesmo passo do OpenAPI: caminho feliz e cada erro aplicável da operação. As duas coleções batem com os `operationId`.

---

## 13. Testes, cobertura e arquitetura

A suíte padrão é `mvn verify`. Ela não sobe Docker, não usa Testcontainers e não abre conexão com MySQL. Banco de verdade continua no Compose, para rodar a aplicação. O teste de unidade prova regra, orquestração, contrato HTTP com caso de uso mockado, fronteira de pacotes e cobertura. O HTTP de ponta a ponta, com sucesso e erro, fica nas coleções Postman e Bruno da seção 15, contra a aplicação no ar.

| Camada | Como testa |
|---|---|
| Domínio | JUnit 6 e AssertJ. Sem Spring. Saldo, parcela, estorno, classificação e conciliação entram aqui. |
| Caso de uso | Mockito nas portas. Verifica `clienteId` do token, mês fechado, idempotência e que a IA não audita. |
| REST | MockMvc com o caso de uso mockado. Confere status, `codigoErro` e o corpo. |
| Parser OFX | Teste de unidade lendo o arquivo `NU_7224833363_01OUT2026_22OUT2026.ofx` do disco. Sem rede e sem banco. |
| Adaptador de IA | `ChatModel` mockado. Sem Ollama e sem Claude no build. |
| Persistência | Mapeador entidade ↔ domínio em teste puro. A query SQL não roda na suíte. |

### Cobertura: JaCoCo 90%

Plugin `org.jacoco:jacoco-maven-plugin` **0.8.16** (a partir do 0.8.14 há suporte oficial a Java 25). O goal `check` quebra o build se a cobertura de instrução ficar abaixo de **0,90**. O relatório sai em `target/site/jacoco`.

Ficam de fora do denominador, porque não são código de negócio: classes geradas do OpenAPI e a classe `*Application` do Boot. O restante (`domain`, `application`, `infrastructure`) entra na conta.

### Hexagonal: ArchUnit

Dá para validar. A dependência é de teste:

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit6</artifactId>
    <version>1.5.1</version>
    <scope>test</scope>
</dependency>
```

O Boot 4 roda JUnit 6, então o artefato é `archunit-junit6`, não o `archunit-junit5`. A regra pronta `onionArchitecture()` é a hexagonal (portas e adaptadores):

- `domain.model` e `domain.service` não dependem de `application` nem de `infrastructure`.
- `domain` não depende de `org.springframework..`, `jakarta.persistence..` nem de cliente HTTP.
- `application` não depende de `infrastructure`.
- Os adaptadores `infrastructure.in.web`, `infrastructure.out.persistence`, `infrastructure.out.security` e `infrastructure.out.external` não dependem uns dos outros.
- `domain` não usa `double`, `Double` nem `float` para dinheiro.

Uma regra extra recusa `System.out`, exceção genérica `Exception` lançada pelo domínio e injeção de dependência por campo (`@Autowired` em atributo).

### Clean code: o que o Maven consegue exigir

Não existe uma dependência que julgue nome de método ou responsabilidade única. O que o build consegue barrar é tamanho, complexidade e alguns vícios. Para isso entra o `maven-pmd-plugin` (PMD 7), com falha no `verify`:

| Regra | Limite |
|---|---|
| Complexidade ciclomática | 10 por método |
| Complexidade cognitiva | 15 por método |
| Tamanho do método | 40 linhas |
| Classe deus | classe com responsabilidade demais, pelo critério do PMD |
| Variável local não usada | zero |

Checkstyle e SpotBugs ficam de fora desta etapa. Checkstyle mede formato, não desenho. SpotBugs acha defeito pontual e não substitui o PMD nem o ArchUnit.

O `mvn verify` só fica verde com os três: testes, JaCoCo em 90% e zero violação de ArchUnit e de PMD.

---

## 14. Segurança das dependências e do código

Três checagens se completam. Nenhuma substitui a outra.

| Checagem | O que olha | Quando bloqueia |
|---|---|---|
| Mend.io (SCA) | Dependência declarada e transitiva, CVE conhecida e pacote malicioso | CVE **crítica** ou **alta**. Pacote marcado como malicioso também bloqueia |
| SonarQube for IDE | Código que estamos escrevendo: bug, falha de segurança, código morto | Issue **nova** de severidade alta no que a fase entrega |
| ArchUnit | Fronteira hexagonal, já no `mvn verify` | Qualquer violação de pacote |

### Mend.io

No IntelliJ, o plugin **Package Checker** (vem com a IDE e usa a base da Mend.io) analisa o `pom.xml`. Quando o frontend existir, o mesmo plugin analisa o `package.json`. A inspeção é **Code | Analyze Code | Vulnerable Dependencies**. A fase só fecha com a aba Security sem item crítico ou alto.

A correção é subir para a versão estável que o checker indicar, ou trocar a biblioteca. Não se ignora CVE crítica ou alta para “seguir a fase”. Alerta médio ou baixo fica registrado e não trava a entrega.

No pipeline, o mesmo corte. O par natural do Package Checker é o **Qodana**, que também consulta a Mend.io. Se houver conta Mend, o CLI da Mend no lugar do Qodana, com falha em crítica e alta. Os dois não precisam rodar juntos.

Entram nessa regra, entre outras: Spring Boot, Spring Security, Spring AI, driver do MySQL, Flyway, JaCoCo, ArchUnit e, no frontend, Angular, PrimeNG, `@primeng/themes`, `primeicons` e `chart.js`.

Versão nova entra pinada, na release estável já corrigida. Sem faixa solta (`4.+`, `latest`) e sem artefato depreciado ou com alerta crítico ou alto já publicado. A versão escolhida é a do `pom.xml` ou do `package.json`, não uma diferente da que o checker analisou.

---

## 15. Coleções Postman e Bruno

As coleções são entregue de fase, versionadas no repositório. Elas batem na aplicação real, com o MySQL do Compose já no ar. Não entram no `mvn verify` e não usam Testcontainers. O JUnit prova a regra com mock. A coleção prova o HTTP de ponta a ponta, inclusive o erro.

```
contracts/
  postman/FinControl.postman_collection.json
  postman/local.postman_environment.example.json
  bruno/fincontrol/bruno.json
  bruno/fincontrol/auth/<cenario>.bru
  bruno/fincontrol/<dominio>/<cenario>.bru
```

Uma pasta por domínio. Um pedido por cenário, nomeado com o `operationId` e o status (`login_200`, `login_401`). Pedido sem `operationId` correspondente no OpenAPI é erro. Falta o caminho feliz ou falta um erro aplicável, a fase não fecha. Um script `contract:check` compara os `operationId` do OpenAPI com os nomes dos pedidos e falha quando a cobertura não fecha.

O ambiente traz `baseUrl` e um e-mail por pessoa (`emailUsuario`, `emailMae`, `emailMaster`). A senha do seed local fica só no ambiente da máquina, fora do Git. O pedido de login grava o access token e o refresh, que as requisições seguintes mandam no header `X-Refresh-Token`. Nenhum token literal no arquivo da coleção.

Todo pedido tem teste, não só a chamada:

- status esperado;
- `application/json` no sucesso e `application/problem+json` no erro;
- no erro, presença de `type`, `title`, `status`, `detail`, `instance` e `codigoErro`, com o `status` do corpo igual ao HTTP;
- `detail` sem stack, sem `Exception`, sem SQL;
- sucesso confere o schema da resposta;
- corpo de sucesso sem `senha`, `senha_hash` nem `Authorization`; o refresh só aparece no campo `refreshToken` do login e do refresh, nunca em outro corpo.

### Sessão, na fase 1

| Pedido | Resultado |
|---|---|
| `POST /api/v1/auth/login` com e-mail e senha certos | 200, access token JWT, `refreshToken` no corpo igual ao header `X-Refresh-Token` e ao cookie, claims da seção 16 conferidas no payload, e em seguida `GET /me` com `Authorization: Bearer` devolve a mesma pessoa |
| `POST /login` sem e-mail, sem senha ou e-mail malformado | 400 `VALIDACAO`, com `erros` por campo |
| `POST /login` com senha errada | 401 `NAO_AUTENTICADO`, mensagem genérica |
| `POST /login` com e-mail que não existe | 401 `NAO_AUTENTICADO`, a mesma mensagem, para não revelar se o e-mail existe |
| `POST /login` de usuário inativo ou de cliente inativo | 401 `NAO_AUTENTICADO`, a mesma mensagem |
| Várias falhas seguidas do mesmo e-mail | 429 `LOGIN_BLOQUEADO` até acabar o bloqueio |
| `GET /api/v1/auth/me` com o access token | 200, com `clienteId` do seu cliente; no Master, `clienteId` nulo |
| `GET /me` sem token, com token expirado ou adulterado | 401 `NAO_AUTENTICADO` |
| `POST /api/v1/auth/refresh_token` com o header `X-Refresh-Token` válido | 200, access token novo, `refreshToken` novo no corpo e cookie novo; o valor anterior deixa de valer |
| `POST /refresh_token` de novo com o refresh já girado | 401 `NAO_AUTENTICADO` |
| `POST /refresh_token` sem header nem cookie, com valor expirado ou já revogado | 401 `NAO_AUTENTICADO` |
| `POST /api/v1/auth/logout` com sessão válida | 204, e o refresh seguinte responde 401 |
| `POST /api/v1/auth/logout` com Bearer válido | 204, o refresh daquele `jti` é revogado e o refresh seguinte responde 401 |
| `POST /logout` sem Bearer | 401 `NAO_AUTENTICADO` |

O login do seu e-mail e o da sua mãe produzem tokens que não leem dados um do outro. Esse par de pedidos entra na pasta de auth e se repete em cada pasta de dado (conta, lançamento, extrato): 200 com o dono, 404 quando o id é do outro cliente.

### Demais fases

Cada operação nova repete o que se aplica: 200 ou 201, 400, 401, 403 do leitor ou do Master fora da rota dele, 404 de id inexistente e de id do outro cliente, 409 no mês fechado, 413 no anexo acima de 1 MB, 422 na regra de negócio e 503 na análise com `IA_PROVEDOR=none`.

Estado que o erro precisa (mês fechado, lançamento de outro cliente) é preparado por pedidos de setup na própria pasta e desfeito no fim, para a coleção rodar num banco recém-migrado com o seed local.

Com a aplicação em `http://localhost:8080` e o MySQL do Compose no ar:

```bash
newman run contracts/postman/FinControl.postman_collection.json \
  -e contracts/postman/local.postman_environment.json --reporters cli,junit --bail
bru run contracts/bruno/fincontrol --env local
```

Falha de asserção quebra a validação da fase. Coleção apontando para mock não conta.

---

## 16. Implementação do login e do JWT

A fase 1 implementa a sessão de verdade. O domínio pede um token por uma porta. Quem assina e quem lê o JWT é `infrastructure/out/security`. O filtro HTTP, em `infrastructure/in/web`, só converte o Bearer já validado em autenticação do Spring. Controller não monta claim e não escolhe perfil.

### Chave e formato

Access token é JWT compacto, assinatura **RS256**. A chave privada e a pública vêm de `JWT_PRIVATE_KEY_PEM` e `JWT_PUBLIC_KEY_PEM`. Não há chave no repositório nem no `application.yml`. O cabeçalho do token leva `alg=RS256` e `typ=JWT`. Algoritmo diferente, assinatura inválida ou chave trocada responde 401.

O refresh **não** é JWT. É um valor opaco de 256 bits, guardado só como hash SHA-256 em `refresh_token`, devolvido no corpo do login e do refresh e aceito no header `X-Refresh-Token` (precedência) ou no cookie `refresh_token` com `HttpOnly`, `Secure` e `SameSite=Strict`, path `/api/v1/auth`, validade de 7 dias.

### Claims do access token

Cada login e cada refresh emitem um JWT novo. As claims são estas, e nenhuma outra:

| Claim | Conteúdo |
|---|---|
| `iss` | `fincontrol` |
| `sub` | id do usuário, em texto |
| `role` | um perfil: `MASTER`, `ADMIN`, `COLABORADOR` ou `LEITOR` |
| `clienteId` | id do cliente. No Master a claim não vai no token |
| `jti` | UUID novo a cada emissão. Dois tokens não compartilham `jti` |
| `iat` | instante da emissão, em segundos |
| `nbf` | igual a `iat` |
| `exp` | `iat` mais 15 minutos |

O filtro lê `role` e cria uma autoridade só: `ROLE_MASTER`, `ROLE_ADMIN`, `ROLE_COLABORADOR` ou `ROLE_LEITOR`. Não existe claim `roles` paralela que possa divergir. `clienteId` do token é a única fonte de tenant. Corpo de requisição com outro cliente é ignorado.

### O que cada rota faz com o token

`POST /api/v1/auth/login` confere e-mail e senha (Argon2id). Se bater, grava um refresh e devolve:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "refreshToken": "<opaco>"
}
```

O cookie de refresh sai no `Set-Cookie` com o mesmo valor de `refreshToken`, e o mesmo valor também é aceito no header `X-Refresh-Token`. O corpo não traz senha nem hash.

`GET /api/v1/auth/me` exige `Authorization: Bearer`. A resposta sai do usuário carregado pelo `sub`, não de claim solta: `id`, `nome`, `email`, `role`, `clienteId`. Se o usuário ou o cliente estiver inativo, 401, mesmo com assinatura válida.

`POST /api/v1/auth/refresh_token` lê o header `X-Refresh-Token` (ou o cookie, nessa ordem), confere o hash e a validade, revoga essa linha e grava outra. A resposta tem o mesmo formato do login, com JWT novo (`jti` novo), `refreshToken` novo no corpo e cookie novo. O valor anterior e o access token anterior deixam de valer.

`POST /api/v1/auth/logout` exige `Authorization: Bearer` e revoga o refresh ligado ao `jti` atual mais o próprio `jti`. Resposta 204. Sem Bearer, 401.

### Revogação do `jti`

Logout e refresh gravam o `jti` antigo em `access_token_revogado` (`jti` único, `expira_em` igual ao `exp` do token). O filtro rejeita com 401 um Bearer cujo `jti` está nessa tabela. Linha com `expira_em` no passado pode ser apagada: o próprio `exp` já recusa o token.

### O que a fase prova

O teste do adaptador, com um par de chaves gerado na memória do teste, confere `iss`, `sub`, `role`, `clienteId`, `jti`, `iat`, `nbf` e `exp`, e recusa token com assinatura trocada. O Master sai sem `clienteId`. O refresh gera outro `jti`.

Na coleção, o script do login decodifica o payload e exige essas claims. O `role` do seu usuário é `ADMIN`, o da sua mãe é `ADMIN` no cliente dela, o do Master é `MASTER` e sem `clienteId`. Em seguida `GET /me` com esse Bearer devolve o mesmo `sub` e o mesmo `role`. O pedido com o `jti` já deslogado responde 401.
