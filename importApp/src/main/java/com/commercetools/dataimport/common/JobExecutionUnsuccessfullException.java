package com.commercetools.dataimport.common;

public class JobExecutionUnsuccessfullException extends RuntimeException {
    public JobExecutionUnsuccessfullException() {
    }

    public JobExecutionUnsuccessfullException(final Throwable cause) {
        super(cause);
    }

    public JobExecutionUnsuccessfullException(final String message) {
        super(message);
    }

    public JobExecutionUnsuccessfullException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public JobExecutionUnsuccessfullException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
