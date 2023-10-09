package eu.europeana.api.embedding.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Throw when there is a problem running the Embeddings API command-line python code
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class EmbedCmdlineException extends EuropeanaApiException {

    private final boolean logStacktrace;
    /**
     * Initialise a new exception with root cause exception
     * @param msg error message
     * @param t root cause exception
     * @param logStacktrace boolean indicating whether the stacktrace should be logged or not
     */
    public EmbedCmdlineException(String msg, Throwable t, boolean logStacktrace) {
        super(msg, t);
        this.logStacktrace = logStacktrace;
    }

    /**
     * In some cases we want to log the stack trace for this exception, but not in others. This method is to inform
     * the GlobalExceptionHandler
     * @return true if we want to log the stacktrace for this
     */
    @Override
    public boolean doLogStacktrace() {
        return logStacktrace;
    }

}
