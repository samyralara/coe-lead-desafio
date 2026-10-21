# Automação de API — Pedidos e Catálogo (Java + Rest-Assured)

Projeto Maven com **26 casos de teste lógicos do back-end (20 originais + 6 adicionais)** da aplicação de gestão de pedidos e catálogo. Os casos preservam os IDs da **matriz otimizada por Partição de Equivalência (PE) e Análise de Valor Limite (AVL)**.

## 1. Tecnologias

- Java 17 (mínimo); Maven 3.9+
- Rest-Assured 5.5.7
- JUnit Jupiter 5.13.0 (testes e parametrização)
- Jackson para geração dos payloads JSON
- Maven Surefire 3.5.2 (relatórios JUnit XML e texto)

## 2. Arquitetura alvo

```text
                  +-----------------------------------+
                  | Testes Java / JUnit / Rest-Assured|
                  +------------------+----------------+
                                     |
                  +------------------+----------------+
                  |                                   |
          HTTP :8080 (direto)                 HTTP :8081 (direto)
                  |                                   |
          Order Service                         Catalog Service
                  |                                   |
             PostgreSQL                             MongoDB
                  +-------- HTTP -> Catálogo ----------+
```

**Atenção:** este repositório contém a **automação de QA**, não o código dos dois microsserviços. Para executar testes de integração reais, **os serviços precisam estar em execução** com os bancos e seeds correspondentes. Sem os repositórios/código da aplicação não é possível empacotar os serviços nem validar sua funcionalidade.

## 3. Pré-requisitos

1. Instalar Java 17+ e Maven 3.9+.
2. Subir `order-service` em `http://localhost:8080` e `catalog-service` em `http://localhost:8081`, preferencialmente pelos scripts do projeto da aplicação.
3. Confirmar `/actuator/health` em ambos (por exemplo, `curl http://localhost:8080/actuator/health`).
4. Preparar o seed de produtos (PROD-001 = R$ 3500,00; PROD-002 = R$ 499,90) e a regra de desconto ativa (R$ 1000 → 10%).
5. Executar **somente em ambiente de teste**: POST cria pedidos permanentemente.

O arquivo `.env.example` documenta variáveis, mas Maven **não lê `.env` automaticamente**. Exporte as variáveis no terminal, passe propriedades `-Dqa.*` ou use o script fornecido em `scripts/`.

## 4. Executar

### Todos os testes

```bash
mvn clean test
```

### Apenas smoke

```bash
mvn test -Dgroups=smoke
```

### Uma classe

```bash
mvn test -Dtest=OrderCreationApiTest
```

### Um caso lógico (e suas partições)

```bash
mvn test -Dtest=OrderCreationApiTest#ped04_limitesQuantidade
```

### URLs diferentes

```bash
mvn test -Dqa.orderBaseUrl=http://qa-order:8080 -Dqa.catalogBaseUrl=http://qa-catalog:8081
```

### Massas opcionais para casos financeiros e classes especiais

```bash
mvn test \
  -Dqa.inactiveProductId=SEU-ID-INATIVO \
  -Dqa.noPriceProductId=SEU-ID-SEM-PRECO \
  -Dqa.price99999Id=SEU-ID-99999 \
  -Dqa.price100000Id=SEU-ID-100000 \
  -Dqa.price100001Id=SEU-ID-100001 \
  -Dqa.price100005Id=SEU-ID-100005
```

Para PowerShell:

```powershell
$env:QA_ORDER_BASE_URL = 'http://localhost:8080'
$env:QA_CATALOG_BASE_URL = 'http://localhost:8081'
mvn clean test
```

## 5. Casos implementados

| Classe | Casos da matriz | Quantidade |
|---|---|---:|
| `OrderCreationApiTest` | PED-01 a PED-10 | 10 |
| `OrderFinancialApiTest` | FIN-01 e FIN-02 | 2 |
| `OrderQueryApiTest` | ORD-01 a ORD-04 | 4 |
| `CatalogApiTest` | CAT-01, CAT-03 e CAT-04 | 3 |
| `RequestTracingApiTest` | INT-06 | 1 |
| **Subtotal da primeira entrega** | | **20** |
| `OrderFinancialAdditionalApiTest` | FIN-03 | 1 |
| `OrderListingAdditionalApiTest` | ORD-05 | 1 |
| `CatalogValidityAdditionalApiTest` | CAT-02 (parcial) | 1 |
| `OrderCatalogRejectionAdditionalApiTest` | INT-02 (parcial) | 1 |
| `ConcurrentOrdersAdditionalApiTest` | INT-07 | 1 |
| `OrderPersistenceObservableAdditionalApiTest` | DB-01 (parcial) | 1 |
| **Total implementado** | | **26** |

**Consulte [docs/CASOS-DE-TESTE.md](docs/CASOS-DE-TESTE.md)** para os 20 originais e **[docs/CASOS-ADICIONAIS.md](docs/CASOS-ADICIONAIS.md)** para os 6 novos e 15 não implementados. A lista completa de 41 IDs está em [docs/RASTREABILIDADE_BACKEND.csv](docs/RASTREABILIDADE_BACKEND.csv).

## 6. Partições, limites e resultados

- Quantidade por item: `-1, 0, 1, 2, 998, 999, 1000` (PED-04).
- Número de itens: `0, 1, 101` (PED-03). O intervalo de 51–100 itens permanece condicionado à resolução do conflito contratual.
- Comprimento `productId`: ausente, vazio, `1, 49, 50, 51` caracteres (PED-05).
- Subtotal/desconto: `999,99`, `1.000,00`, `1.000,01`, `1.000,05` (FIN-01/02).
- Paginação: `size=0,1,100,101` (ORD-04).
- Lote de preços: `1,49,50,51` IDs e duplicados (CAT-04).
- JSON malformado, cliente inválido, produto inexistente/inativo/sem vigência, cálculo e rastreabilidade.

JUnit gera **mais de 26 invocações** porque casos com `@ParameterizedTest` executam todas as linhas de entrada. O projeto **não declara os 20 como execuções físicas**.

## 7. Testes ignorados (skipped) por massa

As entradas das variações opcionais de `PED-09` e os testes financeiros `FIN-01/FIN-02` usam `Assumptions.assumeTrue` quando os IDs adicionais não foram configurados. Isso produz **skipped com motivo explícito**, em vez de falhas por dados não encontrados ou falsos resultados positivos. Com o catálogo corretamente provisionado, todos devem executar. Os demais cenários usam endpoints e referências documentados.

## 8. Relatórios e pipeline

Após `mvn test`, consulte:

- `target/surefire-reports/TEST-*.xml` — integração com CI, número de execuções, falhas e skips.
- `target/surefire-reports/*.txt` — resultado textual por classe.

O workflow `.github/workflows/api-tests.yml` sempre compila os testes em PR. A execução da suíte contra serviços reais é **manual e opcional** em GitHub Actions (`workflow_dispatch`) porque os microsserviços não fazem parte desta entrega. Configure `QA_ORDER_BASE_URL` e `QA_CATALOG_BASE_URL` como variáveis do repositório acessíveis a partir do runner.

## 9. Limitações conhecidas / decisões necessárias

- **GAP-01:** `order-service` aceita até 100 itens, `catalog-service` aceita 50 IDs. Não há oráculo para 51–100 distintos sem decisão de contrato.
- **GAP-02:** '1 MB' não explicita exatidão em bytes. PED-07 valida o excedente inequívoco; L-1/L/L+1 requer `QA_MAX_PAYLOAD_BYTES` confirmado.
- **GAP-03:** o contrato não divulga IDs de itens inativos, sem preço ou com preços de borda. Provisionamento externo e variáveis são necessários.
- **GAP-04:** retries, 503/504, rollback e contagem de queries exigem mecanismos de injeção de falhas/instrumentação além do simples acesso REST; não foram simulados nem afirmados como cobertos.
- O payload de cliente valida obrigatoriedade e formato de e-mail, mas **não existe regra documentada de CPF válido**; os testes não impõem tal regra.
- Para comprovar rollback em nível de `order_items`, adicionar validação de persistência com credenciais autorizadas; a API não fornece acesso a essas tabelas.

## 10. Fontes e versões

Conteúdo derivado dos quatro documentos fornecidos no contexto da tarefa e da matriz otimizada. Dependência Rest-Assured 5.5.7: [Maven Central](https://central.sonatype.com/artifact/io.rest-assured/rest-assured/versions).

**Status de validação:** os fontes foram revisados estruturalmente, mas a suíte não foi compilada/executada contra as APIs aqui, porque Maven, acesso ao repositório de dependências e serviços em execução não estão disponíveis neste ambiente. Execute `mvn clean test` no seu ambiente com os serviços ativos para obter resultados reais.


## 11. Extensão: restante do catálogo de back-end sem novas massas

Esta versão agrega **6 casos lógicos novos** (`FIN-03`, `ORD-05`, `CAT-02`, `INT-02`, `INT-07`, `DB-01`) aos 20 originais. A matriz de origem tem **41 casos** de back-end: os **15 demais ficam explicitamente excluídos**, com motivo em `docs/CASOS-ADICIONAIS.md`. Os testes novos usam exclusivamente o seed de produtos original, IDs já existentes e operações normais das APIs. Não alteram preços, produtos, regras nem estruturas do banco.

**Os POSTs de criação efetivamente gravam pedidos**: utilize um ambiente QA. O caso `ORD-05` fica `skipped` se houver menos de dois pedidos disponíveis; não cria registros apenas para satisfazer a pré-condição. Coberturas marcadas **parciais** não equivalem ao cenário integralmente comprovado.

Para rodar só os casos novos:

```bash
mvn -Dtest=OrderFinancialAdditionalApiTest,OrderListingAdditionalApiTest,CatalogValidityAdditionalApiTest,OrderCatalogRejectionAdditionalApiTest,ConcurrentOrdersAdditionalApiTest,OrderPersistenceObservableAdditionalApiTest test
```

O projeto pode ser compilado sem os serviços, porém o teste de API real depende de serviços em execução e acesso de rede aos ambientes.


## INT-08 — Lacuna 100 itens versus 50 IDs

Novo teste `OrderCatalogBatchLimitApiTest` usa AVL (50, 51, 100, 101 IDs distintos) sem alterar massa do catálogo. Para 51 e 100, a expectativa **proposta** é 422 PRODUCT_NOT_FOUND (IDs gerados são inexistentes), sujeita à decisão de contrato GAP-01; consulte `docs/INT-08-GAP-01.md`. Executar: `mvn -Dtest=OrderCatalogBatchLimitApiTest test`.
