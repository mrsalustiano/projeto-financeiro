# Sistema Financeiro - Frontend Protótipo HTML/JS

## Visão Geral
Protótipo navegável do sistema de controle financeiro desenvolvido como base para a Fase 2 (Micro Frontends/Nx). Utiliza tecnologias modernas e é uma referência sólida para integração futura.

## Stack Tecnológica
- **HTML5** - Estrutura semântica
- **JavaScript Vanilla (ES6+)** - Lógica zero dependências
- **Tailwind CSS** - Design System via CDN
- **Chart.js v4** - Gráficos e visualizações
- **Font Awesome 6** - Ícones modernos

## Estrutura de Navegação

### Páginas Principais
1. **Dashboard** - Visão geral com KPIs, gráficos e transações recentes
2. **Receitas** - Cadastro e gestão de receitas financeiras
3. **Despesas** - Cadastro, filtros e sugestões de economia via IA
4. **Categorias** - Gerenciamento completo de categorias (receita/despesa)
5. **Contas correntes** - Saldos atuais e do mês anterior, com movimentações
6. **Importação Bancária** - Importação local de arquivos OFX, QIF e CSV
7. **Análise por IA** - Análises presuntivas, comparativas e comportamentais mockadas
8. **Usuários** - Gerenciamento RBAC (Administrador, Colaborador, Leitor)
9. **Configurações** - Configuração do sistema, segurança e backup

## Novidades (versão 10 do protótipo)
- **Categorias novas (6)**: Fatura do cartão, Transferências e Pix, Investimentos, Vestuário, Pets (despesas) e Outras receitas (receita). **"Freelance" saiu** (substituída por "Rendimentos" no id 2, que recebe Pix recebido, cashback e afins); "Financiamento" virou **"Financiamentos e seguros"** e "Assinaturas e serviços" virou **"Assinaturas, serviços e taxas"** — clientes salvos antes são migrados e rebatizados automaticamente. A categorização automática manda agora Pix enviado para "Transferências e Pix", pagamento de fatura para "Fatura do cartão", e reconhece pets, vestuário, investimentos e rendimentos.
- **Modelos rápidos de recorrência** para despesas fixas de longo prazo: 🎓 Faculdade (48 meses), 🗣️ Inglês (24), 🚗 Financiamento de veículo (48), 🏦 Acordo com banco (24), 💳 Acordo de cartão (18), 🤖 Assinatura de IA, 🌐 Internet e 📱 Plano do celular (esses três sem mês final — recorrente contínua, desative quando encerrar). Cada modelo preenche tipo, descrição, categoria, dia, valor sugerido e mês final calculado; você ajusta e salva.
- **Tela unificada "Lançamentos"** no lugar dos menus separados de Receitas e Despesas: formulário único com seletor de tipo (que troca categorias e mostra parcelas só para despesa), tabela única com coluna de tipo, categoria, conta e valor colorido (+ verde / − vermelho), contador de lançamentos e saldo do filtro no topo.
- **Filtros na tela Lançamentos**: além de tipo e conta, agora filtra por **Categoria** (só lista as categorias em uso no período) e por **intervalo de data** (De / Até, combinando com o seletor de mês). Botão "Limpar filtros" restaura tudo. O contador e o saldo do topo acompanham qualquer combinação de filtro.
- **"Salvar todos categorizados"**: no fim da lista de conciliação, um botão lança de uma vez só os itens pendentes que já têm categoria. Os itens sem categoria continuam na lista para edição. A confirmação mostra o resumo por tipo, e a auditoria marca o lote como "só categorizados".
- **Cores na conciliação**: linha verde-claro com badge "categorizado" para itens com categoria sugerida automaticamente (com "(sugerida)" ao lado da categoria), amarela para itens sem categoria, azul durante a edição e branca para os editados manualmente.
- **Botão "Salvar" por item**: itens sem candidato de conciliação ganham um botão Salvar que lança aquele item individualmente, já conciliado e com a categoria sugerida. Itens com candidato continuam com o select de conciliação.
- **Auto-categorização na importação**: ao importar um extrato, cada item já recebe uma categoria sugerida por regras de palavra-chave na descrição (Alimentação, Transporte, Moradia, Lazer, Saúde, Educação, Salário, Rendimentos, Fatura do cartão, Transferências e Pix, Investimentos, Vestuário, Pets). Com o OFX do Nubank, 32 de 38 itens saem categorizados. O botão "Recategorizar automaticamente" reprocessa os pendentes; cada item continua editável individualmente.
- **"Lançar todos os pendentes"**: um botão lança de uma vez todos os itens pendentes do extrato (receitas e despesas), com confirmação mostrando o resumo por tipo e avisando quantos ficarão sem categoria. Cada lançamento já nasce conciliado com o item; itens de mês fechado são recusados individualmente.
- **Correção de importação (2)**: a lista de "Conciliação do extrato" ficava desatualizada logo após importar — só aparecia depois de recarregar a página. Agora ela é re-renderizada na hora, junto com a tabela de lançamentos importados.
- **Correção de importação**: carregar um extrato OFX (ex.: Nubank) falhava com "formato is not defined" ao registrar a auditoria. O formato agora é identificado na importação e gravado corretamente.
- **Filtros na tela Lançamentos**: além de tipo e conta, agora filtra por **Categoria** (só lista as categorias em uso no período) e por **intervalo de data** (De / Até, combinando com o seletor de mês). Botão "Limpar filtros" restaura tudo. O contador e o saldo do topo acompanham qualquer combinação de filtro.
- As telas antigas de Receitas e Despesas continuam no código, ocultas, para não quebrar a lógica interna e os testes.
- **Conciliação com contador**: "X itens importados · Y pendentes · Z conciliados" no topo da lista, e texto do critério de sugestão reescrito (mesma conta, valor exatamente igual, data até 5 dias de diferença).
- **Banner de protótipo** no topo da aplicação, lembrando que os dados ficam apenas no navegador.

## Novidades (versão 9 do protótipo)
- **Botão "Setar saldo"** em cada conta corrente e poupança, na tela Contas correntes: abre um **modal** com o saldo atual preenchido. O valor informado passa a valer como ponto de partida na data do ajuste; lançamentos anteriores a essa data deixam de ser somados e os posteriores continuam entrando no saldo.
- O cartão mostra "Saldo ajustado em dd/mm/aaaa" e ganha o link **Limpar ajuste**, que volta ao cálculo apenas pelos lançamentos.
- **Botão "Setar limite"** em cada cartão de crédito: abre o mesmo modal, já com o limite atual, e recalcula o disponível (limite menos fatura em aberto).
- O modal fecha com Cancelar, Esc ou clique fora da caixa; valor inválido mostra o erro dentro do próprio modal, sem `alert`.
- O ajuste entra na auditoria como UPDATE da conta, com o valor anterior e o novo. Mês fechado bloqueia o ajuste; cartões de crédito não têm o botão (neles o valor é a fatura em aberto).

- **Paginação nas tabelas**: 20 registros por página, com o rodapé "Mostrando X–Y de Z" e botões Anterior/Próxima. Aplica a Lançamentos, Receitas, Despesas, Lançamentos importados (importação), transações da conta, transações recentes do dashboard, **Auditoria** e usuários. Tabela com 20 ou menos não mostra o pager. Na auditoria, mudar filtro ou "Limpar filtros" volta para a página 1, e o contador acima da tabela segue mostrando o total geral do filtro.

## Novidades (versão 8 do protótipo)
- **Edição de cada item importado** na Conciliação do extrato: botão "Editar" abre um formulário no próprio item com classificação (Receita/Despesa), conta ou cartão de crédito, categoria, descrição, valor e data.
- Duas ações: **Salvar e lançar** (padrão, botão principal) corrige o item, cria o lançamento na conta ou cartão escolhido e já concilia os dois; **Salvar sem lançar** só corrige o item do extrato.
- Depois de "Salvar e lançar", a tela **permanece na Importação bancária**: a lista de conciliação é atualizada no lugar, o mês selecionado não muda e uma mensagem confirma o lançamento em Receitas ou Despesas. O lançamento é criado com a data do item, então pode ficar em outro mês; para vê-lo, troque o período no seletor do cabeçalho.
- Cada item mostra a classificação atual (Receita, Despesa ou Despesa · Cartão de crédito), a conta, a categoria e o status da conciliação.
- Item já conciliado precisa ter a conciliação desfeita antes de ser editado; mês fechado bloqueia "Salvar e lançar".
- Item conciliado deixa de contar duas vezes no saldo da conta.

## Novidades (versão 7 do protótipo)
- **Tela de auditoria de logs** na área do Admin Master, com filtros por período (de/até), usuário, cliente, ação (CREATE/UPDATE/DELETE) e entidade, além de exportação CSV.
- Trilha **append-only** e global: registra criação, alteração e exclusão de clientes, administradores, usuários, categorias, contas, lançamentos, transferências, recorrências, orçamentos, metas, importação, fechamento de mês, estorno, conciliação, anexo e configurações. A análise por IA não é registrada.
- **Dados mockados**: 27 registros de exemplo, com datas nos últimos 42 dias, quatro usuários (Master, Administrador, Colaborador e Leitor), dois clientes e ações globais, para a tela abrir preenchida e os filtros terem o que mostrar. A trilha de exemplo é substituída pelos registros reais assim que a primeira escrita acontece.
- Cada registro guarda data/hora, usuário, perfil, cliente, ação, entidade, identificador e o diff (`antes`/`depois`), com senha e tokens mascarados como `***`.
- Nenhum perfil de cliente acessa a auditoria: Administrador, Colaborador e Leitor são barrados na rota e na função.
- Fechamento de mês continua bloqueando anexar e remover comprovante, porque altera o lançamento.

## Novidades (versão 6 do protótipo)
- **Estorno/reversão de lançamento**: botão "Estornar" pede o motivo, marca o lançamento com data e autor, risca a linha e exclui o valor dos totais. Lançamento estornado não pode ser excluído nem estornado de novo.
- **Comprovante por lançamento**: anexo de PNG, JPEG ou PDF até 1 MB, com "Ver" e "Remover" na própria linha.
- **Conciliação do extrato**: na Importação bancária, cada item importado é vinculado a um lançamento da mesma conta, com o mesmo valor e data até 5 dias de diferença; permite desfazer.
- **Fechamento de mês**: em Configurações, fechar ou reabrir cada um dos últimos 12 meses. Mês fechado bloqueia lançamento, receita, transferência, estorno, anexo e exclusão com data nele, mostra banner no dashboard e marca "(fechado)" no seletor de período.

## Novidades (versão 5 do protótipo)
- **Admin Master** (`master@fincontrol.com` / `master123`): área própria "Clientes e administradores" para cadastrar clientes e o primeiro administrador de cada um, criar administradores adicionais, redefinir senha, inativar e excluir. O Master **não** acessa os dados financeiros dos clientes.
- **Multi-cliente com dados isolados**: cada cliente tem categorias, lançamentos, contas, orçamentos, metas e usuários próprios. O cliente novo começa vazio (com as categorias padrão e uma conta principal).
- **Login por e-mail e senha** (substitui a lista de usuários), com bloqueio de cliente inativo e sessão restaurada ao recarregar.
- **Perfis mantidos**: Administrador, Colaborador e Leitor continuam iguais, agora dentro de cada cliente. Cada cliente precisa manter ao menos um administrador ativo.
- Contas antigas salvas no navegador são migradas para o cliente "Família Silva"; o Admin Master é criado automaticamente se não existir.
- Botão "Entrar" em azul com texto branco (`.btn-blue`), visível nos dois temas.

## Novidades (versão 4 do protótipo)
- **Contas correntes por banco**: contas agrupadas por banco, com tipo (conta corrente, poupança ou cartão de crédito). Dados de exemplo: Nubank, Itaú e Santander (contas) e cartões Nubank, Amazon e Banco PAN.
- **Filtro por banco ou por conta/cartão** na tela de contas; ao escolher um banco aparecem conta e cartão dele juntos, com a posição líquida.
- **Cartão de crédito**: compras formam a fatura em aberto, com limite e valor disponível; botão "Pagar fatura" preenche a transferência a partir de uma conta.
- Cadastro de novos bancos/contas/cartões (com sugestão de bancos conhecidos); contas salvas em versões antigas são migradas automaticamente.

## Novidades (versão 3 do protótipo)
- **Login simulado com 3 perfis** (todos com acesso aos dados do cliente "Família Silva"). Senhas iniciais: Administrador `admin@fincontrol.com` / `admin123`, João Silva (Colaborador) `joao@exemplo.com` / `colab123`, Maria Oliveira (Leitor) `maria@exemplo.com` / `leitor123`. Na versão 5 o login passou a ser por e-mail e senha. É apenas demonstração no navegador: não oferece segurança real; a proteção de verdade virá do backend (JWT + RBAC).
  - **Administrador:** tudo, incluindo Usuários e Configurações.
  - **Colaborador:** consulta e lança/edita/exclui dados e importa extratos.
  - **Leitor:** somente consulta (dashboard, listas, contas, IA, exportação CSV).
- **CRUD de usuários** (só Administrador): criar, editar, excluir, ativar/inativar, trocar perfil e senha; e-mail único; sempre mantém ao menos um administrador ativo; ninguém exclui ou inativa a si mesmo.
- **Lançamentos recorrentes** (página "Recorrências e parcelas"): gerados mês a mês até o mês atual, com pausa/retomada.
- **Lançamentos parcelados**: campo "Parcelas" no cadastro de despesa (valor total dividido, centavos na 1ª parcela, dia ajustado em meses curtos) e painel de compras parceladas.
- **Várias contas e transferências entre contas**: criar/excluir contas, saldo por conta, filtro por conta e transferências (débito na origem, crédito no destino, sem afetar receitas/despesas).
- **Tema escuro** (botão no cabeçalho, lembrado no navegador).

## Novidades (versão 2 do protótipo)
- **Filtro de período** (mês) no cabeçalho: afeta dashboard e orçamentos
- **Cadastro real** de receitas e despesas (com tipo fixa/variável/investimento) e **exclusão** de lançamentos
- **Orçamentos e metas** (nova página): limite mensal por categoria com barra de progresso, alertas no dashboard a partir de 80% do limite e metas de economia com aportes
- **Evolução dos últimos 6 meses** (gráfico de linha) no dashboard
- **Exportação CSV** dos lançamentos da conta (reimportável na Importação bancária)
- **Persistência em `localStorage`** (dados ficam no navegador) e botão para restaurar os dados de demonstração em Configurações
- Correções: KPI de média por categoria, totais por mês, importação OFX em formato SGML, escape de HTML nas tabelas, nomes dos métodos em `mock-api.js`
- Observação: `mock-api.js` ainda não é carregado por `index.html`; serve de base para a integração futura com o backend

## Funcionalidades Implementadas

### Dashboard
- Cards de KPI em tempo real (Receitas, Despesas, Saldo Mensal, Média por Categoria)
- Gráfico de barras: Receita vs Despesa
- Tabela de transações recentes (top 5)
- Filtros mensais/anuais para todas as seções

### Gestão Financeira
- Formulários completos de cadastro de receitas/despesas
- Seleção dinâmica de categorias por tipo
- Filtros em tempo real (busca, mês atual, ano atual)
- Tabela completa com visualização colorida por categoria

### Categorias
- Gerenciamento CRUD completo
- Atributos: Tipo, Código, Nome, Cor personalizada
- Visual cards intuitivos com ícones de cor

### Contas Correntes
- Saldo atual e saldo acumulado até o fim do mês anterior
- Movimentações baseadas nos lançamentos simulados e extratos importados
- Saldos mockados assumem saldo inicial zero e ficam em memória durante a sessão

### Importação Bancária
- **Suporte a formatos:** OFX, QIF e CSV
- CSV esperado com colunas `data`, `descricao` e `valor`; valores negativos representam débitos
- Drag & Drop interface moderno
- Resumo das transações importadas e atualização dos saldos mockados

### Spring AI Integration
- Consulta local mockada por IA financeira; não envia dados a um serviço externo
- Modelos disponíveis: `llm-financial-v1`, `vector-memories`
- Tipos de análise: Presuntiva, Comparativa (Ano/Mês), Comportamental, Econômica
- Sugestões simuladas baseadas nos dados financeiros de demonstração
- Confiança simulada exibida em cada resposta

### Usuários e RBAC
- Controle de acesso com 3 níveis: Administrador, Colaborador, Leitor
- Stats em tempo real (total, ativos, administradores)
- Tabela completa com status e última conexão
- Configurações de segurança do sistema

## Dados Simulados (Mock Data)
O protótipo utiliza dados mock para demonstração. Em produção:

```javascript
// Dados carregados do Backend (Spring Boot)
const receitas = [];        // Array de {id, valor, descricao, categoriaId, data}
const despesas = [];       // Array de {id, valor, descricao, categoriaId, tipo, data}
const categorias = [];     // Array de {id, tipo, codigo, nome, cor, observacoes}
const usuarios = [];       // Array com RBAC
```

## Como Utilizar

### 1. Abrir no Navegador
```bash
# Opção 1: Arquivo direto (mais simples)
start frontend\index.html

# Opção 2: Via VS Code Live Server
# Instalar extensão "Live Server" e clicar Right-Click → Open with Live Server
```

### 2. Para Desenvolvimento com Backend
Configurar servidor local e API endpoints:

```bash
# Iniciar Backend Spring Boot (backend-phase-1)
cd backend && mvn spring-boot:run

# API Endpoints Esperados
GET    /api/financeiro/receitas           # List receitas
POST   /api/financeiro/receitas           # Criar receita
GET    /api/financeiro/despesas           # List despesas
POST   /api/financeiro/despesas           # Criar despesa
GET    /api/financeiro/categorias         # List categorias
POST   /api/financeiro/categorias         # Criar categoria
GET    /api/ai/analise/presuntiva        # AI análise presuntiva (Spring AI)
```

## Integração com Backend Spring Boot

### Arquitetura Hexagonal
O frontend consumirá contratos OpenAPI definidos em `backend/openapi-contract.json`:

```yaml
# Exemplo de endpoint API (Spring AI)
GET /ai/analise {
  request: { prompt: string, tipo: enum },
  response: { 
    content: string, 
    sugestoes: [object],
    confianza: number
  }
}
```

### Segurança RBAC
- JWT tokens no Header `Authorization`
- Endpoints protegidos por role
- Isolamento de dados por `userId` em todas as camadas

## Próximos Passos (Fase 2)

1. **Micro Frontends Nx** - Migrar para estrutura monorepo
2. **Angular 18+** - Refatorar para Signals/Angular components
3. **Design System** - Extrair Tailwind config para `@nx/design-system`
4. **Testes E2E** - Playwright/Cypress para fluxos críticos

## Arquitetura do Frontend (Atual)

```
frontend/
├── index.html          # Single HTML page (protótipo atual)
└── assets/             # Future static files
    └── css/
        ├── tailwind.css  # Tailwind config
        └── custom.css
    └── js/
        ├── app.js        # Core state management
        ├── charts.js     # Chart.js logic
        └── ai-spring.js  # Spring AI client wrapper
```

## Tecnologias Recomendadas para Migração

| Atual | Fase 2 (Meta) |
|-------|--------------|
| Vanilla JS | TypeScript |
| HTML Single Page | Nx React/Micro Frontends |
| Chart.js CDN | Nx Module + Custom Charts |
| Tailwind via CDN | Nx Plugin: `@nx/tailwindcss` |
| Mock Data | Spring Boot APIs (JWT Auth) |

## Regras do Projeto (Referência)

✅ **Pureza do Domínio:** Frontend nunca deve conter lógica de negócio crítica  
✅ **Segurança:** RBAC em todas as páginas, JWT no Header  
✅ **Qualidade:** Teste mínimo 90% coberto em Fase 2  
✅ **Encoding:** UTF-8 with LF  

## Contribuição

Para adicionar novas features ou visualizar melhor o protótipo:
1. Fork do repositório
2. Criar branch `feature/novo-feature`
3. Implementar e testar localmente
4. Abra PR para revisão

---

**🤖 Gerado por [Claude Code](https://claude.com/claude-code) como parte da Fase 1 → 2 migration**
