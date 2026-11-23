# Como chamar a skill do backend

A skill `fincontrol-backend` implementa o backend uma fase por vez, seguindo `docs/plano-backend.md`. Ela mora em `.agents/skills/fincontrol-backend/SKILL.md`.

No chat, cite o nome da skill e o número da fase. Uma mensagem implementa uma fase e para no aceite. A fase seguinte só começa depois que você confirmar.

## Antes da fase 0

O Docker roda no WSL. No terminal da distro, na pasta do projeto (`/mnt/c/Users/mrsal/Documents/projeto-financeiro`):

```bash
cp infra/.env.example infra/.env
docker compose -f infra/docker-compose.yml up -d
```

Troque as senhas em `infra/.env` antes de subir. O MySQL fica em `localhost:3306` (Windows e WSL). O Portainer abre em `https://localhost:9443` e administra o Docker do WSL pelo socket `/var/run/docker.sock`.

## O que escrever no chat

| Fase | Mensagem |
|---|---|
| 0 Fundação | Implemente a fase 0 com a skill fincontrol-backend. Suba o MySQL e o Portainer e pare no aceite. |
| 1 Login e JWT | Implemente a fase 1 com a skill fincontrol-backend. Quero login, logout, refresh_token e me, com JWT RS256, role, jti e as demais claims do plano. |
| 2 Categorias e contas | Implemente a fase 2 com a skill fincontrol-backend. Inclua a conta Flash do tipo benefício no meu cliente. |
| 3 Lançamentos | Implemente a fase 3 com a skill fincontrol-backend. |
| 4 Transferência e recorrência | Implemente a fase 4 com a skill fincontrol-backend. Inclua o depósito mensal de R$ 1.570,00 na Flash e os gastos de comida e abastecimento nessa conta. |
| 5 OFX | Implemente a fase 5 com a skill fincontrol-backend. Use o OFX do Nubank como prova e não misture o benefício da Flash. |
| 6 Conciliação | Implemente a fase 6 com a skill fincontrol-backend. |
| 7 Auditoria | Implemente a fase 7 com a skill fincontrol-backend. |
| 8 Análise por IA | Implemente a fase 8 com a skill fincontrol-backend. O provedor fica no application.yml: ollama, anthropic, openai ou none. |

Para corrigir algo já entregue, sem abrir a fase seguinte:

```text
Na fase 1, com a skill fincontrol-backend, ajuste só o refresh_token. Não comece a fase 2.
```

## O que a skill faz em toda fase

1. Atualiza o OpenAPI antes do código.
2. Coloca a regra no domínio, o caso de uso na aplicação e o adaptador na infraestrutura.
3. Cria a migração Flyway se nascer tabela.
4. Cobre com teste de domínio e de caso de uso, sem Testcontainers.
5. Acrescenta os pedidos no Postman e no Bruno, com sucesso e erro.
6. Publica o contrato novo no Swagger, em `/swagger-ui.html`.
7. Roda `mvn verify` (JaCoCo em 90%, ArchUnit e PMD).

Quando a fase fecha, o chat devolve o aceite e para. Você responde se segue ou se algo precisa mudar.

## O que não chamar junto

Não peça duas fases na mesma mensagem. Não peça a tela Angular enquanto o backend daquela fase não estiver aceito. Ollama não entra no Docker Compose.
