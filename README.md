
## Configurações e inicialização

Para iniciar, depurar, testar e dockerizar a aplicação, utilizados os comandos do [Makefile](./Makefile):

```
make setup          # Configurar ambiente (build de containers, etc.)
make up             # Subir toda a infraestrutura (docker-compose up)
make down           # Derrubar infraestrutura
make build          # Build da aplicação
make test           # Executar testes
make clean          # Limpar containers e volumes
make logs           # Ver logs da aplicação
make db-migrate     # Executa migrações de banco (necessário aplicação rodando)
make coverage       # Executa JaCoCo para analise de cobertura de testes
```

### O que você vai precisar:
- terminal bash
- Docker
- docker compose
- Maven (apenas para migrations)

### Inicializar a aplicação
1. Crie um arquivo `.env`, copie o conteúdo de `env.example` e ajuste os valores conforme necessário
1. Execute o comando ```make up```

### Aplicar migrações no banco de dados (se necessário)
1. Ajuste a URL do banco de dados no `Makefile`, se o banco de dados está num container Docker, aponte para porta do container
1. Execute o comando ```make migrate-db```

## Quanto ao uso de IA

Durante o desenvolvimento, utilizei extensivamente o Cursor, ChatGPT e CodeRabbit.

O Cursor foi utilizado para geração de códigos boilerplate inicial (entidades, dtos, controllers, etc), através de prompts estruturados com contexto, objetivo e regras claras. Com isso, eu solicitava um plano de implementação do que foi requisitado, revisava o plano e solicitava a implementação. Após a implementação, todo o código era revisado.
Além disso, usei para gerar os testes inicias do projeto, mais tarde, utilizando a mesma estratégia de prompt, eu informava um cenário (contexto) e solicitava a criação do teste.

Essa foi minha primeira vez usando Cursor com Java (havia utilizado Copilot anteriormente), e muitas implementações inicias feitas por ele precisavam de correção, como por exemplo, ele insiste em fazer comparações usando `==`, quando esse operador só funciona para tipos primitivos. Por isso, posso dizer que apesar de utilizar o Cursor, boa parte do código precisava de revisão e ajuste manual.

O ChatGPT foi utilizado principalmente para debug de erros do Kafka, Docker e Testcontainers. Também o utilizei para modulação do projeto, apesar de já conhecer a arquitetura DDD, alguns casos podem requerer uma visão alternativa, como se o dominio (domain) deve conhecer a existencia de eventos do Kafka.

CodeRabbit foi utilizado diretamente no repositório GitHub, onde, em cada PR, criava um resumo das alterações e fazia comentários sobre o código, apontando possiveis falhas e melhorias.

## ADRs

As decisões arquiteturais do projeto estão documentadas em [docs/adr/](./docs/adr/README.md):

| ADR | Título |
|-----|--------|
| [0001](./docs/adr/0001-arquitetura-ddd-hexagonal.md) | Arquitetura DDD e Hexagonal |
| [0002](./docs/adr/0002-estrategia-ci-testes.md) | Estratégia de CI e Testes |
| [0003](./docs/adr/0003-dlq-retry-kafka.md) | Dead Letter Queue e Retry no Kafka |
| [0004](./docs/adr/0004-handler-pattern-processamento.md) | Handler Pattern para Validação e Processamento |
| [0005](./docs/adr/0005-logs-correlacao.md) | Estruturação de Logs e Correlação |
| [0006](./docs/adr/0006-kafka-zookeeper.md) | Kafka com Zookeeper e Kafka UI |