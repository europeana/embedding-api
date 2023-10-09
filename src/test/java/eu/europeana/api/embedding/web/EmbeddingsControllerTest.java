package eu.europeana.api.embedding.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.api.embedding.service.EmbeddingsService;
import eu.europeana.api.recommend.common.model.EmbeddingRecord;
import eu.europeana.api.recommend.common.model.EmbeddingRequestData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit test for testing the RecommendController class
 */
@SpringBootTest
@AutoConfigureMockMvc
public class EmbeddingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    EmbeddingsService embeddingsService;

    @Test
    public void testValidInput() throws Exception {
        EmbeddingRequestData content = new EmbeddingRequestData(new EmbeddingRecord[0]);
        var mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(content);

        // with accept header
        mockMvc.perform(post("/embedding_api/embeddings")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(HttpStatus.OK.value()));

        // without accept header
        mockMvc.perform(post("/embedding_api/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testEmptyPost() throws Exception {
        // with accept header
        mockMvc.perform(post("/embedding_api/embeddings")
                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }


}
