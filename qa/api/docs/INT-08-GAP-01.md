# INT-08 — Limite de 100 itens do pedido versus 50 IDs do catálogo

**Origem:** `requirements.md` (pedido 1–100 itens); `api.md` (catálogo até 50 IDs); `architecture.md` (uma chamada normal por pedido; ADR-010). O comportamento para 51–100 IDs **não está definido**.

## Técnica / caso de teste

**Análise de valor limite (AVL)** com quatro partições: 50, 51, 100 e 101 `productId` distintos, todos com quantidade 1. **Partição de equivalência (PE):** IDs inexistentes mas sintaticamente válidos. Os IDs seguem `QA-NOT-SEEDED-000..100`, não exigem criação ou alteração de massa no MongoDB.

| ID de variação | Itens | Resultado proposto | Base |
|---|---:|---|---|
| INT-08-A | 50 | 422 `PRODUCT_NOT_FOUND` | Até o limite do lote; ids inexistentes |
| INT-08-B | 51 | 422 `PRODUCT_NOT_FOUND` | Regra proposta: erro de domínio, não 500/503 por incompatibilidade entre limites |
| INT-08-C | 100 | 422 `PRODUCT_NOT_FOUND` | Máximo de itens declarado como válido estruturalmente |
| INT-08-D | 101 | 400 `VALIDATION_ERROR` | Excede o máximo permitido para pedido |

**Resultado esperado proposto para 51 e 100:** `422 PRODUCT_NOT_FOUND` porque todos os IDs são inexistentes. Isso é uma decisão de contrato a homologar, **não** um comportamento já garantido pela documentação. O teste pode falhar contra a implementação atual (falha útil para expor GAP-01). O caso não comprova que um pedido com 100 produtos válidos possa ser criado.

**Cobertura:** status HTTP, código de erro padronizado, `requestId`, e ausência de header `Location` no erro. A ausência de registros no banco **não** é comprovada diretamente pela API; para comprovar atomicidade, é necessário teste de persistência com instrumentação/acesso ao banco.

**Restrição de negócio pendente:** escolher entre (a) limitar pedidos a 50 produtos distintos, mantendo catálogo em lote 50 e uma chamada; (b) permitir divisão em duas chamadas para 51–100, alterando ADR-010; ou (c) ampliar capacidade do lote do catálogo para 100 e manter uma chamada. Atualizar contratos e testes após decisão.

## Execução

```bash
mvn -Dtest=OrderCatalogBatchLimitApiTest test
```

Requer apenas os serviços ativos nas URLs configuradas pelo projeto. Não executar contra produção, embora os pedidos deste teste devam ser rejeitados.
