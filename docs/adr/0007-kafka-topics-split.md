# ADR 0007: Tópicos no Kafka separados por status do pedido

## Status

Aceito

## Contexto

O sistema precisa lidar com diferentes eventos de pedidos no Kafka. Cada tipo de evento possui um payload diferente, e utilizar um único tópico global com um consumer genérico adiciona complexidade conforme a quantidade de tipos de eventos de ordem aumenta.

## Decisão

Separar eventos em tópicos distintos por tipo:

- `order-created` - eventos de novos pedidos
- `order-processed` - pedidos processados com sucesso
- `order-failed` - pedidos que falharam no processamento
- `order-pending-approval` - pedidos aguardando aprovação manual
- `low-stock-alert` - alertas de estoque baixo

## Consequências

### Positivas

- Consumers mais simples e focados
- Payloads type-safe por tópico
- Facilita manutenção e debug
- Possibilidade de escalar consumers independentemente

### Negativas

- Mais tópicos para gerenciar
- Configuração adicional no Kafka
- Necessário criar mais tópicos, producers e consumers, sempre que um novo status de pedido é criado

