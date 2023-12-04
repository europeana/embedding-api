package eu.europeana.api.embedding.service;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.api.embedding.exception.ExecutorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * Class that is responsible for (re)starting and communicating with a Python process
 * @author Patrick Ehlert
 */
public class Executor {

    private static final Logger LOG = LogManager.getLogger(Executor.class);

    private final int portNr;
    private final int maxRecords;
    private final String directory;

    private int nrRecordsProcessed;
    private Socket socket;
    private StartedProcess process;

    /**
     * Create a new Python executor
     * @param portNr the port number on which the process should listen
     * @param maxRecords the number of items processed before the process should be restarted
     * @param directory the folder where the Python data is
     * @throws EuropeanaApiException when there's a problem starting the executor process
     */
    public Executor(int portNr, int maxRecords, String directory) throws EuropeanaApiException {
        this.portNr = portNr;
        this.maxRecords = maxRecords;
        this.directory = directory;
        this.nrRecordsProcessed = 0;

        // Normally we start a process, except when debugging locally and starting a Python process manually via Docker
        this.process = createProcess();
    }

    /**
     *
     * @return the port number on which the process should listen
     */
    public int getPortNr() {
        return this.portNr;
    }

    /**
     *
     * @return the number of items processed so far
     */
    public int getNrRecordsProcessed() {
        return this.nrRecordsProcessed;
    }

    /**
     * Send new data to the Python process and return its output
     * @param dataJson record data in json format
     * @param nrRecords the number of items we are sending
     * @return Python process output
     * @throws ExecutorException where there's a problem communicating with the Python process
     */
    public String sendData(String dataJson, int nrRecords) throws EuropeanaApiException {
        String result = null;
        try {
            // For local debugging fill in IP address of the Python Docker container instead
            LOG.debug("Opening socket on port {}", portNr);
            this.socket = new Socket("127.0.0.1", this.portNr);
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true, Charset.defaultCharset());
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.defaultCharset()))) {
                LOG.trace("Sending json data: {}", dataJson);
                out.println(dataJson);
                while (result == null) {
                    result = in.readLine();
                    LOG.trace("Waiting for answer...");
                    Thread.sleep(500);
                }
                LOG.trace("Received answer: {}", result);
                nrRecordsProcessed = nrRecordsProcessed + nrRecords;
            }
        } catch (IOException e) {
            Process p = process.getProcess();
            LOG.error("Executor error: process pid {}, port {}, isAlive {}, exitValue {}", p.pid(), portNr, p.isAlive(), p.exitValue());
            throw new ExecutorException("Executor not available!", e, true);
        } catch (InterruptedException e) {
            LOG.error("Interruption while waiting for Python process", e );
            Thread.currentThread().interrupt();
        }

        // Because of a memory-leak in the Python LLM we need to restart the process every now and then
        if (nrRecordsProcessed >= maxRecords) {
            this.destroy();
            nrRecordsProcessed = 0;
            this.process = createProcess();
        }
        return result;
    }

    private StartedProcess createProcess() throws EuropeanaApiException {
        long start = System.currentTimeMillis();
        LOG.debug("Creating new embedding process...");
        StartedProcess result = null;
        try {
            ProcessExecutor pe = new ProcessExecutor()
                    .directory(new File(this.directory))
                    .command("python3.6", "./europeana_embeddings_cmd.py", "--port=" + portNr,
                            (LOG.isDebugEnabled() ? "--verbose" : ""))
                    .redirectError(Slf4jStream.of("Python").asError())
                    .exitValue(0);
            if (LOG.isDebugEnabled()) {
                // For some reason we often don't get the Python console output until an error occurs or the process stopped
                    pe.redirectOutput(Slf4jStream.of("Python").asDebug());
            }
            result = pe.start();
        } catch (IOException e) {
            throw new ExecutorException("Error creating process", e, true);
        }
        LOG.debug("Process with pid {} listening on port {} created in {} ms", result.getProcess().pid(), portNr, System.currentTimeMillis() - start);
        return result;
    }

    /**
     * Kill the Python process of this executor.
     * @throws EuropeanaApiException when there is an error closing the socket connection
     */
    public void destroy() throws EuropeanaApiException {
        String processId = (process == null ? "null" : String.valueOf(process.getProcess().pid()));

        // To prevent 'hanging' open ports in the Python process, make sure we close the socket first
        try {
            LOG.debug("Closing socket {} to process {}...", portNr, processId);
            this.socket.close();
        } catch (IOException e) {
            throw new ExecutorException("Error closing socket", e, true);
        }

        LOG.debug("Sending terminate signal to process {} (executor with port {}, nrProcessed = {})", processId, portNr, nrRecordsProcessed);
        try (Socket socket = new Socket("127.0.0.1", this.portNr);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, Charset.defaultCharset())) {
            out.println("TERMINATE");
        } catch (IOException e) {
            LOG.error("Error sending terminate signal to process {}. Forcing process kill...", processId, e);
            process.getProcess().destroyForcibly();
        }
    }

}
