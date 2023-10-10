FROM python:3.6-slim

# Install Python application
WORKDIR /opt/embeddings-python-app

COPY /python/requirements36.txt .

RUN pip3.6 install -r requirements36.txt && python3 -m laserembeddings download-models
COPY python/embeddings-commandline/default_reduce_model.joblib ./embeddings-commandline/
COPY python/embeddings-commandline/europeana_embeddings_cmd.py ./embeddings-commandline/

# Install Java
RUN apt-get update && apt-get -y install openjdk-17-jdk-headless

# Copy APM agent
ENV ELASTIC_APM_VERSION 1.34.1
ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$ELASTIC_APM_VERSION/elastic-apm-agent-$ELASTIC_APM_VERSION.jar /usr/local/elastic-apm-agent.jar

# Copy Java Embeddings API as a war (user properties doesn't contain sensitive data)
WORKDIR /opt/embedding-api
COPY target/embedding.war/ .

ENTRYPOINT ["java","-jar","/opt/embedding-api/embedding.war", "--server.port=8080"]