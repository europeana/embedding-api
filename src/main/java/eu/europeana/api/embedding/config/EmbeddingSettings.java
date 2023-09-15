package eu.europeana.api.embedding.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;

/**
 * Container for all settings that we load from the embedding.properties file and optionally override from
 * embedding.user.properties file
 */
@Configuration
@PropertySource("classpath:embedding.properties")
@PropertySource(value = "classpath:embedding.user.properties", ignoreResourceNotFound = true)
public class EmbeddingSettings {

    private static final Logger LOG = LogManager.getLogger(EmbeddingSettings.class);

    @Value("${python.timeout}")
    private String pythonTimeout;

    @PostConstruct
    private void logImportantSettings() {
        LOG.info("Embedding API settings:");
        LOG.info("  Python timeout: {}", pythonTimeout);
    }

    public String getPythonTimeout() {
        return pythonTimeout;
    }


}
