package eu.europeana.api.embedding.config;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.embedding.exception.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
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


    @Value("${executor.path}")
    private String executorPath;
    @Value("${executor.max.instance}")
    private int executorMaxInstances;
    @Value("${executor.port}")
    private int executorFirstPort;

    @Value("${executor.restart.after}")
    private int executorRestartAfter;


    @PostConstruct
    private void logImportantSettings() throws EuropeanaApiException {
        executorPath = trimAndAppendSlashIfNecessary("embedcmd.path", executorPath);
        LOG.info("Embedding API settings:");
        LOG.info("  Executor directory: {}", executorPath);
        LOG.info("  Executor max instances: {}", executorMaxInstances);
        LOG.info("  Executor ports: {} to {}", executorFirstPort, executorFirstPort + executorMaxInstances - 1);
        LOG.info("  Executor restart after: {} processed records", executorRestartAfter);
    }

    public String getExecutorPath() {
        return this.executorPath;
    }

    private String trimAndAppendSlashIfNecessary(String keyName, String value) throws EuropeanaApiException {
        if (StringUtils.isBlank(value)) {
            throw new ConfigurationException("Configuration option '" + keyName + "' not set!");
        }
        return value.trim() + (value.endsWith("/") ? "" : "/");
    }

    public int getExecutorMaxInstances() {
        return executorMaxInstances;
    }

    public int getExecutorFirstPort() {
        return executorFirstPort;
    }

    public int getExecutorRestartAfter() {
        return executorRestartAfter;
    }
}
