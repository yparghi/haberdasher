package com.haberdashervcs.common.io;


// TODO rename this, ugh.
public class HdObjectId {

    public enum ObjectType {
        FILE,
        FOLDER,
        COMMIT
    }


    private final ObjectType type;
    private final String id;

    public HdObjectId(ObjectType type, String id) {
        this.type = type;
        this.id = id;
    }

    public ObjectType getType() {
        return type;
    }

    public String getId() {
        return id;
    }
}
