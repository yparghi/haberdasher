package com.haberdashervcs.common.objects.user;

import java.io.IOException;
import java.util.Optional;


// TEMP!:
//
// How web auth (not CLI/VCS auth) works:
// - login: given an email & pass, get a token
// - authenticator takes tokens to validate perms/operations
//
// - Token generation?
public interface HdAuthenticator {

    void start() throws Exception;

    Optional<AuthToken> loginToWeb(String email, String password) throws IOException;

    AuthToken webTokenForId(String tokenId);

    AuthToken cliTokenForId(String tokenId);

    AuthResult canAccessRepo(AuthToken authToken, String org, String repo);
}
