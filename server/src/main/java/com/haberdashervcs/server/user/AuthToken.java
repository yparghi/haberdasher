package com.haberdashervcs.server.user;


public final class AuthToken {

    private final HdUser user;
    private final String org;

    public AuthToken(HdUser user, String org) {
        this.user = user;
        this.org = org;
    }

    public HdUser getUser() {
        return user;
    }

    public String getOrg() {
        return org;
    }
}
