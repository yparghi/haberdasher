package com.haberdashervcs.client.db.objects;


public final class LocalRepoState {

    public static LocalRepoState forState(State state) {
        return new LocalRepoState(state);
    }

    public enum State {
        NORMAL,
        REBASE_IN_PROGRESS
    }


    private final State state;

    private LocalRepoState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }
}
