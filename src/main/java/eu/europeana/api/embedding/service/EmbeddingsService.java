package eu.europeana.api.embedding.service;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.embedding.config.EmbeddingSettings;
import eu.europeana.api.embedding.exception.ConfigurationException;
import eu.europeana.api.recommend.common.model.EmbeddingRequestData;
import eu.europeana.api.recommend.common.model.RecordVectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Given one or more records, this service that generates a vector for each record.
 * For each request this launches the python script that generates the embeddings vectors.
 */
@Service
public class EmbeddingsService {

    private static final Logger LOG = LogManager.getLogger(EmbeddingsService.class);

    private EmbeddingSettings settings;

    public EmbeddingsService(EmbeddingSettings settings) {
        this.settings = settings;
    }

    /**
     * Check if python3 is installed
     * @throws EuropeanaApiException if python was not found
     */
    @PostConstruct
    public void checkRequirements() throws EuropeanaApiException {
        String output = null;
        try {
            output = new ProcessExecutor().command("python3", "--version")
                    .readOutput(true).execute()
                    .outputUTF8();
        } catch (IOException | InterruptedException | TimeoutException e) {
            LOG.error("Error checking if python3 is installed", e);
        }
        if (output == null || !output.contains("Python 3")) {
            throw new ConfigurationException("Python3 not found");
        } else {
            LOG.info("Found {}", output);
        }
    }

    public RecordVectors generateEmbeddings(EmbeddingRequestData data) {
        // TODO implement
        return null;
    }


}
