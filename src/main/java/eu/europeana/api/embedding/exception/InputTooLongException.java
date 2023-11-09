package eu.europeana.api.embedding.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Throw when there's a problem with the received data, e.g. unparsable json or we are receiving more data than we
 * can handle
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InputTooLongException extends EuropeanaApiException {


    /**
     * Initialise a new exception with root cause exception
     * @param msg error message
     * @param t root cause exception
     */
    public InputTooLongException(String msg, Throwable t) {
        super(msg, t);
    }

    /**
     * In some cases we want to log the stack trace for this exception, but not in others. This method is to inform
     * the GlobalExceptionHandler
     * @return true if we want to log the stacktrace for this
     */
    @Override
    public boolean doLogStacktrace() {
        return false;
    }

}
