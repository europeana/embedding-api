# For testing the Embeddings API python code in isolation
FROM python:3.6-slim

WORKDIR /opt/embeddings-python-app

COPY embeddings-python/requirements36.txt .

RUN pip3 install -r requirements36.txt
RUN python3 -m laserembeddings download-models
COPY embeddings-python/default_reduce_model.joblib ./embeddings-python/
COPY embeddings-python/*.py ./embeddings-python/
COPY test-run.sh .

RUN chmod +x ./test-run.sh

ENTRYPOINT ["./test-run.sh"]
