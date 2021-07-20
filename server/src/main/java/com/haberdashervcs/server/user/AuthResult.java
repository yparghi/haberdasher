package com.haberdashervcs.server.user;


public final class AuthResult {

    public enum Type {
        PERMITTED,
        FORBIDDEN,
        AUTH_EXPIRED
    }

    private final Type type;
    private final String message;

    public AuthResult(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
