package com.haberdashervcs.common.objects.user;


public final class HdUser {

    private final String userId;
    private final String email;
    private final String org;
    // TODO: bcrypt or something
    private final String password;

    public HdUser(String userId, String email, String org, String password) {
        this.userId = userId;
        this.email = email;
        this.org = org;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getOrg() {
        return org;
    }

    public String getPassword() {
        return password;
    }
}
