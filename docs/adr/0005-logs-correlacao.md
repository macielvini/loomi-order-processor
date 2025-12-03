# ADR 0005: Estruturação de Logs e Correlação

## Status

Aceito

## Contexto

O sistema precisa de logs estruturados e correlacionados para facilitar debug e observabilidade, especialmente em um ambiente distribuído com Kafka.

## Decisão

### Stack de Logging

- **SLF4J + Logback**: framework de logging
- **logstash-encoder**: geração de logs em JSON
- **Configuração global**: `/logback-spring.xml`

### Correlação via MDC (Mapped Diagnostic Context)

Campos incluídos no contexto:
- `correlationId`: ID único de correlação
- `orderId`: ID do pedido
- `customerId`: ID do cliente

### Correlação HTTP

`CorrelationIdFilter` busca header de correlação na requisição. Se não encontrar, gera um novo ID e adiciona no header de resposta e no contexto MDC.

### Correlação Kafka

1. **Producer**: `correlationId` adicionado nos headers do evento
2. **Consumer**: `CorrelationIdInterceptor` extrai ID dos headers e inclui no MDC

### Configuração

Nível de log controlado via variável de ambiente `LOG_LEVEL` no `.env`.

## Exemplo de Output

```json
{
   "@timestamp": "2025-12-02T13:49:32.906573285-03:00",
   "message": "Received Order Created Event: OrderCreatedEvent()",
   "logger_name": "com.loomi.order_processor.infra.consumer.OrderEventListenerImpl",
   "level": "INFO",
   "correlationId": "e7dcc43d-8f12-472b-866b-4c118f733867",
   "application": "order_processor"
}
```

## Consequências

### Positivas

- Rastreabilidade end-to-end entre HTTP e Kafka
- Logs estruturados facilitam análise e integração com ferramentas (ELK, etc.)
- Configuração flexível via variável de ambiente

### Negativas

- Nenhuma identificada

