The python file europeana_embeddings_cmd.py contains a modified version of the original Embeddings API designed by
Anacode (see also https://bitbucket.org/jhn-ngo/recsy-xx/src/master/src/engines/encoders/europeana-embeddings-api/)
This version is a command-line program that accepts a --data= parameter with json data and returns embeddings (vectors).
For this the software loads data from the Laser model and default_reduce_model.joblib file.
The program either returns an error message (starting with the word 'ERROR') or it will return the json output containing
the Embeddings. When verbose mode is on (-v parameter), additional output is printed during the calculation of the
Embeddings.

The Dockerfile, test-run.sh and requirements36.txt files can be used to test changes to the Python code in isolation.
At the moment it only works with Python 3.6. Upgrading to 3.10 is work in progress.