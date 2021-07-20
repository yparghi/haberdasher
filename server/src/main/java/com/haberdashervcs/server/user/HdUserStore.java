package com.haberdashervcs.server.user;

import java.io.IOException;


// TODO HBase! One table, multiple cfs, for look by email vs. userid?
public interface HdUserStore {
    void start() throws Exception;

    HdUser getByEmail(String email) throws IOException;

    HdUser getById(String userId) throws IOException;
}
