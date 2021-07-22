package com.haberdashervcs.common.objects.user;

import java.io.IOException;


public interface HdUserStore {
    void start() throws Exception;

    HdUser getByEmail(String email) throws IOException;

    HdUser getById(String userId) throws IOException;

    void putUser(HdUser user) throws IOException;
}
