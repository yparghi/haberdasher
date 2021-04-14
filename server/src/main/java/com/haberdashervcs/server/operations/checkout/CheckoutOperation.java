package com.haberdashervcs.server.operations.checkout;

import static com.google.common.base.Preconditions.checkNotNull;


public final class CheckoutOperation {

    private final String org;
    private final String repo;
    private final String path;
    private final String commit;

    public CheckoutOperation(String org, String repo, String path, String commit) {
        this.org = checkNotNull(org);
        this.repo = checkNotNull(repo);
        this.path = checkNotNull(path);
        this.commit = checkNotNull(commit);
    }
}
