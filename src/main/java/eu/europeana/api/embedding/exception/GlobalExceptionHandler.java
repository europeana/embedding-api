package eu.europeana.api.embedding.exception;

import eu.europeana.api.commons_sb3.error.EuropeanaGlobalExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Add a class with the @ControllerAdvice annotation that extends the EuropeanaGlobalExceptionHandler
 * Basic error processing (whether to log and error or not, plus handle ConstraintViolations) is done in the
 * EuropeanaGlobalExceptionHandler, but you can add more error handling here
 */
@ControllerAdvice
public class GlobalExceptionHandler extends EuropeanaGlobalExceptionHandler {



}
