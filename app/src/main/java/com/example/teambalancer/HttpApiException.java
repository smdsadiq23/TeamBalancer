package com.example.teambalancer;

public final class HttpApiException extends Exception {

    public final int code;

    public HttpApiException(int code, String message) {
        super(message);
        this.code = code;
    }
}
