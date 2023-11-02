package eu.europeana.api.embedding.exception;

import eu.europeana.api.commons.error.EuropeanaGlobalExceptionHandler;
import io.micrometer.core.instrument.util.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Add a class with the @ControllerAdvice annotation that extends the EuropeanaGlobalExceptionHandler
 * Basic error processing (whether to log and error or not, plus handle ConstraintViolations) is done in the
 * EuropeanaGlobalExceptionHandler, but you can add more error handling here
 */
@ControllerAdvice
public class GlobalExceptionHandler extends EuropeanaGlobalExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(GlobalExceptionHandler.class);

    /**
     * Make sure we return a 400 response when too much data is sent
     * @param e caught {@link EmbedCmdlineException}
     * @param response the response of the failing request
     * @throws EmbedCmdlineException if there's an error sending back the response
     */
    @ExceptionHandler
    public void handleMissingAuthHeader(EmbedCmdlineException e, HttpServletResponse response) throws EmbedCmdlineException {
        if (e.getMessage() != null && e.getMessage().contains("too long")) {
            try {
                response.sendError(HttpStatus.BAD_REQUEST.value(), StringEscapeUtils.escapeJson(e.getMessage()));
            } catch (IOException ioe) {
                LOG.error("Error handling exception", ioe);
            }
        }
        throw e;
    }


}
