package eu.europeana.api.embedding.web;

import eu.europeana.api.embedding.service.EmbeddingsService;
import eu.europeana.api.recommend.common.model.EmbeddingRequestData;
import eu.europeana.api.recommend.common.model.RecordVectors;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest Controller that mimics the original Embedding API endpoint
 * (see https://bitbucket.org/jhn-ngo/recsy-xx/src/master/src/engines/encoders/europeana-embeddings-api/)
 */
@RestController
public class EmbeddingsController {

    private EmbeddingsService embeddingsService;

    public EmbeddingsController(EmbeddingsService embeddingsService) {
        this.embeddingsService = embeddingsService;
    }

    /**
     * Here we mimic the original Embeddings API endpoint
     */
    @PostMapping(value = "/embedding_api/embeddings", produces = MediaType.APPLICATION_JSON_VALUE)
    public RecordVectors embeddings(@RequestBody EmbeddingRequestData embeddingRequestData) {
        return embeddingsService.generateEmbeddings(embeddingRequestData);
    }

}
