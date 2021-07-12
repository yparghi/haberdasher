package com.haberdashervcs.client.db.objects;


public class LocalFileState {

    public static LocalFileState of(boolean pushedToServer) {
        return new LocalFileState(pushedToServer);
    }


    private final boolean pushedToServer;

    private LocalFileState(boolean pushedToServer) {
        this.pushedToServer = pushedToServer;
    }

    public boolean isPushedToServer() {
        return pushedToServer;
    }
}
