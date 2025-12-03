# ADR 0006: Kafka com Zookeeper e Kafka UI

## Status

Aceito

## Contexto

O projeto precisa de um sistema de mensageria para comunicação assíncrona entre serviços. Existem alternativas como Redpanda (compatível com Kafka) e diferentes modos de operação do Kafka (KRaft vs Zookeeper).

## Decisão

1. **Kafka** ao invés de Redpanda por familiaridade
2. **Zookeeper mode** ao invés de KRaft mode para gerenciamento do cluster
3. **Kafka UI** para visualização e monitoramento de eventos

## Consequências

### Positivas

- Menor curva de aprendizado (familiaridade com Kafka)
- Zookeeper mode é mais maduro e tem mais conteúdo online 
- Kafka UI facilita debug e monitoramento de eventos

### Negativas

- Zookeeper adiciona componente extra
- Redpanda poderia ter uma implementação no ambiente mais fácil
- Zookeeper será descontinuado

