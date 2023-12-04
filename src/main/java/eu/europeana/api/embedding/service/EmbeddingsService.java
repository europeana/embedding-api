package eu.europeana.api.embedding.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.embedding.config.EmbeddingSettings;
import eu.europeana.api.embedding.exception.ConfigurationException;
import eu.europeana.api.embedding.exception.ExecutorException;
import eu.europeana.api.recommend.common.model.EmbeddingRequestData;
import eu.europeana.api.recommend.common.model.EmbeddingResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Given one or more records, this service that generates a vector for each record.
 * For each request this launches the python script that generates the embeddings vectors.
 */
@Service
public class EmbeddingsService {

    private static final Logger LOG = LogManager.getLogger(EmbeddingsService.class);

    private EmbeddingSettings settings;
    private ConcurrentLinkedQueue<Executor> executorsFree;
    private List<Executor> executorsBusy;
    private ObjectMapper serializer;

    /**
     * Initialize a new Embeddings Service
     * @param settings application settings
     */
    public EmbeddingsService(EmbeddingSettings settings) {
        this.settings = settings;
        this.executorsFree = new ConcurrentLinkedQueue<>();
        this.executorsBusy = Collections.synchronizedList(new ArrayList<>(settings.getExecutorMaxInstances()));

        this.serializer = new ObjectMapper();
        this.serializer.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    /**
     * We verify if Python 3.6 is installed (we haven't found a way to support Python 3.10 yet)
     * @throws EuropeanaApiException if python3.6 was not found
     */
    @PostConstruct
    public void checkRequirements() throws EuropeanaApiException {
        String pythonVersion = ExecutorUtils.runSimpleCommand("python3.6", "--version");
        if (pythonVersion == null || !pythonVersion.startsWith("Python 3.6")) {
            throw new ConfigurationException("Python 3.6 not found");
        } else {
            LOG.info("Found {}", pythonVersion);
        }

        // launch executors
        LOG.info("Launching {} executors...", settings.getExecutorMaxInstances());
        for (int i = 0; i < settings.getExecutorMaxInstances(); i++) {
            executorsFree.add(new Executor(settings.getExecutorFirstPort() + i, settings.getExecutorRestartAfter(),
                    settings.getExecutorPath()));
        }
        LOG.info("Done launching executors");
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

        // parse data
        String dataJson;
        try {
            dataJson = serializer.writeValueAsString(data);
        } catch (JsonProcessingException jpe) {
            throw new ExecutorException("Error serializing request data", jpe, true);
        }

        // get executor
        if (executorsFree.isEmpty()) {
            throw new ExecutorException("Embedding executor service not available. Maximum number reached: " + settings.getExecutorMaxInstances(), false);
        }
        Executor executor = executorsFree.remove();
        executorsBusy.add(executor);

        // invoke executor
        String output;
        try {
            output = executor.sendData(dataJson, data.getRecords().length);
        } finally {
            executorsBusy.remove(executor);
            executorsFree.add(executor);
        }

        if (output == null) {
            throw new ExecutorException("No output received from Embedding executor service", null, false);
        }
        if (output.toLowerCase(Locale.ROOT).startsWith("{'status': 'error')")) {
            // TODO better parsing of error messages
            throw new ExecutorException(output, null, false);
        }

        // serialize output
        try {
            EmbeddingResponse response = serializer.readValue(output, EmbeddingResponse.class);
            LOG.debug("Result: {} in ms {}", response.getStatus(), System.currentTimeMillis() - start);
            return response;
        } catch (JsonProcessingException jpe) {
            throw new ExecutorException("Error parsing Embedding executor output: " + output, jpe, true);
        }
    }

    @PreDestroy
    private void stopExecutors() throws EuropeanaApiException {
        executorsFree.addAll(executorsBusy);
        while (!executorsFree.isEmpty()) {
            Executor executor = executorsFree.remove();
            executor.destroy();
        }
    }

}
