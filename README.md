# NATS Connection Manager

Uma biblioteca Java que abstrai e padroniza a comunicação com o servidor [NATS](https://nats.io/) em arquiteturas distribuídas, facilitando a implementação de padrões de messaging como **Request-Reply** e **Publish** de forma simplificada.

## 🎯 Objetivo

Fornecer uma abstração robusta para gerenciar conexões com NATS, permitindo que aplicações se conectem, publica e subscrevam a eventos de forma padronizada, com tratamento automático de serialização, logging e resolução de desconexões.

## 📋 Funcionalidades

- ✅ **Gerenciamento automático de conexão** com NATS
- ✅ **Padrão Request-Reply** abstraído com callbacks
- ✅ **Publicação de eventos** com serialização automática
- ✅ **Listener de eventos de conexão** para monitoramento
- ✅ **Shutdown automático** ao encerrar a aplicação
- ✅ **Logging integrado** com Log4j2 (em português)
- ✅ **DTO padronizado** (`EventDTO`) para todas as respostas
- ✅ **Sem código boilerplate** — apenas configure e use

## 📦 Instalação

### Maven

Adicione a dependência ao seu `pom.xml`:

```xml
<dependency>
    <groupId>com.github.mathlazaro</groupId>
    <artifactId>nats-conn-manager</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Dependências incluídas

- **NATS Java Client** (`io.nats:jnats:2.16.0`)
- **Jackson** para serialização JSON
- **Lombok** para redução de boilerplate
- **Log4j2** para logging

## 🚀 Uso Rápido

### 1. Inicializar a conexão

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mathlazaro.configuration.NatsConnectionManager;
import io.nats.client.Connection;

public class MinhaAplicacao {
    
    public static void main(String[] args) {
        String natsUrl = "nats://localhost:4222";
        String projectNickname = "meu-servico";
        
        ObjectMapper mapper = new ObjectMapper();
        NatsConnectionManager connManager = new NatsConnectionManager(
            natsUrl, 
            projectNickname, 
            mapper
        );
        
        Connection nc = connManager.getNc();
        
        // Sua aplicação está pronta para usar NATS!
    }
}
```

### 2. Padrão Request-Reply

Um serviço que recebe requisição, processa e envia resposta:

```java
import com.github.mathlazaro.domain.EventDTO;
import io.nats.client.Message;

// Registrar um listener que responde a requisições
connManager.publishReply("pedidos.processar", (Message msg) -> {
    try {
        String pedidoId = new String(msg.getData());
        
        // Processar pedido...
        String resultado = "Pedido " + pedidoId + " processado com sucesso";
        
        return EventDTO.success(resultado);
    } catch (Exception e) {
        return EventDTO.error("Erro ao processar pedido: " + e.getMessage());
    }
});
```

### 3. Publicar eventos

```java
import com.github.mathlazaro.domain.EventDTO;

// Publicar um evento sem esperar resposta
EventDTO<?> evento = EventDTO.success("Dados importantes");
connManager.publish("notificacoes.email", evento);
```

### 4. Cliente fazendo requisição (Request-Reply)

```java
// Enviar uma requisição e esperar a resposta
Message resposta = nc.request("pedidos.processar", "PED123".getBytes(), Duration.ofSeconds(5));

// Processar resposta...
String dados = new String(resposta.getData());
```

## 📊 Estrutura

```
nats-conn-manager/
├── src/main/java/com/github/mathlazaro/
│   ├── configuration/
│   │   └── NatsConnectionManager.java      # Classe principal
│   └── domain/
│       └── EventDTO.java                   # DTO padronizado
└── pom.xml
```

## 🔧 Classes Principais

### `NatsConnectionManager`

Responsável por gerenciar a conexão NATS e fornecer abstrações para messaging.

#### Construtor
```java
NatsConnectionManager(String natsUrl, String projectNickname, ObjectMapper objectMapper)
```

- **natsUrl**: URL do servidor NATS (ex: `nats://localhost:4222`)
- **projectNickname**: Nome/nickname do projeto para identificação nos logs
- **objectMapper**: Instância de `ObjectMapper` para serialização JSON

#### Métodos

| Método | Descrição |
|--------|-----------|
| `getNc()` | Retorna a conexão NATS bruta (`Connection`) |
| `publishReply(String subject, Function<Message, EventDTO<?>> callback)` | Registra um handler para Request-Reply |
| `publish(String subject, EventDTO<?> event)` | Publica um evento simples (sem resposta) |
| `closeConnection()` | Fecha a conexão com NATS |

### `EventDTO<T>`

Record padronizado para todas as respostas e eventos.

```java
public record EventDTO<T>(String status, T payload, String error)
```

#### Métodos auxiliares

```java
// Criar uma resposta de sucesso
EventDTO<String> sucesso = EventDTO.success("Processamento OK");

// Criar uma resposta de erro
EventDTO<Void> erro = EventDTO.error("Ocorreu um erro");

// Converter erro para bytes
byte[] msgErro = EventDTO.errorBytes("Erro crítico");
```

## 📝 Exemplo Completo: Serviço de Processamento

```java
package com.exemplo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mathlazaro.configuration.NatsConnectionManager;
import com.github.mathlazaro.domain.EventDTO;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ProcessadorPedidos {
    
    private final NatsConnectionManager connManager;
    
    public ProcessadorPedidos() {
        ObjectMapper mapper = new ObjectMapper();
        this.connManager = new NatsConnectionManager(
            "nats://localhost:4222",
            "processador-pedidos",
            mapper
        );
    }
    
    public void iniciar() {
        // Registrar handler para processar pedidos
        connManager.publishReply("pedidos.processar", msg -> {
            try {
                String pedidoJson = new String(msg.getData());
                log.info("Pedido recebido: {}", pedidoJson);
                
                // Lógica de processamento...
                String resultado = "Pedido processado";
                
                return EventDTO.success(resultado);
            } catch (Exception e) {
                log.error("Erro ao processar pedido", e);
                return EventDTO.error(e.getMessage());
            }
        });
        
        log.info("Processador iniciado e aguardando pedidos");
    }
    
    public static void main(String[] args) {
        new ProcessadorPedidos().iniciar();
    }
}
```

## 🔐 Configuração NATS

Certifique-se de que o servidor NATS está rodando:

```bash
# Iniciar NATS localmente (com Docker)
docker run -d -p 4222:4222 nats:latest

# Ou instalar localmente: https://nats.io/download/
nats-server
```

## 📊 Fluxo de Conexão

1. **Inicialização**: `NatsConnectionManager` é instanciado
2. **Listener de Conexão**: Um `ConnectionListener` é registrado automaticamente
3. **Log de Evento**: Evento `CONNECTED` publica mensagem em `logs.connection`
4. **Pronto para uso**: Pode-se usar `publishReply()`, `publish()` e acessar `getNc()`
5. **Shutdown**: Ao encerrar a aplicação, fecha a conexão automaticamente

## 🔍 Logging

A biblioteca usa **Log4j2** e logging está em português. Exemplo de output:

```
[INFO] conexao conectado
[INFO] Processador iniciado e aguardando pedidos
[INFO] Pedido recebido: {"id":"123"}
[ERROR] Erro ao processar pedido: NullPointerException
```

Configure Log4j2 no seu `log4j2.xml` conforme necessário.

## 🤝 Contribuindo

Se encontrar bugs ou tiver sugestões, considere abrir uma issue ou pull request no repositório.

## 📄 Licença

Este projeto é parte de um trabalho acadêmico de Sistemas Distribuídos.

## 🔗 Referências

- [NATS Official Documentation](https://docs.nats.io/)
- [NATS Java Client](https://github.com/nats-io/nats.java)
- [Jackson JSON Library](https://github.com/FasterXML/jackson)

---

**Versão**: 1.0.1  
**Autor**: Matheus Lázaro  
**Última atualização**: Maio 2026

