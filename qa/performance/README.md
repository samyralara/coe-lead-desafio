# README - Testes de Performance com Apache JMeter

## Visão Geral

Este projeto tem como objetivo validar o desempenho e a estabilidade das APIs de Catálogo e Pedidos através de testes de performance utilizando Apache JMeter.

Foram implementados testes para consulta, criação e recuperação de dados, com parametrização de massa de testes via CSV, validações de resposta e monitoramento dos resultados através do Summary Report do JMeter.

## Arquitetura da Aplicação

### Catalog Service

Responsável pelas operações relacionadas a produtos.

```text
Host: localhost
Porta: 8081
```

Endpoints testados:

```http
GET /api/v1/products/{id}
POST /api/v1/products/prices
```

### Order Service

Responsável pelas operações relacionadas a pedidos.

```text
Host: localhost
Porta: 8080
```

Endpoints testados:

```http
GET /api/v1/orders
POST /api/v1/orders
GET /api/v1/orders/{id}
```

## Pré-requisitos

- Java 17+
- Apache JMeter 5.6.3
- Docker Desktop
- MongoDB Compass (opcional)
- DBeaver (opcional)

## Infraestrutura

### Containers Docker

```bash
docker ps
```

Serviços identificados:

```text
Catalog Service   -> Porta 8081
Order Service     -> Porta 8080
MongoDB           -> Porta 27017
PostgreSQL        -> Porta 5432
Frontend          -> Porta 4200
```

## Resultado Final

A suíte de testes cobre:

```http
GET  /api/v1/products/{id}
POST /api/v1/products/prices
GET  /api/v1/orders
POST /api/v1/orders
GET  /api/v1/orders/{id}
```

Com suporte a:

- Parametrização via CSV
- Criação dinâmica de massa
- Captura automática de IDs
- Validação de respostas
- Testes de carga
- Testes de stress
- Validação de persistência em MongoDB e PostgreSQL