package com.haberdashervcs.common.objects.user;


public final class UserAuthToken {

    public enum Type {
        CLI,
        WEB
    }


    public static UserAuthToken forCli(String tokenId, String userId, String org) {
        return new UserAuthToken(Type.CLI, tokenId, userId, org);
    }

    public static UserAuthToken forWeb(String tokenId, String userId, String org) {
        return new UserAuthToken(Type.WEB, tokenId, userId, org);
    }


    private final Type type;
    private final String tokenId;
    private final String userId;
    private final String org;

    private UserAuthToken(Type type, String tokenId, String userId, String org) {
        this.type = type;
        this.tokenId = tokenId;
        this.userId = userId;
        this.org = org;
    }

    public Type getType() {
        return type;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrg() {
        return org;
    }
}
