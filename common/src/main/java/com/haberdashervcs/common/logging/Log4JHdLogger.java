package com.haberdashervcs.common.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;


public class Log4JHdLogger implements HdLogger {

    private final Logger log;

    Log4JHdLogger(Class myClass) {
        log = LogManager.getLogger(myClass);
    }

    @Override
    public void debug(String fmt, Object... args) {
        log.debug(fmt, args);
    }

    @Override
    public void info(String fmt, Object... args) {
        log.info(fmt, args);
    }

    @Override
    public void warn(String fmt, Object... args) {
        log.warn(fmt, args);
    }

    @Override
    public void error(String fmt, Object... args) {
        log.error(fmt, args);
    }

    @Override
    public void exception(Exception ex, String fmt, Object... args) {
        Message msg = log.getMessageFactory().newMessage(fmt, args);
        log.error(msg, ex);
    }
}
