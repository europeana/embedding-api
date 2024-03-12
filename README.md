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

## Run
The application has a Tomcat web server that is embedded in Spring-Boot.

Either select the `EmbeddingsApplication` class in your IDE and 'run' it

or 

go to the application root where the pom.xml is located and excute  
`./mvnw spring-boot:run` (Linux, Mac OS) or `mvnw.cmd spring-boot:run` (Windows)


## For local debugging
Launch a Python process manually. For this either use the Dockerfile in the `python` folder or make sure Python 3.6 is installed.
When using Docker to launch Python:
1. Don't forget to map the port specified in the test-run.sh file.
2. In the `Executor` class modify the 127.0.0.1 address to the IP of the Docker container and comment out the `createProcess` method. 
3. In the `EmbeddingsService` class, comment out the Python 3.6 check in the `checkRequirements` method.

## License
Licensed under the EUPL 1.2. For full details, see [LICENSE.md](LICENSE.md).
