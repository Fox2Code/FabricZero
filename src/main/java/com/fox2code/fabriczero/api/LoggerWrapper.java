package com.fox2code.fabriczero.api;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;

public class LoggerWrapper extends ExtendedLoggerWrapper implements Logger {
    public LoggerWrapper(Logger logger) {
        super((ExtendedLogger) logger, logger.getName(), logger.getMessageFactory());
    }
}
