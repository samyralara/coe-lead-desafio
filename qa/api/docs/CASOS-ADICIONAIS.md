# Back-end — 21 casos remanescentes (6 incorporados, 15 não implementados)

Referências: `Matriz_Casos_Teste_Otimizada_PE_AVL.xlsx` (`Casos sintetizados`), `requirements.md`, `api.md`, `architecture.md`.

## Interpretação da restrição sobre massas

- **Nenhum teste novo exige inserir/alterar produtos, preços, janelas de preço ou regras de desconto.**
- Os testes **reutilizam** `PROD-001` e `PROD-002` e, quando informados, IDs de produtos que *já existam* no ambiente.
- O ato de testar `POST /api/v1/orders` **naturalmente cria pedidos de teste**. Isso é exercício da funcionalidade, e não a criação de novas *massas de catálogo*. Por isso, rode em QA, nunca em produção.
- Não são criados mocks de catálogo nem modificado o relógio/bancos para forçar falhas. Se um requisito não for inteiramente observável via REST, o teste é marcado **PARCIAL**, sem declarar cobertura integral.

## Casos adicionais automatizados

| ID | Classe / método | Técnica | Fonte de dados | Oráculo / cobertura | Situação |
|---|---|---|---|---|---|
| `FIN-03` | `OrderFinancialAdditionalApiTest#fin03_composicaoMonetariaDePedido` | PE, composição | PROD-001, PROD-002 | Confere preços, quantidade, linhas, subtotal, desconto HALF_UP, total e leitura posterior | **Integral** (com seed de referência) |
| `ORD-05` | `OrderListingAdditionalApiTest#ord05_ordenacaoPorCriacaoDecrescente` | PE, ordenação | Pedidos já disponíveis | Timestamps não crescentes entre itens e páginas; se houver menos de dois pedidos, teste fica `skipped` | **Integral quando >= 2 registros** |
| `CAT-02` | `CatalogValidityAdditionalApiTest#cat02_precoVigenteComJanelaCoerente` | AVL temporal | PROD-001, PROD-002 | currentPrice informado corresponde a janela coerente na consulta, inclusive `validTo=null` | **Parcial**: não executa fronteiras temporais exatas |
| `INT-02` | `OrderCatalogRejectionAdditionalApiTest#int02_propagarFalhasSemanticasConhecidas` | PE semântica | PROD-001 + ID inexistente; IDs especiais preexistentes opcionais | notFound no catálogo corresponde a 422 no pedido; valida detalhes; inativo/sem preço quando IDs já disponíveis | **Parcial** sem IDs especiais |
| `INT-07` | `ConcurrentOrdersAdditionalApiTest#int07_criacaoConcorrenteSemContaminacao` | PE, concorrência | PROD-001; 3 requisições | 3 pedidos com IDs, clientes, quantidades e cálculos segregados, verificados por GET | **Integral funcional**, sem métrica de SLA |
| `DB-01` | `OrderPersistenceObservableAdditionalApiTest#db01_persistenciaObservavelPelaApi` | PE sucesso/falha | PROD-001, PROD-002 + ID inexistente | Criação legível após GET; erro com item inválido e totalElements inalterado | **Parcial**: não verifica órfãos diretamente nas tabelas |

> `ORD-05` não gera dados artificialmente para satisfazer sua pré-condição. `DB-01` e `PED-10` usam `totalElements` como indício de ausência de pedido após erro: essa comparação exige ambiente sem outras escritas simultâneas e **não comprova rollback dos `order_items`**. Os testes JUnit da suíte rodam sequencialmente por padrão.

## Casos remanescentes NÃO automatizados

Os 15 seguintes foram descartados nesta entrega por exigir alteração de massas, instrumentação especial, injeção de falhas ou decisão contratual pendente.

| ID | Motivo concreto | Condição para viabilizar posteriormente |
|---|---|---|
| `FIN-04` | Exige desativar/alterar as regras de desconto | Banco/configuração de QA controlados e restauração da regra |
| `FIN-05` | Exige inserir regra inválida | Fixture inválida isolada ou mecanismo de injeção |
| `FIN-06` | Regras concorrentes requerem massa alterada; precedência é GAP-07 | Regra de precedência definida e seed próprio |
| `FIN-07` | Precisa de produto em moeda diferente de BRL; contrato é GAP-10 | Política de moeda e produto existente em outra moeda |
| `CAT-05` | Número de queries Mongo não é exposto nas APIs públicas | Métrica/log do repositório ou teste de persistência instrumentado |
| `CAT-06` | Janelas sobrepostas exigem modificar dados; GAP-06 | Fixture controlada e definição de precedência |
| `CAT-07` | Preços zero/negativos/precisão adicional exigem modificar dados; GAP-06 | Contrato de validação e fixtures apropriadas |
| `INT-01` | Uma chamada HTTP ao catálogo é detalhe interno não verificável pelo resultado REST | MockWebServer/spy do gateway ou métrica de chamadas de saída |
| `INT-03` | Retry de conexão exige controlar falhas da dependência; contagem das tentativas é GAP-03 | Proxy de falhas e configuração definida |
| `INT-04` | Timeout determinístico exige atraso artificial ou controle do ambiente | Simulador de latência com backend apontado para ele |
| `INT-05` | Erros 5xx/schema inválido do catálogo exigem falha induzida; classificação JSON é GAP-13 | Stub de catálogo com respostas controladas e contrato definido |
| `INT-08` | 51–100 produtos distintos exige massa adicional e é GAP-01 | Resolver conflito entre 100 itens e lote de 50 IDs |
| `DB-02` | Snapshot exige alterar nome/preço ou regra entre pedidos | Dados mutáveis em QA isolado e restauração |
| `DB-03` | Falha de PostgreSQL requer desligar/isolar o banco | Ambiente descartável com fault injection |
| `DB-04` | Três prepared statements e ausência de N+1 não são observáveis via API | Testes de persistência com métricas SQL/JPA |

## Como executar apenas os 6 casos novos

```bash
mvn -Dtest=OrderFinancialAdditionalApiTest,OrderListingAdditionalApiTest,CatalogValidityAdditionalApiTest,OrderCatalogRejectionAdditionalApiTest,ConcurrentOrdersAdditionalApiTest,OrderPersistenceObservableAdditionalApiTest test
```

**Resultado esperado:** os seis métodos são descobertos; `ORD-05` poderá ficar `skipped` por insuficiência de pedidos. Os demais requerem os dois serviços disponíveis e o seed de referência citado nos documentos.

Para toda a automação original + adicionada:

```bash
mvn clean test
```

**Observação sobre contagem:** 26 casos lógicos implementados (20 anteriores + 6 novos), de um catálogo de 41. São **15 não implementados**; 3 dos 6 novos possuem cobertura parcial. A cobertura real de cada execução depende do ambiente, das partições executadas e de casos `skipped`.
