package me.valkeea.fishyaddons.vconfig.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VCLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("VConfig");
    private static final String INSERT = "[{}] {}";

    public static void debug(Class<?> origin, String message) {
        LOGGER.debug(INSERT, origin.getSimpleName(), message);
    }

    public static void info(Class<?> origin, String message) {
        LOGGER.info(INSERT, origin.getSimpleName(), message);
    }

    public static void warn(Class<?> origin, String message) {
        LOGGER.warn(INSERT, origin.getSimpleName(), message);
    }

    public static void error(Class<?> origin, Exception e, String message) {
        LOGGER.error(INSERT, origin.getSimpleName(), message, e);
    }

    private VCLogger() {}
}
