package com.haberdashervcs.common.objects.user;

import java.io.IOException;
import java.util.Optional;


public interface HdAuthenticator {

    void start() throws Exception;

    Optional<UserAuthToken> loginToWeb(String email, String password) throws IOException;

    UserAuthToken webTokenForId(String tokenId) throws IOException;

    UserAuthToken cliTokenForId(String tokenId) throws IOException;

    AuthResult canAccessRepo(UserAuthToken authToken, String org, String repo);
}
