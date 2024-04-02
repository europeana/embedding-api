FROM python:3.6-slim
LABEL org.opencontainers.image.vendor="Europeana Foundation" \
      org.opencontainers.image.authors="api@europeana.eu" \
      org.opencontainers.image.documentation="https://pro.europeana.eu/page/apis" \
      org.opencontainers.image.source="https://github.com/europeana/" \
      org.opencontainers.image.licenses="EUPL-1.2"

# Install Python application
WORKDIR /opt/embeddings-python-app

COPY python/embeddings-python/requirements36.txt .

RUN pip3.6 install -r requirements36.txt && python3 -m laserembeddings download-models
COPY python/embeddings-python/default_reduce_model.joblib ./
COPY python/embeddings-python/*.py ./

# Install Java (and curl so we can add health checks)
RUN apt-get update && \
    apt-get -y install openjdk-17-jdk-headless && \
    apt-get -y install curl

# Copy APM agent
ENV ELASTIC_APM_VERSION 1.48.1
ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$ELASTIC_APM_VERSION/elastic-apm-agent-$ELASTIC_APM_VERSION.jar /usr/local/elastic-apm-agent.jar

# Copy Java Embeddings API as a war (user properties doesn't contain sensitive data)
WORKDIR /opt/embedding-api
COPY target/embedding.war .

ENTRYPOINT ["java","-jar","/opt/embedding-api/embedding.war", "--server.port=8080"]