package eu.europeana.api.embedding.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Setup CORS for all requests and setup default Content-type
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Setup CORS for all SpringDoc, requests.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/v3/api-docs")
                .allowedOrigins("*")
                .allowedMethods(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name())
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(1000L); // in seconds
    }

    /*
     * Enable content negotiation via path extension (as long as Spring supports it) and set default content type in
     * case we receive a request without an extension or Accept header
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // Enable content negotiation via path extension. Note that this is deprecated with Spring 5.2.4,
        // (see also https://github.com/spring-projects/spring-framework/issues/24179), so it may not work in future
        // releases
        configurer.favorPathExtension(true);

        // set json as default answer, even if no accept header or extension was provided
        configurer.defaultContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE), MediaType.APPLICATION_JSON);
    }
}
