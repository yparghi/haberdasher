package com.haberdashervcs.server.user;


// TEMP!:
//
// How web auth (not CLI/VCS auth) works:
// - login: given an email & pass, get a token
// - authenticator takes tokens to validate perms/operations
//
// - Token generation?
public interface HdAuthenticator {

    AuthToken login(String email, String password);

    AuthToken webTokenForId(String tokenId);

    AuthToken cliTokenForId(String tokenId);

    AuthResult canAccessRepo(AuthToken authToken, String org, String repo);
}
