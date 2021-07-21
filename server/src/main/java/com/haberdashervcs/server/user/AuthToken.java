package com.haberdashervcs.server.user;


public final class AuthToken {

    public enum Type {
        CLI,
        WEB
    }


    private final Type type;
    private final String tokenId;
    private final HdUser user;
    private final String org;

    public AuthToken(Type type, String tokenId, HdUser user, String org) {
        this.type = type;
        this.tokenId = tokenId;
        this.user = user;
        this.org = org;
    }

    public Type getType() {
        return type;
    }

    public String getTokenId() {
        return tokenId;
    }

    public HdUser getUser() {
        return user;
    }

    public String getOrg() {
        return org;
    }
}
