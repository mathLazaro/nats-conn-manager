package com.github.mathlazaro.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mathlazaro.domain.EventDTO;
import io.nats.client.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.function.Function;

/**
 * Classe responsável por gerenciar a conexao com o NATS
 */
@Log4j2
public class NatsConnectionManager {

    private final Options options;

    private final ObjectMapper mapper;

    @Getter
    private final Connection nc;


    /**
     * Listener de conexão para logar eventos de conexão e desconexão
     *
     * @param projectNickname
     */
    private record ConnectionListenerImpl(String projectNickname) implements ConnectionListener {

        @Override
        public void connectionEvent(Connection conn, Events type) {
            switch (type) {
                case CONNECTED:
                    String logMessage = projectNickname + " conectado";
                    log.info(logMessage);
                    conn.publish("logs.connection", logMessage.getBytes());
                    break;
                case CLOSED:
                    log.info("{} desconectado", projectNickname);
                    break;
                default:
                    log.info("Evento Nats: {}", type);
            }
        }
    }

    public NatsConnectionManager(String natsUrl, String projectNickname, ObjectMapper objectMapper) {
        this.mapper = objectMapper;
        this.options = new Options.Builder()
                .server(natsUrl)
                .connectionListener(new ConnectionListenerImpl(projectNickname))
                .build();
        this.nc = openConnection();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownProducer));
    }

    /**
     * Abre a conexão com o NATS usando as opções configuradas, a conexão é aberta automaticamente ao instanciar a classe
     *
     * @return NATS connection
     */
    private Connection openConnection() {
        try {
            log.info("Iniciando conexao no servidor NATS: {}", options.getServers().stream().findFirst().map(Object::toString).orElse(""));
            return Nats.connect(options);
        } catch (IOException | InterruptedException e) {
            log.error("Erro na abertura de conexão", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Abstari a conexão Request-Reply do nats, o callback é chamado toda vez que uma mensagem chegar no subject, a resposta é enviada automaticamente para o replyTo da mensagem
     *
     * @param subject  subject do tópico a ser subscrito o handler
     * @param callback callback responsável por lidar com os dados recebidos
     */
    public void publishReply(String subject, Function<Message, EventDTO<?>> callback) {
        Dispatcher dispatcher = nc.createDispatcher(msg -> {
            EventDTO<?> payload = callback.apply(msg);
            if (msg.getReplyTo() != null) {
                byte[] data;
                try {
                    data = mapper.writeValueAsBytes(payload);
                } catch (JsonProcessingException e) {
                    log.error("Erro ao serializar a resposta", e);
                    data = EventDTO.errorBytes("Erro ao serializar a resposta");
                }
                nc.publish(msg.getReplyTo(), data);
            }
        });
        dispatcher.subscribe(subject);
    }

    /**
     * Abstrai a publicação de eventos no NATS
     *
     * @param subject subject do tópico a ser publicado
     * @param event   payload da mensagem
     */
    public void publish(String subject, EventDTO<?> event) {
        byte[] data;
        try {
            data = mapper.writeValueAsBytes(event);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar a resposta", e);
            data = EventDTO.errorBytes("Erro ao serializar a resposta");
        }
        nc.publish(subject, data);
    }

    /**
     * Fecha a conexão do NATS
     */
    public void closeConnection() {
        if (nc == null) {
            log.error("Nenhuma conexão para fechar");
            return;
        }

        try {
            nc.close();
        } catch (InterruptedException e) {
            log.error("Conexão nats fechada", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Lida com o encerramento inesperado da thread principal
     */
    private void shutdownProducer() {
        log.info("Iniciando shutdown do producer");
        closeConnection();
    }

}
