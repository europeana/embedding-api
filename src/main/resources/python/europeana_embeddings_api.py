#
# Copyright (c) 2021 Anacode GmbH.
#
# This file is part of Europeana XX.
# See https://pro.europeana.eu/project/europeana-xx for further info.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import logging
import os
import psutil
import gc
import time
import argparse
import traceback

from waitress import serve
import joblib
import numpy as np
from flask import Flask, request, abort
from flask_cors import CORS
from flask_restful import Resource, Api, reqparse
from laserembeddings import Laser
from gevent.lock import Semaphore
from sys import stdout

try:
    parser = argparse.ArgumentParser(description='Argument parser for the Embeddings API')
    parser.add_argument('--reload_after', type=str, help='After how many records should the model be reloaded', default="1000")
    parser.add_argument('--webserver', type=str, help='Webserver to use. The possible values are flask, waitress, gevent. Default: gevent', default="gevent")
    args, unknown = parser.parse_known_args()
    RELOAD_AFTER = int(args.reload_after)
    WEBSERVER = args.webserver
except:
    RELOAD_AFTER = 1000
    WEBSERVER = 'gevent'

PORT = 4995
LOG_DIR = "logs"

SEMAPHORE = Semaphore()

workingdirectory = os.getcwd()
print('Working directory:', workingdirectory)

if not os.path.exists(LOG_DIR):
    os.mkdir(LOG_DIR)
os.environ['CUDA_VISIBLE_DEVICES'] = '0'


PROCESS = __file__.split("/")[-1].split(".")[0]
logger = logging.getLogger(PROCESS)
PID = psutil.Process(os.getpid())
logger.setLevel(logging.DEBUG) # set logger level
logFormatter = logging.Formatter("%(name)-12s %(asctime)s %(levelname)-8s %(filename)s:%(funcName)s %(message)s")
consoleHandler = logging.StreamHandler(stdout) #set streamhandler to stdout
consoleHandler.setFormatter(logFormatter)
logger.addHandler(consoleHandler)


def print_memory(suffix=""):
    return ('Current memory usage {}: {}'.format(suffix, PID.memory_info().rss))


class LaserModelWithReload():
    """
    This is a wrapper for the Laser model. It reloads the model after a specified number of calls to prevent memory leak.
    """

    def __init__(self, reload_after=1000):
        self.model = Laser()
        self.reload_after = reload_after
        self.n_calls = 0

    def update(self, n_calls):
        self.n_calls += n_calls
        if self.n_calls >= self.reload_after:
            logger.info("Reloading Laser model after >= {} calls.".format(self.reload_after))
            start = time.time()
            self.model = None
            self.model = Laser()
            end = time.time()
            logger.info("Reloaded Laser model in {} sec.".format(abs(start - end)))
            self.n_calls = 0


logger.info("Starting process {} with reload_after={}...".format(PROCESS, RELOAD_AFTER))
logger.info(print_memory())
LASER = LaserModelWithReload(reload_after=RELOAD_AFTER)
REDUCE_MODEL = joblib.load("europeana-embeddings-api/default_reduce_model.joblib")
#REDUCE_MODEL = joblib.load("default_reduce_model.joblib")
logger.info(print_memory("after loading models"))


def compute_similarity(v1, v2, measure="cosine"):
    """
    Computes the distance between two vectors.
    :param v1: first vector
    :param v2: second vector
    :param measure: distance measure; possible values: cosine, dotproduct, euclidean
    :return:
    """
    if measure == "cosine":
        n1 = np.linalg.norm(v1)
        n2 = np.linalg.norm(v2)
        return float("{:.4f}".format(np.dot(v1, v2) / n1 / n2))
    if measure == "dotproduct":
        return np.dot(v1, v2)
    if measure == "euclidean":
        return np.linalg.norm(v1 - v2)


def normalize_vector(vector):
    """
    Normalizes the provided vector to unit length.
    :param vector
    :return: array of same dimensionality as input vector, normalized to unit length.
    """
    return vector / np.linalg.norm(vector)


# this map is used to normalize field names to our unified format
FIELD_MAP = {
    "country": "places",
    'edmPlaceLabel': "places",
    'edmPlaceLabelLangAware': "places",
    "dcCreator": "creator",
    # "dcDescription": "description",
    "description": "description",
    "dcDescriptionLangAware": "description",
    "title": "title",
    "dcTitleLangAware": "title",
    "edmConceptPrefLabelLangAware": "tags"
}

# for multilingual fields, this list defines the priority order of languages to use
LANGUAGES_BY_PRIORITY = ["en", "de", "fr", "es", "def"]


def clean_textlist(textlist):
    # removes duplicates and empty strings
    return list(dict.fromkeys([k.strip() for k in ", ".join(textlist).split(", ") if k.strip()]))


def get_value_with_best_language(field_value,
                                 languages_by_priority=LANGUAGES_BY_PRIORITY):
    """
    For a multilingual field, this function returns the value in the highest-priority language available.
    :param field_value: Value of a multilingual field, which is a language:value dictionary or a list of such dictionaries.
    :param languages_by_priority: priority order of languages to use
    :return: value in highest-priotity language, highest-priotity language
    """
    if isinstance(field_value, dict):
        logging.debug("is dict")
        for lang in languages_by_priority:
            if lang in field_value:
                return ",  ".join(field_value[lang]), lang
        return ", ".join(list(field_value.items())[0][1]), list(field_value.items())[0][0]
    if isinstance(field_value, list):
        logging.debug("is list")
        langs = [list(item.keys())[0] for item in field_value]
        for lang in languages_by_priority:
            if lang in langs:
                return list(field_value[langs.index(lang)].values())[0], lang
        return list(field_value[0].items())[1], list(field_value[0].items())[0]


def transform_record(record, return_format="string", field_map=FIELD_MAP):
    """
    This function transforms the original record into a tuple (record_id, text string or list).
    :param record: original Europeana record
    :param return_format: possible values: string, text
    :return: tuple (record_id, text string or list)
    """
    new_record = {k: [] for k in set(field_map.values())}
    new_record["id"] = [record["id"]]
    for k in FIELD_MAP:
        if k in record:
            if isinstance(record[k], str):
                new_record[field_map[k]].append(record[k])
            elif isinstance(record[k], list) and record[k] and isinstance(record[k][0], str):
                new_record[field_map[k]].extend(record[k])
            else:
                try:
                    new_record[field_map[k]].append(get_value_with_best_language(record[k])[0])
                except:
                    pass
    for f in new_record:
        new_record[f] = ", ".join(new_record[f])
    new_record["description"] = new_record["description"][:300]
    if return_format == "string":
        return new_record["id"], ", ".join(clean_textlist([new_record[k] for k in sorted(set(FIELD_MAP.values()))]))
    else:
        return new_record["id"], clean_textlist([new_record[k] for k in sorted(set(FIELD_MAP.values()))])


def process_records(records_with_reduced_structure,
                    steps=["laser", "reduce", "normalize"]):
    """
    This function takes as input a list of records and transforms them to embeddings.
    :param records_with_reduced_structure: list of records
    :param steps: there are three steps to transform the records to the final embeddings:
      - laser: embed the records using the Laser model (embedding dimensionality: 1024)
      - reduce: reduce record dimensionality using Europeana model (output embedding dimensionality: 300)
      - normalize: normalize the embedding entries to [0, 1]  (output embedding dimensionality: 300)
      Important: each step uses the output of the previous step. Thus, it is not possible to skip steps.
    :return: list of record embeddings (numpy array)
    """
    transformed_records = [transform_record(record, return_format="string") for record in
                           records_with_reduced_structure]
    if "laser" in steps:
        processed_records = LASER.model.embed_sentences(transformed_records, lang="en")
        LASER.update(len(processed_records))
        if "reduce" in steps:
            processed_records = REDUCE_MODEL.transform(processed_records)
            if "normalize" in steps:
                processed_records = [normalize_vector(embedding) for embedding in processed_records]
        return processed_records
    return []


def build_error(error, description):
    """
    This function returns an error record for the API output.
    :param error: name of error
    :param description: description of error
    :return:
    """
    return {"error": error, "description": description}


def recordobj(record):
    """
    Function for type check on API input argument "records".
    :param record:
    :return:
    """
    for field in ['id', 'title']:
        if field not in record:
            raise ValueError("Invalid input: missing field {}; could not parse record.".format(field))
    return record

from gevent.lock import Semaphore
semaphore = Semaphore()


class EmbeddingsResource(Resource):
    def __init__(self):
        self.parser = reqparse.RequestParser()
        self.parser.add_argument('reduce', type=str, help="The 'reduce' step reduces the initial Laser embeddings to "
                                                          "a dimensionality of 300 (output embedding dimensionality: 300).")
        self.parser.add_argument('normalize', type=str,
                                 help="The 'normalize' step scales the embedding entries to [0, "
                                      "1] (output embedding dimensionality: 300).")
        self.parser.add_argument('records', help='record_data', type=recordobj, action="append")

    def post(self):
        print("Acquiring semaphore...")
        result = semaphore.acquire(timeout=5)
        if not result:
            abort(503, "Timed out")
        print("Acquired semaphore")
        """
        API function to parse input records and produce the embeddings.
        :return:
        """
        result = SEMAPHORE.acquire(blocking=False)
        logger.info("Semaphore acquired.")
        if not result:
            logger.info("Aborting: queuing not possible.")
            abort(503, description='The service is busy and queueing is not possible.')
        try:
            result = {}
            errors = []
            try:
                input_para = self.parser.parse_args(strict=True)
                records = input_para["records"]
                logger.info("You are passing {} records.".format(len(records)))
            except:
                traceback.print_exc()
                errors.append(build_error("InputValueError", "Could not parse records data."))
                result["errors"] = errors
                logger.info("Sorry, I could not parse your records. Leaving with errors.")
                return result
            if len(records) > 500:
                errors.append(
                    build_error("InputTooLongError", "'records' array is too long. Please provide a maximum of 500 "
                                                     "records."))
                logger.info(
                    "You are passing too many records. The maximum number of records is 500. I will leave soon.")
            steps = ["laser"]
            try:
                if "reduce" in input_para and input_para["reduce"]:
                    if int(input_para["reduce"]) == 1:
                        steps.append("reduce")
                    elif int(input_para["reduce"]) == 0:
                        pass
                    else:
                        errors.append(build_error("InputValueError", "Parameter 'reduce' can be either 0 or 1."))
                else:
                    steps.append("reduce")
            except:
                errors.append(build_error("InputValueError", "Could not parse parameter 'reduce'."))
            try:
                if "normalize" in input_para and input_para["normalize"]:
                    if int(input_para["normalize"]) == 1:
                        steps.append("normalize")
                    elif int(input_para["normalize"]) == 0:
                        pass
                    else:
                        errors.append(build_error("InputValueError", "Parameter 'normalize' can be either 0 or 1."))
                else:
                    steps.append("normalize")
            except:
                errors.append(build_error("InputValueError", "Could not parse parameter 'normalize'."))

            if errors:
                logger.info("Sorry, I am leaving with errors.")
                result["errors"] = errors
                result["status"] = "failure"
                return errors
            logger.info("I will try to execute the following steps: {}.".format(steps))
            start = time.time()
            embeddings = process_records(records, steps=steps)
            SEMAPHORE.release()
            logger.info("Semaphore released.")
            end = time.time()
            semaphore.release()
            print("Released semaphore")
            logger.info("I processed {} records in {} sec.".format(len(records), abs(start - end)))
            logger.info(print_memory())
            result["data"] = [{"id": record["id"], "embedding": embeddings[i].tolist()} for i, record in
                              enumerate(records)]
            result["status"] = "success"
            gc.collect()
            return result
        except Exception as error:
            logger.info("Sorry, your call failed.")
            logger.exception(error)


app = Flask(__name__)
cors = CORS(app)
api = Api(app)


@app.before_request
def log_request_info():
    logger.debug('Body: %s', str(request.get_data())[:100] + "...")


@app.after_request
def after_request(response):
    logger.info('%s %s %s %s %s', request.remote_addr, request.method, request.scheme, request.full_path,
                response.status)
    return response


api.add_resource(EmbeddingsResource, '/embedding_api/embeddings')


if __name__ == '__main__':
    print("Starting on webserver {}...".format(WEBSERVER))
    if WEBSERVER == "waitress":
        serve(app, host='0.0.0.0', port=PORT)
    elif WEBSERVER == "flask":
        app.run(debug=False, host="0.0.0.0", port=PORT, threaded=False)
    else:
        # default (gevent)
        from gevent import monkey
        monkey.patch_all()
        from gevent.pywsgi import WSGIServer
        http_server = WSGIServer(('0.0.0.0', PORT), app)
        http_server.serve_forever()
    

