package com.haberdashervcs.client.commands;

import java.util.List;

public class CheckoutCommand implements Command {

    private final List<String> otherArgs;

    CheckoutCommand(List<String> otherArgs) {
        this.otherArgs = otherArgs;
    }

    @Override
    public void perform() {
        // TODO
    }
}
