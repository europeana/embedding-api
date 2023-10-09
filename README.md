# Europeana Embedding API

Spring-Boot2 wrapper around legacy Python code to generate Embeddings.
The wrapper makes the performance of a single request a bit slower, but makes the API more stable and capable of
processing multiple requests at the same time (at the cost of increasing memory usage)

## Prerequisites
 * Java 17
 * Maven<sup>*</sup> 
 * [Europeana parent pom](https://github.com/europeana/europeana-parent-pom)
 * Python3.6
 
 <sup>* A Maven installation is recommended, but you could use the accompanying `mvnw` (Linux, Mac OS) or `mvnw.cmd` (Windows) 
 files instead.

## For local debugging
Either use the Dockerfile in the python folder or make sure Python 3.6 is installed. 

## Run
The application has a Tomcat web server that is embedded in Spring-Boot.

Either select the `EmbeddingsApplication` class in your IDE and 'run' it

or 

go to the application root where the pom.xml is located and excute  
`./mvnw spring-boot:run` (Linux, Mac OS) or `mvnw.cmd spring-boot:run` (Windows)


## License
Licensed under the EUPL 1.2. For full details, see [LICENSE.md](LICENSE.md).
