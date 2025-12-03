# ADR 0002: Estratégia de CI e Testes

## Status

Aceito

## Contexto

O projeto precisa de uma estratégia de testes que garanta qualidade do código e validação do fluxo completo (API → Evento → Processamento → Atualização de status).

## Decisão

1. **Testes unitários**: foco nas regras de negócio por tipo de produto (`domain/order/service`), atingindo 98% de cobertura (JaCoCo)
2. **Testes de integração**: validação do fluxo completo, mais abrangentes e focados no fluxo end-to-end
3. **CI com GitHub Actions**: todos os testes devem passar para uma PR ser aceita
4. **Testcontainers**: uso de containers para testes de integração com Kafka e banco de dados

## Consequências

### Positivas

- Alta cobertura das regras de negócio (+90%)
- Validação automática em PRs
- Ambiente de teste isolado e reproduzível

### Negativas

- Testes de integração não cobrem todas as regras de produtos (testadas em unitários)
- Testes com Testcontainers pulados no GitHub Actions (tier FREE) para manter CI < 15 minutos
- Possíveis problemas de concorrência com Testcontainers + Kafka (~5% de falhas)
- Teste `OrderProcessorApplicationTests` desativado temporariamente devido a flakiness (instável)

### Ações Futuras

- Expandir testes de integração para cobrir regras de produtos
- Revisar configuração de CI quando recursos permitirem
- Investigar e resolver problemas de concorrência com Testcontainers

