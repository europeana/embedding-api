package eu.europeana.api.embedding.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JUnit test for testing if the /info and /health actuator endpoints are available
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ActuatorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testActuatorInfo() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/info"))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();
        // also check that there are contents
        assert result.getResponse().getContentAsString().contains("app");
    }

    @Test
    public void testActuatorHealth() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();
        // also check that there are contents
        assert result.getResponse().getContentAsString().contains("UP");
    }

    @Test
    public void testActuatorHealthLiveness() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();
        // also check that there are contents
        assert result.getResponse().getContentAsString().contains("UP");
    }

    @Test
    public void testActuatorHealthReadiness() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().is(HttpStatus.OK.value())).andReturn();
        // also check that there are contents
        assert result.getResponse().getContentAsString().contains("UP");
    }

}
