# Architecture Decision Records (ADRs)

Este diretório contém os registros de decisões arquiteturais do projeto Order Processor.

## O que é um ADR?

Um ADR (Architecture Decision Record) é um documento que captura uma decisão arquitetural importante, junto com seu contexto e consequências.

## Índice

| ADR | Título | Status |
|-----|--------|--------|
| [0001](./0001-arquitetura-ddd-hexagonal.md) | Arquitetura DDD e Hexagonal | Aceito |
| [0002](./0002-estrategia-ci-testes.md) | Estratégia de CI e Testes | Aceito |
| [0003](./0003-dlq-retry-kafka.md) | Dead Letter Queue e Retry no Kafka | Aceito |
| [0004](./0004-handler-pattern-processamento.md) | Handler Pattern para Validação e Processamento | Aceito |
| [0005](./0005-logs-correlacao.md) | Estruturação de Logs e Correlação | Aceito |
| [0006](./0006-kafka-zookeeper.md) | Kafka com Zookeeper e Kafka UI | Aceito |

## Formato

Cada ADR segue a estrutura:

- **Status**: Proposto, Aceito, Depreciado, Substituído
- **Contexto**: Por que essa decisão foi necessária
- **Decisão**: O que foi decidido
- **Consequências**: Resultados positivos e negativos
- **Ações Futuras** (opcional): Melhorias identificadas

