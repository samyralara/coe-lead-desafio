# Especificação de 20 casos de teste automatizados — Back-end

Referências: `requirements.md`, `architecture.md`, `api.md` e `Matriz_Casos_Teste_Otimizada_PE_AVL.xlsx` disponibilizados para a elaboração do projeto.

**Definição de contagem:** cada método anotado com `@Test` ou `@ParameterizedTest` equivale a um caso lógico. Casos parametrizados geram várias invocações, permitindo cobertura das partições e valores limite sem manter métodos redundantes. Cenários ainda sem contrato fechado não recebem asserções arbitrárias.

| Caso | Técnica | Entrada/variações | Oráculo de teste | Requisito | Preparação |
|---|---|---|---|---|---|
| PED-01 | PE nominal | PROD-001×2 + PROD-002×1 | 201, Location, status CRIADO; subtotal 7499,90, desconto 749,99, total 6749,91, GET por ID | RF-01/04/05 | Seed de referência |
| PED-02 | PE inválida | nome ausente/vazio, documento ausente, email ausente/inválido, cliente ausente | 400 VALIDATION_ERROR e envelope | RF-01 | Nenhuma extra |
| PED-03 | AVL / cardinalidade | 0, 1, 101 itens | 0 e 101 → 400; 1 → 201 | RF-01 | Seed de referência |
| PED-04 | AVL | -1, 0, 1, 2, 998, 999, 1000 | válidos 201 com linha calculada; inválidos 400 | RF-01 | Seed de referência |
| PED-05 | PE + AVL | ausente, vazio, 1, 49, 50, 51 caracteres | 400 nos inválidos estruturais; 422 PRODUCT_NOT_FOUND nos IDs sintaticamente válidos e inexistentes | RF-01/05 | IDs 'Z...' não presentes |
| PED-06 | PE inválida | PROD-001 repetido duas vezes | 400 VALIDATION_ERROR | RF-01 | Nenhuma extra |
| PED-07 | AVL (bytes) | 1.100.000 bytes; opcional L-1, L e L+1 | 413 > limite; <=L não retorna 413 | RF-01 | opcional `QA_MAX_PAYLOAD_BYTES` |
| PED-08 | PE inválida | JSON malformado | 400 VALIDATION_ERROR | RF-01 | Nenhuma extra |
| PED-09 | PE semântica | inexistente / inativo / sem preço vigente | 422 com três códigos distintos | RF-05 | Dois IDs configuráveis para classes especiais |
| PED-10 | Tabela de decisão | ativo + inexistente | 422 e contagem de pedidos sem alteração | RF-01/05 | Ambiente isolado |
| FIN-01 | AVL do limiar | 999,99 / 1000,00 / 1000,01 | desconto 0 / 100 / 100 e total correto | RF-04 | Três produtos com preços controlados |
| FIN-02 | AVL arredondamento | subtotal 1000,05 | HALF_UP: desconto 100,01, total 900,04 | RF-04 | Produto com preço 1000,05 |
| ORD-01 | PE existência | UUID existente e inexistente | 200 completo; 404 ORDER_NOT_FOUND | RF-03 | Seed de referência |
| ORD-02 | PE / paginação | sem parâmetros, size=1 e página além da última | defaults 0/20; conteúdo/metadados corretos | RF-02 | Serviço ativo |
| ORD-03 | PE status | CRIADO e INVALIDO | 200 apenas CRIADO; 400 VALIDATION_ERROR | RF-02 | Serviço ativo |
| ORD-04 | AVL paginação | size 0, 1, 100, 101 | 400 / 200 / 200 / 400 | RF-02 | Serviço ativo |
| CAT-01 | PE existência/status | PROD-001, ID inexistente, inativo opcional | 200 com preço; 404; status INATIVO quando configurado | RF-06 | Seed de referência; ID opcional |
| CAT-03 | PE lote misto | PROD-001, PROD-002, ID inexistente | 200; dois produtos em prices; um em notFound | RF-07 | Seed de referência |
| CAT-04 | AVL batch/duplicidade | 1, 49, 50, 51 IDs; 2 duplicados | 200 até 50, 400 acima e em duplicatas | RF-07 | Serviço ativo |
| INT-06 | PE cabeçalho | Request-ID informado / ausente; dois serviços | Request-ID gerado/ecoado em header e envelope | ADR-008 | Ambos os serviços |

## Escopo e ressalvas

- O **caso PED-03** não declara se 51–100 produtos distintos devem ser aceitos: há conflito entre limite de 100 itens por pedido e lote de 50 IDs do catálogo (GAP-01). Apenas 0, 1, 101 foram automatizados sem ambiguidade.
- O **caso PED-07** aceita configurar o limite exato em bytes; a documentação registra '1 MB' sem explicitar se é 1.000.000 ou 1.048.576 bytes. Por padrão executa somente a classe inequivocamente acima do limite.
- O **caso PED-05** usa IDs sintaticamente válidos inexistentes para comprovar que passaram pela validação do comprimento; espera 422 em vez de 201, pois não há tais produtos cadastrados.
- O **caso PED-10** observa a ausência de registro novo pela API. Para demonstrar também ausência de registros órfãos em `order_items`, acrescentar um teste JDBC após conhecer a configuração e credenciais do banco, fora destes 20.
- **FIN-01/FIN-02** e algumas variações de PED-09/CAT-01 dependem de massas não identificadas nos quatro arquivos; as invocações são **abortadas (skipped)** com motivo explícito até que os IDs sejam informados.
- Não foram incluídos nos 20 testes: falhas 503/504, simulação controlada de retries, contagem de consultas Mongo, modificações de regra de desconto, snapshot com alteração de preço, nem concorrência. Esses exigem hooks/configuração do ambiente ou fixtures adicionais e podem ser incorporados posteriormente.
- O pacote aponta diretamente para o Order Service (8080) e Catalog Service (8081), **sem uso do proxy Angular**.

## Resultado por técnica

Asserções cobrem valores internos e extremos e classes de respostas distintas; parametrização evita duplicação. O total de invocações é superior a 20 e depende das variações ativadas e das massas provisionadas. **20 é o número de casos lógicos implementados, não a quantidade fixa de execuções JUnit.**
