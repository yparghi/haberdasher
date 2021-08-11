package com.haberdashervcs.common.objects.user;


public final class UserAuthToken {

    public enum Type {
        CLI,
        WEB
    }


    public static UserAuthToken forCli(String tokenId, HdUser user) {
        return new UserAuthToken(Type.CLI, tokenId, user);
    }

    public static UserAuthToken forWeb(String tokenId, HdUser user) {
        return new UserAuthToken(Type.WEB, tokenId, user);
    }


    private final Type type;
    private final String tokenId;
    private final HdUser user;

    private UserAuthToken(Type type, String tokenId, HdUser user) {
        this.type = type;
        this.tokenId = tokenId;
        this.user = user;
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
}
