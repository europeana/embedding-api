package eu.europeana.api.embedding.exception;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Throw when there is no executor available (all are in use
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class NoExecutorAvailableException extends EuropeanaApiException {

    /**
     * Initialise a new exception with root cause exception
     * @param maxInstances the maximum number of instances allowed
     */
    public NoExecutorAvailableException(int maxInstances) {
        super("Embedding executor service not available. Maximum number reached: " + maxInstances);
    }

    @Override
    public boolean doLogStacktrace() {
        return false;
    }
}
