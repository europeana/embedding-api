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


    @Value("${embedcmd.path}")
    private String embedCmdPath;
    @Value("${embedcmd.timeout:60}")
    private long embedCmdTimeout;

    @Value("${embedcmd.max.instance:3}")
    private int embedCmdMaxInstances;

    @PostConstruct
    private void logImportantSettings() throws EuropeanaApiException {
        embedCmdPath = trimAndAppendSlashIfNecessary("embedcmd.path", embedCmdPath);
        LOG.info("Embedding API settings:");
        LOG.info("  Embed cmdline path: {}", embedCmdPath);
        LOG.info("  Embed cmdline timeout: {}", embedCmdTimeout);
        LOG.info("  Embed cmdline max instances: {}", embedCmdMaxInstances);
    }

    public String getEmbedCmdPath() {
        return this.embedCmdPath;
    }

    private String trimAndAppendSlashIfNecessary(String keyName, String value) throws EuropeanaApiException {
        if (StringUtils.isBlank(value)) {
            throw new ConfigurationException("Configuration option '" + keyName + "' not set!");
        }
        return value.trim() + (value.endsWith("/") ? "" : "/");
    }


    public long getEmbedCmdTimeout() {
        return embedCmdTimeout;
    }

    public int getEmbedCmdMaxInstances() {
        return embedCmdMaxInstances;
    }
}
