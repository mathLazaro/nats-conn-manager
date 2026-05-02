package com.github.mathlazaro.domain;

/**
 * DTO para padronizar a comunicação entre os workers
 *
 * @param status  status da comunicação: 'SUCCESS' | 'ERROR'
 * @param payload payload da mensagem
 * @param error   detalhes do erro obtido
 * @param <T>
 */
public record EventDTO<T>(String status, T payload, String error) {

    public static EventDTO<Void> error(String error) {
        return new EventDTO<>("ERROR", null, error);
    }

    public static <T> EventDTO<T> success(T payload) {
        return new EventDTO<>("SUCCESS", payload, null);
    }

    public static byte[] errorBytes(String error) {
        return error(error).toString().getBytes();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"status\":\"");
        sb.append(status);
        sb.append("\",\"payload\":\"");
        sb.append(payload);
        sb.append("\",\"error\":\"");
        sb.append(error);
        sb.append("\"}");
        return sb.toString();
    }
}
