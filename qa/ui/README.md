# Automação Front-end — Playwright / TypeScript (alinhada ao código-fonte)

Projeto ajustado após leitura dos componentes Angular 21 contidos em `coe-lead-desafio-master.zip`. A pasta `application/frontend` contém a cópia do front-end recebido para permitir executar o projeto sem localizar outro repositório. Não há alteração da implementação Angular.

## Pré-requisitos
- Node.js compatível com Angular 21 (Node 22.12+ recomendado); npm.
- Navegador Chromium do Playwright (`npx playwright install chromium`).
- Para o modo integrado, PostgreSQL, MongoDB e os serviços Java ativos (8080 e 8081), com seed dos produtos.

## Instalação
```bash
npm install
npm --prefix application/frontend ci
npx playwright install chromium
```

## Rodar a interface e testes com mocks
Em um terminal, na raiz deste projeto:
```bash
npm --prefix application/frontend start -- --host 127.0.0.1
```
Em outro terminal:
```bash
npm run test:mock
```
Alternativamente, no macOS/Linux: `START_APP=1 npm run test:mock` (o Playwright inicia o Angular após instalar suas dependências).

## Executar em ambiente integrado
Inicie os bancos e microsserviços conforme o README do repositório original. Com o Angular iniciado usando seu `proxy.conf.json`, execute:
```bash
npm run test:integrated
```
Esta modalidade executa `@integrated`, sem interceptar respostas HTTP. Os testes de UI isolados permanecem determinísticos via `page.route`, sem criar registros nos bancos.

## Comandos
`npm run typecheck` — compilação TypeScript da automação.
`npm run lint` — revisão estática.
`npm run test:smoke` — smoke da interface.
`npm run report` — relatório HTML.
`npx playwright test --list` — lista cenários registrados.

## Código-fonte que determinou a automação
- `app.routes.ts` e `features/orders/orders.routes.ts`: `/`, `/orders`, `/orders/new`, `/orders/:id`, `/catalog`.
- `order-form.page.html` e `.ts`: campos `#client-name`, `#client-document`, `#client-email`, `#item-product-id`, `#item-quantity`; consulta prévia por `POST /api/catalog/prices` e redirecionamento após sucesso.
- `order-list.page.html`: `ngb-pagination` 1-based convertida para página 0-based na API. **Não existe filtro visual de status.**
- `catalog-lookup.page.ts`: ID único via GET; múltiplos via POST; deduplicação de IDs e máximo 50.
- `toasts-container.component.ts` e `error-state.component.ts`: notificações `ngb-toast` e `app-error-state`.
- `dashboard.page.ts`: dados de uma página de até 100 pedidos e aviso da limitação.

## Cobertura
Ver `docs/CENARIOS.md`. Os testes são organizados por jornada, sem valores de tempo fixos. Os testes com mocks avaliam formulário, rotas, interceptadores e renderização reais, **não** comprovam persistência ou integração real entre serviços. Uma jornada integrada está em `tests/integrated.spec.ts`.

## Estado de validação
Os arquivos foram conferidos contra o código Angular anexado. Compilação/testes automatizados no ambiente de geração dependem da instalação das dependências npm e do navegador, podendo não estar disponíveis offline. Consulte os relatórios locais após instalar e executar.
