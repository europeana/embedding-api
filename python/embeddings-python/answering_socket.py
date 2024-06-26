import socket

ID = ""

def socket_listen(socket, answer_callback_function, verbose, id):
    """
    Listen on the provided socket and send back an answer generated by the callback function.
    If the string '{TERMINATE}' is received then the socket will send back an "OK" response, close the connection and then
    the program will stop itself
    :param socket: the socket to use for listening and sending back data
    :param answer_callback_function: a function that accepts string data as input and sends back dictionary data as output
    :param verbose: boolean, if true we print the flow to the console for debugging
    :param id: unique identifier that's added to logs so we can easily see what logs come from what process
    :return received data from the client
    """
    global ID
    ID = id
    socket.listen(1)  # put the socket into listening mode (1 thread). This is also a good time to flush the logs
    if verbose: print(f"{ID} - Socket listening on {socket.getsockname()}...", flush=True)

    conn, addr = socket.accept()  # wait until client established a connection
    with conn:
        if verbose: print(f"{ID} - Connection from {addr}")

        data = __read_data(conn)
        if (data == '{TERMINATE}\n'):
            conn.send("OK".encode())
            print(f"{ID} - Received terminate signal. Shutting down application...")
            exit(0)

        # Send back answer message to the client
        response = answer_callback_function(data)
        conn.send(str(response).encode())
        return data


def __read_data(conn):
    '''
    Read bytes from a socket connection until we receive '}\n' (end of json input and newline)
    :param conn: the socket connection to read from
    :return: string containing the read data
    '''
    bytes = b''
    while True:
        try:
            next_bytes = conn.recv(4096)
            bytes = bytes + next_bytes
            if bytes.endswith(b'}\n'):
                break
        except Exception as error:
            print_and_return_error(conn, "Error reading data", error)
            break

    try:
        result = bytes.decode("utf-8")
        return result
    except Exception as error:
        print_and_return_error(conn, "Error decoding data", error)


def print_and_return_error(conn, custom_message, error):
    print(f"{ID} ERROR - {error}")
    result = {}
    result["status"] = "error"
    result["message"] = custom_message + "\n" + str(error)
    conn.send(result)



#
# For testing purposes
#
def dummy_callback_function(data):
    response = {}
    response["status"] = "success"
    response["data"] = "Thank you for connecting. This is just a test answer"
    return response

if __name__ == '__main__':
    s = socket.socket()
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind(('127.0.0.1', 12001)) # only allow local connections
    while True:
        data = socket_listen(s, dummy_callback_function, True, "TEST_PROCESS")
        print(f"{ID} - Data received = {data}")

