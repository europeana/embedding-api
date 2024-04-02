package eu.europeana.api.embedding.exception;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Throw when there is a problem with the configuration (e.g. python3 is not installed)
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ConfigurationException extends EuropeanaApiException {

    /**
     * Initialise a new exception for which there is no root cause
     * @param msg error message
     */
    public ConfigurationException(String msg) {
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
