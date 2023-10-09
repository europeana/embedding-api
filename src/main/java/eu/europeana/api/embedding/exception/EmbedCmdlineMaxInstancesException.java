package eu.europeana.api.embedding.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Throw when we can't launch a new Embedding command line python instances because we have reached to maximum
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class EmbedCmdlineMaxInstancesException extends EuropeanaApiException {

    /**
     * Initialise a new exception for which there is no root cause
     * @param msg error message
     */
    public EmbedCmdlineMaxInstancesException(String msg) {
        super(msg);
    }

    /**
     * We don't want to log the stack trace for this exception
     * @return false
     */
    @Override
    public boolean doLogStacktrace() {
        return false;
    }

}
