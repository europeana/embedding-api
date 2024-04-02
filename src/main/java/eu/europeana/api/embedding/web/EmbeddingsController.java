package eu.europeana.api.embedding.web;

import eu.europeana.api.commons_sb3.error.EuropeanaApiException;
import eu.europeana.api.embedding.service.EmbeddingsService;
import eu.europeana.api.recommend.common.model.EmbeddingRequestData;
import eu.europeana.api.recommend.common.model.EmbeddingResponse;
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
     * @param embeddingRequestData data to process
     * @return EmbeddingsResponse object
     * @throws EuropeanaApiException if there is a problem generating embeddings
     */
    @PostMapping(value = "/embedding_api/embeddings", produces = MediaType.APPLICATION_JSON_VALUE)
    public EmbeddingResponse embeddings(@RequestBody EmbeddingRequestData embeddingRequestData) throws EuropeanaApiException {
        return embeddingsService.generateEmbeddings(embeddingRequestData);
    }

}
