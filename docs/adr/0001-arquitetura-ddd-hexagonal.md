# ADR 0001: Arquitetura DDD e Hexagonal

## Status

Aceito

## Contexto

O projeto precisa de uma arquitetura que permita separação clara entre regras de negócio e implementação, facilitando manutenção, testes e evolução futura.

## Decisão

Adotar DDD (Domain-Driven Design) combinado com Arquitetura Hexagonal, separando o código em 3 camadas principais:

- **domain**: regras de negócio (use-cases), entidades e value-objects (DTOs)
- **app**: orquestração das regras de negócio e serviços externos
- **infra**: acesso a serviços externos (banco de dados, mensageria, APIs)

## Consequências

### Positivas

- Separação clara entre regra de negócio e implementação
- Facilita aplicação de princípios SOLID e Design Patterns
- Testes unitários mais simples no domínio
- Flexibilidade para trocar implementações de infraestrutura

### Negativas

- **Contaminação do domain**: devido a restrições de tempo, o domínio não ficou 100% agnóstico a bibliotecas externas (ex: `@Entity` do JPA dentro do domínio)
- Isso não impacta a aplicação, mas pode impactar o desenvolvimento futuro

### Ações Futuras

- Revisão de baixa prioridade para remover dependências externas do domínio

