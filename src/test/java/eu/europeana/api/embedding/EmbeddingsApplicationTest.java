package eu.europeana.api.embedding;

import eu.europeana.api.embedding.service.EmbeddingsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Basic test for loading context
 */
@SpringBootTest
class EmbeddingsApplicationTest {

    @MockBean
    private EmbeddingsService embeddingsService;

    @SuppressWarnings("squid:S2699") // we are aware that this test doesn't have any assertion
    @Test
    void contextLoads() {
    }

}
