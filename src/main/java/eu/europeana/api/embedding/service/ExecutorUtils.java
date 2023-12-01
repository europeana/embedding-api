package eu.europeana.api.embedding.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Helper class for starting new processes
 */
public final class ExecutorUtils {

    private static final Logger LOG = LogManager.getLogger(ExecutorUtils.class);

    private ExecutorUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Run a command and return its output
     * @param commands command with extra parameters
     * @return process output
     */
    @SuppressWarnings("java:S2142") // no need to warn for no handling InterruptedException as the executor will
    // clean up after itself
    public static String runSimpleCommand(String... commands) {
        String result = null;
        try {
            result = new ProcessExecutor().command(commands)
                    .readOutput(true)
                    .exitValue(0)
                    .execute()
                    .outputUTF8();
        } catch (IOException | InterruptedException | TimeoutException e) {
            LOG.error("Error running commands {}", commands, e);
        }
        LOG.debug("Command {} result = {}", commands, result);
        return result;
    }
}
