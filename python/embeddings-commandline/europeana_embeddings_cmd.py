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

## October 2024. This is a modified version of the original Embeddings API created by Anacode.
#  See also https://bitbucket.org/jhn-ngo/recsy-xx/src/master/src/engines/encoders/europeana-embeddings-api/
#  Instead of a webserver, we changed the program into a command-line version.

import os
import psutil
import time
import argparse
import traceback
import json

import joblib
import numpy as np
from laserembeddings import Laser

# Global variables
VERBOSE = False

PID = psutil.Process(os.getpid())
PROCESS = __file__.split("/")[-1].split(".")[0]

workingdirectory = os.getcwd()
if VERBOSE: print('Working directory:', workingdirectory)
os.environ['CUDA_VISIBLE_DEVICES'] = '0'


def process_arguments():
    global VERBOSE
    if VERBOSE: print("Parsing arguments...")
    parser = argparse.ArgumentParser(description='Argument parser for the Embeddings API')
    parser.add_argument('--data', type=str, help='JSON record data to parse (max size = 32K)')
    parser.add_argument("-v", "--verbose", help="verbose output", action="store_true")
    args, unknown = parser.parse_known_args()
    if VERBOSE: print("Unknown arguments: ", unknown)
    try:
        data = json.loads(args.data)
        if args.verbose:
            VERBOSE = True
            print("Received data = ", data)
        return data
    except :
         print("ERROR - Failed to parse input data: ", args.data)
         exit()


def load_models():
    if VERBOSE: print("Starting process {} ...".format(PROCESS))
    if VERBOSE: print(print_memory())
    global LASER
    LASER = Laser()
    global REDUCE_MODEL
    REDUCE_MODEL = joblib.load("embeddings-commandline/default_reduce_model.joblib")
    if VERBOSE: print(print_memory("after loading models"))


def print_memory(suffix=""):
    # 28 sep 2023 PE: unit should be in bytes, so we convert to MiB.
    # However, numbers reported  does not match that of docker stats
    # Processing 2 simple records takes about 385 MiB says Docker whereas we report 498 MiB
    return ('Mem usage {}: {} MiB'.format(suffix, PID.memory_info().rss / (1024 * 1024)))


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
        for lang in languages_by_priority:
            if lang in field_value:
                return ",  ".join(field_value[lang]), lang
        return ", ".join(list(field_value.items())[0][1]), list(field_value.items())[0][0]
    if isinstance(field_value, list):
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
    if VERBOSE: print("Processing records...")
    transformed_records = [transform_record(record, return_format="string") for record in
                           records_with_reduced_structure]
    if "laser" in steps:
        processed_records = LASER.embed_sentences(transformed_records, lang="en")
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
    if VERBOSE: print("Check mandatory fields...")
    for field in ['id', 'title']:
        if field not in record:
            raise ValueError("ERROR missing field {}; could not parse record.".format(field))
    return record


class EmbeddingsResource():
    def process(data):
        """
        API function to parse input records and produce the embeddings.
        :return:
        """
        try:
            result = {}
            try:
                records = data["records"]
                if VERBOSE: print("Received {} records.".format(len(records)))
            except:
                traceback.print_exc()
                print("ERROR - Could not find records field")
                return result
            if len(records) > 500:
                print("ERROR - Too many records (max is 500)")
                return result

            steps = ["laser", "reduce", "normalize"]
            if VERBOSE: print("Executing the following steps: {}.".format(steps))

            start = time.time()
            embeddings = process_records(records, steps=steps)
            end = time.time()
            if VERBOSE: print("Processed {} records in {} sec.".format(len(records), abs(start - end)))
            if VERBOSE: print(print_memory())
            result["data"] = [{"id": record["id"], "embedding": embeddings[i].tolist()} for i, record in
                              enumerate(records)]
            result["status"] = "success"
            return result
        except Exception as error:
            traceback.print_exc()
            print("ERROR - ", error)


if __name__ == '__main__':
    data = process_arguments()
    load_models()
    result = EmbeddingsResource.process(data)
    # We output the final result (should be the only output when VERBOSE = False)
    print(result)
    

