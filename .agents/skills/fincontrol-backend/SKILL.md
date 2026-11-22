---
name: fincontrol-backend
description: Implementa o backend FinControl fase a fase (Java 25, Spring Boot 4, hexagonal, OpenAPI, MySQL 8.4 no Docker Compose com Portainer, JWT, OFX, Postman/Bruno, JaCoCo 90%, ArchUnit, PMD, Mend.io). Use ao criar ou alterar backend/, contracts/, infra/docker-compose.yml, migração Flyway, login, lançamento, extrato ou análise por IA.
---

# Backend FinControl

Fonte fechada: `docs/plano-backend.md`. Não reabra MySQL vs MongoDB, Testcontainers, QIF/CSV nem micro-frontend. Implemente uma fase por vez e pare no aceite dela. Como o usuário chama cada fase: `docs/como-chamar-a-skill.md`.

Se a mensagem citar um número de fase, implemente só essa fase. Não avance para a seguinte.

## Ambiente

O banco não sobe no teste. Sobe no Docker:

```bash
cp infra/.env.example infra/.env
docker compose -f infra/docker-compose.yml up -d
```

- MySQL 8.4 em `localhost:3306`, banco `fincontrol`, charset `utf8mb4`. Usuário da aplicação `fincontrol_app`. Usuário do Flyway `fincontrol_migrate` (criado em `infra/mysql/init/01-usuario-migrate.sh`).
- Portainer em `https://localhost:9443` (primeiro acesso define a senha de admin).
- Senha só em `infra/.env`, que não entra no Git. Ollama fica nativo no Windows, fora deste compose.

`mvn verify` não conecta no MySQL e não usa Testcontainers. Coleção Postman/Bruno só roda com a aplicação no ar apontando para esse MySQL.

## Ordem dentro de cada fase

1. Editar `backend/src/main/resources/openapi/<dominio>.yaml` antes de controller ou serviço.
2. Regra em `domain` (BigDecimal, LocalDate, sem Spring, sem JPA, sem Jackson).
3. Caso de uso em `application`, transação aqui, `clienteId` vindo do token.
4. Adaptador em `infrastructure`. DTO de borda não é entidade.
5. Migração Flyway nova se houver tabela. Não editar migração já aplicada.
6. Teste de domínio e de caso de uso (Mockito nas portas).
7. Pedidos novos no Postman e no Bruno, caminho feliz e cada erro aplicável.
8. `mvn verify`: JaCoCo ≥ 90% de instrução, ArchUnit e PMD limpos.
9. Package Checker (Mend.io) sem CVE crítica ou alta. SonarQube for IDE sem issue nova de severidade alta.

Pacotes: `br.com.fincontrol.domain`, `application`, `infrastructure.in.web`, `infrastructure.out.persistence`, `infrastructure.out.security`, `infrastructure.out.external`. Adaptador não chama outro adaptador.

Erro HTTP só pelo `ApiExceptionHandler`, em `application/problem+json`, com `codigoErro`.

## Fase 0 — Fundação

Entregar o módulo `backend`, o gerador OpenAPI, o handler de erro, JaCoCo 0.8.16, ArchUnit `archunit-junit6` 1.5.1 e PMD 7. Subir o compose.

ArchUnit: `onionArchitecture()` com domínio sem `org.springframework`, `jakarta.persistence` e HTTP; application sem infrastructure; adaptadores isolados entre si; domínio sem `double`/`Double`/`float`.

PMD no `verify`: complexidade ciclomática ≤ 10, cognitiva ≤ 15, método ≤ 40 linhas, sem classe deus, sem variável local morta.

Aceite: `docker compose -f infra/docker-compose.yml up -d` saudável e `mvn verify` verde sem Docker.

## Fase 1 — Login, JWT e clientes

Contrato `auth.yaml` e `master.yaml`. Rotas:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/refresh_token`
- `GET /api/v1/auth/me`

JWT RS256 em `infrastructure/out/security`. Chaves só em `JWT_PRIVATE_KEY_PEM` e `JWT_PUBLIC_KEY_PEM`. Claims e nenhuma outra: `iss=fincontrol`, `sub` (id do usuário), `role` (`MASTER`, `ADMIN`, `COLABORADOR`, `LEITOR`), `clienteId` (ausente no Master), `jti` (UUID novo a cada emissão), `iat`, `nbf`, `exp` (+15 min). O filtro cria uma autoridade `ROLE_<role>`.

Refresh não é JWT: 256 bits, hash SHA-256, cookie `refresh_token` `HttpOnly` `Secure` `SameSite=Strict`, path `/api/v1/auth`, 7 dias. Corpo do login e do refresh: `accessToken`, `tokenType`, `expiresIn` (900). Logout revoga o cookie e grava o `jti` em `access_token_revogado` até o `exp`. Refresh gira o cookie, emite outro `jti` e revoga o access anterior.

Master cria cliente e o primeiro administrador. Dois clientes de prova, cada um com o próprio e-mail. Senha Argon2id. Login inválido, e-mail inexistente, usuário ou cliente inativo: o mesmo 401 `NAO_AUTENTICADO`. Após várias falhas, 429 `LOGIN_BLOQUEADO`.

Coleção da seção 15 do plano, inclusive decodificar o payload e exigir as claims. Teste do adaptador usa par de chaves só na memória.

## Fase 2 — Categorias e contas

CRUD por cliente. Ao criar o cliente, gravar as categorias padrão, com Salário, Rendimentos e Benefícios. Conta: banco, tipo (`CORRENTE`, `POUPANCA`, `CARTAO_CREDITO`, `BENEFICIO`), saldo inicial. `BENEFICIO` não tem limite. No cliente de prova do usuário, criar a conta Flash além do Nubank. No cliente da mãe, não criar Flash. Leitor não escreve. Id de outro cliente responde 404.

## Fase 3 — Lançamentos

Criar, editar, estornar (motivo obrigatório), excluir só o que não está estornado, parcelas (1 a 60, resto de centavo na primeira, dia 31 no último dia do mês curto). Tipo de despesa só `FIXA` ou `VARIAVEL`. Fechar e reabrir mês: escrita com data no mês fechado responde 409 `MES_FECHADO`. Saldo numa consulta, sem estornado. `Idempotency-Key` por cliente: mesma chave e mesmo corpo devolve a mesma resposta; corpo diferente, 409.

## Fase 4 — Transferência, orçamento, meta, recorrência

Transferência entre duas contas do mesmo cliente, atômica, fora do total de receitas e despesas. Orçamento por categoria e mês; o alerta de 80% é calculado no backend. Meta e aporte. Job diário de recorrência, idempotente por `recorrencia_id` + mês. Pausa não gera retroativo ao retomar.

No cliente do usuário, uma recorrência na conta Flash gera no último dia do mês a receita Benefícios de R$ 1.570,00. Esse valor não cai no Nubank. Comida (Alimentação), abastecimento (Transporte) e qualquer outro gasto aceito no cartão flex são despesas lançadas na conta Flash. O job não inventa esses gastos. O saldo disponível da Flash é o crédito do benefício menos as despesas dela. O saldo do Nubank não entra nessa conta. Se o depósito vier entre R$ 1.520,00 e R$ 1.620,00, ajusta-se o lançamento do mês.

## Fase 5 — OFX

Parser em `infrastructure/out/external` para OFX SGML 1.x e XML 2.x, de qualquer banco. Data = oito primeiros dígitos de `DTPOSTED`. Sem `FITID`, o item não grava e volta como aviso. Dedup em (`cliente_id`, `conta_id`, `fitid`), `fitid VARCHAR(255)`. Arquivo de outro banco que a conta escolhida: 422 e nada gravado. Arquivo que não é OFX: 422.

`regra_classificacao` é dado do cliente. A regra de salário (conta Itaú de origem, dia 15 em R$ 4.800,00, dia 28 ao dia 2 entre R$ 3.750,00 e R$ 3.850,00) entra só no cliente de prova do usuário, não no da mãe. `<BALLIST>` de rendimento, quando existir, vira receita com chave `rendimento:{conta}:{início}:{fim}`. `<LEDGERBAL>` volta na resposta e não altera saldo.

Prova: `NU_7224833363_01OUT2026_22OUT2026.ofx` — 38 movimentos, o crédito de R$ 4.800,00 em 15/10 como Salário, o de R$ 1.200,00 sem essa regra, segunda importação com zero itens novos. Esse arquivo não contém o benefício Flash nem compra de comida ou combustível paga com ele.

## Fase 6 — Conciliação e comprovante

Sugestão: mesma conta, valor exato, data até 5 dias. Conciliar e desfazer gravam os dois lados na mesma transação. Par único. Anexo PNG, JPEG ou PDF até 1 MB, `storage_key` fora do banco. Mês fechado bloqueia o anexo (409). Acima de 1 MB: 413.

## Fase 7 — Auditoria

Aspecto na mesma transação da escrita. CREATE grava o objeto, UPDATE o diff, DELETE o removido. Snapshot de e-mail e perfil. Mascarar senha, hash e token. Consulta só em rota do Master, paginação por cursor. Análise por IA não grava linha. Falha de negócio não deixa registro de sucesso.

## Fase 8 — Análise por IA

Porta `AnaliseFinanceiraPort`. O adaptador é o único que importa `ChatClient`. Entrada: agregado do mês, sem lista completa e sem `ACCTID`, documento ou comprovante. Saída: record com resumo, insights e ações. Provedor em `application.yml`: `ollama`, `anthropic`, `openai` (com `base-url`) ou `none`. `none` e provedor fora: 503. Schema inválido depois de uma nova tentativa: 422 `IA_RESPOSTA_INVALIDA`. Teste mocka `ChatModel`. Prompt versionado em `resources/prompts/analise-financeira.st`.

## Coleções

`contracts/postman/FinControl.postman_collection.json` e `contracts/bruno/fincontrol/`. Um pedido por cenário, nome `operationId` + status. Todo erro aplicável entra: 400, 401, 403, 404 do próprio cliente e do outro, 409, 413, 422, 429, 503. Senha do seed só no ambiente local, fora do Git. Conferir `codigoErro`, `problem+json` e ausência de senha no corpo.

```bash
newman run contracts/postman/FinControl.postman_collection.json \
  -e contracts/postman/local.postman_environment.json --reporters cli,junit --bail
bru run contracts/bruno/fincontrol --env local
```

## Não fazer

- Testcontainers, H2, ou `mvn test` conectado no MySQL.
- `double` para dinheiro, data de negócio como `String` no domínio.
- Regra no controller ou no repositório.
- Chave JWT, senha ou token no arquivo versionado.
- QIF, CSV, fatura de cartão linha a linha, MongoDB, rateio.
- Ollama dentro do compose.
