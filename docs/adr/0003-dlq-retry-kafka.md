# ADR 0003: Dead Letter Queue e Retry no Kafka

## Status

Aceito

## Contexto

O sistema precisa de um mecanismo robusto para lidar com falhas no processamento de eventos Kafka, garantindo que mensagens problemáticas não bloqueiem o fluxo e possam ser analisadas posteriormente.

## Decisão

Implementar `DefaultErrorHandler` com as seguintes características:

1. **Retry exponencial**: reenvio automático do evento para o consumer com backoff exponencial
2. **DLQ (Dead Letter Queue)**: após esgotar tentativas de retry, evento é enviado para DLQ
3. **Ack manual**: exceções tratadas que fazem `ack.acknowledge()` não vão para DLQ (offset commitado)

## Consequências

### Positivas

- Resiliência no processamento de eventos
- Eventos problemáticos não bloqueiam o fluxo
- Possibilidade de análise posterior de falhas via DLQ
- Distinção clara entre erros recuperáveis e não-recuperáveis

### Negativas

- **Problema não solucionado**: não foi possível desativar a lógica de retry exponencial via configurações exclusivas para testes
- Necessário adicionar handler exclusivo para testes dentro da configuração de produção

### Ações Futuras

- Investigar forma de isolar configuração de retry para ambiente de testes

