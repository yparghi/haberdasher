package com.haberdashervcs.common.logging;


// Since I don't want concrete logging dependencies in every class (be they slf4j or log4j or whatever), I wrap them
// here.
public final class HdLoggers {

    public static HdLogger create(Class myClass) {
        return fromLog4j(myClass);
    }

    private static HdLogger fromLog4j(Class myClass) {
        return new Log4JHdLogger(myClass);
    }

    private HdLoggers() {}
}
