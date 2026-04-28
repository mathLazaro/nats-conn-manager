package com.github.mathlazaro.domain;

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
