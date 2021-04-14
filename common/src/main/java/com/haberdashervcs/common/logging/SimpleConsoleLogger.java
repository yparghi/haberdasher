package com.haberdashervcs.common.logging;

import com.google.common.base.Throwables;


// TODO file logging, some basic configuration?
//
// Note: This impl exists because I got sick of Log4j's baffling configuration API, and the need for Slf4j as well.
// I should add a configurable supplier for users' own HdLogger implementations, if they want to use that stuff.
class SimpleConsoleLogger implements HdLogger {

    private final Class clazz;

    SimpleConsoleLogger(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public void debug(String fmt, Object... args) {
        log("DEBUG", String.format(fmt, args));
    }

    @Override
    public void info(String fmt, Object... args) {
        log("INFO", String.format(fmt, args));
    }

    @Override
    public void warn(String fmt, Object... args) {
        log("WARN", String.format(fmt, args));
    }

    @Override
    public void error(String fmt, Object... args) {
        log("ERROR", String.format(fmt, args));
    }

    @Override
    public void exception(Exception ex, String fmt, Object... args) {
        String msg = String.format(fmt, args);
        msg += "\n";
        msg += Throwables.getStackTraceAsString(ex);
        log("ERROR", msg);
    }

    private void log(String level, String msg) {
        String wholeMsg = String.format("[%s] %s %s", this.clazz.getSimpleName(), level, msg);
        System.out.println(wholeMsg);
    }
}
