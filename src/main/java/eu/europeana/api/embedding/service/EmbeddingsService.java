package eu.europeana.api.embedding.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.embedding.config.EmbeddingSettings;
import eu.europeana.api.embedding.exception.ConfigurationException;
import eu.europeana.api.embedding.exception.EmbedCmdlineException;
import eu.europeana.api.embedding.exception.EmbedCmdlineMaxInstancesException;
import eu.europeana.api.recommend.common.model.EmbeddingRequestData;
import eu.europeana.api.recommend.common.model.EmbeddingResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Given one or more records, this service that generates a vector for each record.
 * For each request this launches the python script that generates the embeddings vectors.
 */
@Service
public class EmbeddingsService {

    private static final Logger LOG = LogManager.getLogger(EmbeddingsService.class);

    private EmbeddingSettings settings;
    private List<ProcessExecutor> executors;
    private ObjectMapper serializer;

    /**
     * Initialize a new Embeddings Service
     * @param settings application settings
     */
    public EmbeddingsService(EmbeddingSettings settings) {
        this.settings = settings;
        this.executors = Collections.synchronizedList(new ArrayList<ProcessExecutor>(settings.getEmbedCmdMaxInstances()));
        this.serializer = new ObjectMapper();
        this.serializer.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    /**
     * For local debugging we do a number of checks and actions:
     * <ul>
     * <li>Is python3 installed</li>
     * </ul>
     * @throws EuropeanaApiException if python3 was not found
     */
    @PostConstruct
    public void checkRequirements() throws EuropeanaApiException {
        checkPythonVersion();
    }

    @SuppressWarnings("java:S2142") // no need to warn for no handling InterruptedException as the executor will
    // clean up after itself
    private String runSimpleCommand(String... commands) {
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

    /**
     * At the moment the python code requires Python 3.6. So far we didn't manage to get it running with Python 3.10
     * @throws EuropeanaApiException
     */
    private void checkPythonVersion() throws EuropeanaApiException {
        String pythonVersion = runSimpleCommand("python3.6", "--version");
        if (pythonVersion == null || !pythonVersion.startsWith("Python 3.6")) {
            throw new ConfigurationException("Python 3.6 not found");
        } else {
            LOG.info("Found {}", pythonVersion);
        }
    }

    /**
     * Return an object containing the vectors for the provided data
     * @param data the data for which to generate vectors
     * @return EmbeddingResponse object
     * @throws EuropeanaApiException if there's a problem generating the vectors
     */
    @SuppressWarnings("java:S2142") // no need to warn for no handling InterruptedException as the executor will
     // clean up after itself
    public EmbeddingResponse generateEmbeddings(EmbeddingRequestData data) throws EuropeanaApiException {
        long start = System.currentTimeMillis();
        String output = null;
        ProcessExecutor executor = null;
        try {
            executor = createEmbedApplication(data);
            executors.add(executor);
            LOG.debug("Starting embedding executing...");
            output = executor.execute().outputUTF8();
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new EmbedCmdlineException("Error running Embedding service: " + output, e, true);
        } catch (InvalidExitValueException e) {
            throw new EmbedCmdlineException("Error running Embedding service: " + output + " (exit code = " + e.getExitValue() + ")", e, false);
        } finally {
            if (executor != null) {
                executors.remove(executor);
            }
        }
        LOG.trace("Result = {}", output);
        if (output == null) {
            throw new EmbedCmdlineException("No output received from Embedding service", null, false);
        }
        if (output.toUpperCase(Locale.ROOT).startsWith("ERROR")) {
            throw new EmbedCmdlineException(output, null, false);
        }

        // Serialize output
        try {
            EmbeddingResponse response = serializer.readValue(output, EmbeddingResponse.class);
            LOG.debug("Result: {} in ms {}", response.getStatus(), System.currentTimeMillis() - start);
            return response;
        } catch (JsonProcessingException jpe) {
            throw new EmbedCmdlineException("Error parsing Embedding service output: " + output, jpe, true);
        }
    }

    private ProcessExecutor createEmbedApplication(EmbeddingRequestData data) throws EuropeanaApiException {
        long start = System.currentTimeMillis();
        String dataInput;
        try {
            dataInput = serializer.writeValueAsString(data);
        } catch (JsonProcessingException jpe) {
            throw new EmbedCmdlineException("Error serializing request data", jpe, true);
        }
        LOG.debug("Parsed data in {} ms", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        int nrExecutors = executors.size();
        LOG.debug("Currently {} executors working", nrExecutors);
        if (nrExecutors < settings.getEmbedCmdMaxInstances()) {
            LOG.debug("Creating new embedding process...");
            ProcessExecutor result = new ProcessExecutor()
                    .directory(new File(settings.getEmbedCmdPath()))
                    .command("python3.6", "./embeddings-commandline/europeana_embeddings_cmd.py", "--data=" + dataInput)
                    .timeout(settings.getEmbedCmdTimeout(), TimeUnit.SECONDS)
                    .redirectError(Slf4jStream.of(getClass()).asError())
                    .readOutput(true)
                    .exitValue(0)
                    .destroyOnExit();
            LOG.debug("  Process created in {} ms", System.currentTimeMillis() - start);
            return result;
        }
        throw new EmbedCmdlineMaxInstancesException("Embedding service not available. Maximum number of instances reached: " + nrExecutors);
    }

}
