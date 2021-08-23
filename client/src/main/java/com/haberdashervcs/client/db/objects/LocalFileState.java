package com.haberdashervcs.client.db.objects;


public class LocalFileState {

    public static LocalFileState withPushedToServerState(boolean pushedToServer) {
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
