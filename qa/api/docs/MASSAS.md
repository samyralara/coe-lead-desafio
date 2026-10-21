# Massas e pré-condições

## Massas de referência do contrato

| Produto | Propriedade contratual | Uso |
|---|---|---|
| PROD-001 | ATIVO, preço vigente R$ 3500,00 | PED-01/03/04/06/10, ORD-01, CAT-01/03/04 |
| PROD-002 | ATIVO, preço vigente R$ 499,90 | PED-01, CAT-03 |

As propriedades acima vêm dos exemplos de `api.md` e do seed descrito em `requirements.md`. Verifique que o banco QA mantém os mesmos dados; os testes devem falhar se o seed esperado não estiver presente.

## Massas adicionais (somente ambiente de teste)

O contrato descreve condições, mas **não fornece todos os IDs nem o schema interno completo de `products` no MongoDB**. Por isso o projeto não tenta inserir diretamente documentos com uma estrutura inventada. Utilize o seed do repositório da aplicação (`docker/mongo-init.js`) ou o processo oficial de carga e configure os IDs abaixo:

| Variável de ambiente | Propriedade Maven | Estado obrigatório para o caso |
|---|---|---|
| QA_INACTIVE_PRODUCT_ID | qa.inactiveProductId | produto INATIVO |
| QA_NO_PRICE_PRODUCT_ID | qa.noPriceProductId | produto ATIVO sem preço vigente |
| QA_PRICE_999_99_ID | qa.price99999Id | produto ATIVO, currentPrice.value=999.99 |
| QA_PRICE_1000_00_ID | qa.price100000Id | produto ATIVO, currentPrice.value=1000.00 |
| QA_PRICE_1000_01_ID | qa.price100001Id | produto ATIVO, currentPrice.value=1000.01 |
| QA_PRICE_1000_05_ID | qa.price100005Id | produto ATIVO, currentPrice.value=1000.05 |
| QA_MAX_PAYLOAD_BYTES | qa.maxPayloadBytes | limite em bytes, caso confirmado no ambiente |

**Regra de desconto:** tabela `discount_rule` ativa com subtotal mínimo R$ 1.000,00 e percentual 10% (Flyway V2). Sem essa condição, os testes FIN e PED-01 falham e devem ser executados em ambiente preparado, não ajustados para mascarar divergências.

**Ambiente isolado:** os testes de criação efetivamente criam pedidos. Não executar contra produção; não habilitar paralelismo entre suites que verifiquem contagem (`PED-10`). Planejar limpeza dos registros de teste após o ciclo; a API documentada não expõe exclusão de pedidos.

**IDs de produto inexistentes:** gerados com prefixo `QA-MISSING-` e UUID. As variações de comprimento usam `Z` repetido; garantir que esses IDs não estejam cadastrados.
