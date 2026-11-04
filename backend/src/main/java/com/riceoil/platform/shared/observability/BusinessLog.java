package com.riceoil.platform.shared.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Structured business-event logging: {@code event=ORDER_PLACED orderNumber=ORD-1 total=100.00}.
 * Every line also carries correlationId / customerId / sessionId through the MDC (see logging.pattern),
 * so a customer journey from login to logout can be traced with a single grep.
 * Never pass secrets, signatures, passwords or tokens here.
 */
public final class BusinessLog {

    private static final Logger LOG = LoggerFactory.getLogger("business");

    private BusinessLog() {
    }

    public static void info(String event, Object... keyValues) {
        if (LOG.isInfoEnabled()) {
            LOG.info(format(event, keyValues));
        }
    }

    public static void warn(String event, Object... keyValues) {
        if (LOG.isWarnEnabled()) {
            LOG.warn(format(event, keyValues));
        }
    }

    public static void error(String event, Throwable cause, Object... keyValues) {
        if (LOG.isErrorEnabled()) {
            LOG.error(format(event, keyValues), cause);
        }
    }

    /** Keeps the domain part of the email for diagnosis but hides the mailbox name. */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    static String format(String event, Object... keyValues) {
        StringBuilder line = new StringBuilder("event=").append(event);
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            if (value == null) {
                continue;
            }
            line.append(' ').append(keyValues[i]).append('=').append(escape(String.valueOf(value)));
        }
        return line.toString();
    }

    private static String escape(String value) {
        String clean = value.replace('\r', ' ').replace('\n', ' ');
        boolean needsQuotes = clean.isEmpty() || clean.chars().anyMatch(c -> c == ' ' || c == '"' || c == '=');
        return needsQuotes ? '"' + clean.replace("\"", "\\\"") + '"' : clean;
    }
}
