# Cenários do front-end automatizados

| IDs | Objetivo |
|---|---|
| FE-001 a FE-011 | Criação, validação de campos, limites, duplicatas, preço, erro de rede, envio único e mensagens |
| FE-012, FE-013, FE-014, FE-016 a FE-019 | Lista, vazio, paginação, detalhe, 404, BRL/status, indisponibilidade |
| FE-020 a FE-025 | Consulta unitária e em lote, produto sem preço, inexistente, limite 50, aviso de lacuna |
| CAT-UX-01 | Deduplicação de IDs antes do lote |
| DASH-01 e DASH-02 | Indicadores limitados ao carregado e estado vazio |
| HTTP-01 a HTTP-06 | RequestId, envelope de erro, rede, fallback, URLs relativas e payload |
| E2E-001 | Jornada com serviços reais (somente TEST_MODE=integrated) |

**Não implementado propositalmente:** FE-015 (filtro de status) porque a listagem Angular recebida não oferece esse controle. O contrato de API prevê `status`, mas `OrderListPage` não o expõe.

Testes com mocks (`TEST_MODE=mock`) validam o comportamento do front-end, não simulam o banco. A jornada E2E usa os serviços existentes. As prioridades devem ser priorizadas conforme a matriz de 80 casos.
