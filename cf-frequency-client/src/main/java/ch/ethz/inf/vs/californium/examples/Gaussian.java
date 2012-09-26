/**
 * 
 */

package ch.ethz.inf.vs.californium.examples;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.TokenManager;
import ch.ethz.inf.vs.californium.coap.registries.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.registries.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.util.Log;
import ch.ethz.inf.vs.californium.util.Properties;

import com.google.common.io.Files;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Ints;

/**
 * @author Francesco Corazza
 * 
 */
public class Gaussian {
    private static final int NUM_OF_REPEATS = 5;
    private static final int WAITING_MILLIS = 2500;
    
    private static final boolean TEST_PROXY = true;
    private static final String[] RESOURCES = { "helloWorld", "seg1/seg2/seg3", "query?a=1", "obs",
                    "separate" };
    private final static DatagramPacket[] REQUESTS = new DatagramPacket[RESOURCES.length];
    
    /** The server uri. */
    private final static String SERVER_URI = "coap://localhost:5684";
    /** The proxy uri. */
    private final static String PROXY_URI = "coap://localhost:5683";
    
    private static final int MAX_THREADS = 300;
    private static ScheduledExecutorService requestScheduler = Executors
                    .newScheduledThreadPool(MAX_THREADS);
    
    private static final int[] NUM_OF_REQUESTS = { 100, 200, 300, 400, 500 };
    // private static final int[] NUM_OF_REQUESTS = { 50, 150, 200, 400, 800, 1000, 1500, 2000,
    // 2500,
    // 3000 };
    private static final int NUM_OF_TESTS = NUM_OF_REQUESTS.length;
    
    // creating the responses offline
    static {
        // create a request for each resource
        for (int i = 0; i < RESOURCES.length; i++) {
            String resource = RESOURCES[i];
            
            // create a new request
            Request request = new Request(CodeRegistry.METHOD_GET, false);
            
            request.setAccept(MediaTypeRegistry.TEXT_PLAIN);
            
            // set the params
            if (TEST_PROXY) {
                request.setURI(PROXY_URI);
                String proxyUriString = SERVER_URI + "/" + resource;
                Option proxyUriOption = new Option(proxyUriString, OptionNumberRegistry.PROXY_URI);
                request.setOption(proxyUriOption);
            } else {
                request.setURI(SERVER_URI + "/" + resource);
            }
            
            byte[] payload = request.toByteArray();
            
            // create datagram
            REQUESTS[i] =
                            new DatagramPacket(payload, payload.length, request.getPeerAddress()
                                            .getAddress(), request.getPeerAddress().getPort());
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // set the level high
        Log.setLevel(Level.SEVERE);
        Log.init();
        
        // creating statistics
        double[][] doubleMatrix = new double[4][NUM_OF_TESTS];
        DescriptiveStatistics[] responseStats = new SynchronizedDescriptiveStatistics[NUM_OF_TESTS];
        List<List<Double>> valuesList = new LinkedList<List<Double>>();
        
        // repeat the batch of requests for each test
        for (int i = 0; i < NUM_OF_TESTS; i++) {
            System.out.println();
            System.out.println("********** TEST " + (i + 1) + "/" + NUM_OF_TESTS + " **********");
            List<Double> values = new LinkedList<Double>();
            valuesList.add(values);
            makeTest(doubleMatrix, responseStats, i, values);
        } // for (int i = 0; i < NUM_OF_TESTS; i++)
        
        // print the statistic of all tests
        System.out.println("********** SINGLE STATS **********");
        for (int i = 0; i < responseStats.length; i++) {
            DescriptiveStatistics descriptiveStatistics = responseStats[i];
            printStats(descriptiveStatistics, i);
        }
        
        System.out.println("********** CUMULATIVE STATS **********");
        String[] titles = { "MEAN", "PERCENTILE 90", "LOST RESPONSE" };
        printCumulativeStats(titles, doubleMatrix);
        
        writeFile(valuesList);
        
        System.exit(0);
    }
    
    /**
     * @param i
     * @return
     */
    private static int getNumOfRequest(int i) {
        return NUM_OF_REQUESTS[i];
        // IntMath.checkedPow(i, 4) / 2 + 2 * i + 50;
    }
    
    /**
     * @return
     */
    private static int getPositiveGaussian(int arrayLength) {
        Random random = new Random(System.currentTimeMillis());
        int gaussianValue =
                        DoubleMath.roundToInt(Math.abs(random.nextGaussian()) * arrayLength,
                                        RoundingMode.HALF_UP);
        
        if (gaussianValue < 0 || gaussianValue > arrayLength - 1) {
            gaussianValue = 0;
        }
        
        return gaussianValue;
    }
    
    /**
     * @param numOfRequests
     * @param l
     * @return
     */
    private static int[] getRequestDistribution(int numOfRequests, long intervals) {
        // compute the distribution of the requests
        int[] timings = new int[numOfRequests];
        Random random = new SecureRandom();
        for (int j = 0; j < numOfRequests; j++) {
            timings[j] = random.nextInt(Ints.checkedCast(intervals + 1));
        }
        System.out.println("Created distribution array, tot requests: " + numOfRequests);
        Arrays.sort(timings);
        return timings;
    }
    
    /**
     * @param doubleMatrix
     * @param responseStats
     * @param i
     * @param values
     */
    private static void makeTest(double[][] doubleMatrix, DescriptiveStatistics[] responseStats,
                    int i, List<Double> values) {
        long startingTimestamp = System.nanoTime();
        
        // calculate the number of thread to spawn
        int numOfRequests = getNumOfRequest(i);
        // create the list of futures
        // List<Future<Double>> futures = new LinkedList<Future<Double>>();
        // create the statistics
        responseStats[i] = new SynchronizedDescriptiveStatistics();
        
        int[] timings = getRequestDistribution(numOfRequests, TimeUnit.SECONDS.toMillis(1));
        
        // repeat the test "i" with the same distribution to have better results
        for (int k = 0; k < NUM_OF_REPEATS; k++) {
            // reset the cache
            requestScheduler.execute(new CacheRunnable());
            
            System.out.println();
            System.out.println("Interation: " + (k + 1) + "/" + NUM_OF_REPEATS);
            
            // initialize the var before to enter in the loop
            long previousTime = 0;
            // make the requests
            for (int j = 0; j < numOfRequests; j++) {
                
                long delay = timings[j] - previousTime;
                
                requestScheduler.schedule(new BytesSender(responseStats[i], values), delay,
                                TimeUnit.MILLISECONDS);
                
                previousTime = timings[j];
            }
            
            // wait for the end of the receptions
            try {
                Thread.sleep(WAITING_MILLIS);
            } catch (InterruptedException e) {
            }
            // System.out.println("Waiting for " + WAITING_MILLIS + " millis");
        }
        
        System.out.println();
        System.out.println("Request received: " + responseStats[i].getN() + "/" + numOfRequests
                        * NUM_OF_REPEATS);
        System.out.println("The test has lasted: " + (System.nanoTime() - startingTimestamp)
                        / 1000000 + " millis");
        
        // add the data for the regression
        doubleMatrix[0][i] = numOfRequests * NUM_OF_REPEATS;
        doubleMatrix[1][i] = responseStats[i].getMean();
        doubleMatrix[2][i] = responseStats[i].getPercentile(90);
        doubleMatrix[3][i] = numOfRequests - responseStats[i].getN();
        
    }
    
    private static void printCumulativeStats(String[] titles, double[][] doubleMatrix) {
        if (titles.length + 1 != doubleMatrix.length) {
            return;
        }
        
        SimpleRegression regression = new SimpleRegression();
        // start from 1 because the first row is the number of requests
        for (int i = 1; i <= titles.length; i++) {
            String title = titles[i - 1];
            
            // create the double[][2] for the current parameter
            double[][] values = { doubleMatrix[0], doubleMatrix[i] };
            
            // clear and add data to the regression
            regression.clear();
            regression.addData(values);
            
            // create correlation and covariance from the data
            double correlation =
                            new PearsonsCorrelation().correlation(doubleMatrix[0], doubleMatrix[i]);
            double covariance = new Covariance().covariance(doubleMatrix[0], doubleMatrix[i]);
            
            // print stats
            System.out.println();
            System.out.println("** " + title + " **");
            System.out.println(String.format("regression:\ty = %.3f * x + %.3f",
                            regression.getSlope(), regression.getIntercept()));
            // System.out.println(String.format("slope error:\t%.3f", regression.getSlopeStdErr()));
            System.out.println(String.format("Pearson Correlation:\t%.3f", correlation));
            System.out.println(String.format("covariance:\t%.3f", covariance));
        }
        
    }
    
    /**
     * @param descriptiveStatistics
     * @param received
     */
    private static void printPercentiles(DescriptiveStatistics descriptiveStatistics, int received) {
        String percentile = "Class\t|\tCum.(f)\t|\t# req.\n";
        int increment = 15;
        
        for (int i = 45; i <= 100;) {
            percentile +=
                            i
                                            + "\t|\t"
                                            + String.format("%.2f",
                                                            descriptiveStatistics.getPercentile(i))
                                            + "\t|\t" + i * received / 100 + "\n";
            
            if (i < 75) {
                increment = 15;
            } else if (i < 93) {
                increment = 3;
            } else {
                increment = 1;
            }
            
            i += increment;
        }
        
        System.out.println(percentile);
    }
    
    /**
     * @param descriptiveStatistics
     * @param testNumber
     */
    private static void printStats(DescriptiveStatistics descriptiveStatistics, int testNumber) {
        System.out.println("** Test " + (testNumber + 1) + " **");
        // Compute some statistics
        int received = (int) descriptiveStatistics.getN();
        int sent = getNumOfRequest(testNumber) * NUM_OF_REPEATS;
        System.out.println("sent:\t" + sent);
        System.out.println("received:\t" + received);
        System.out.println(String.format("ratio:\t%.3f", received / (double) sent));
        System.out.println(String.format("max:\t%.3f", descriptiveStatistics.getMax()));
        System.out.println(String.format("min:\t%.3f", descriptiveStatistics.getMin()));
        System.out.println(String.format("mean:\t%.3f", descriptiveStatistics.getMean()));
        System.out.println(String.format("geom:\t%.3f", descriptiveStatistics.getGeometricMean()));
        System.out.println(String.format("var:\t%.3f", descriptiveStatistics.getVariance()));
        System.out.println(String.format("std:\t%.3f", descriptiveStatistics.getStandardDeviation()));
        System.out.println(String.format("median:\t%.3f", descriptiveStatistics.getPercentile(50)));
        System.out.println(String.format("90-cen:\t%.3f", descriptiveStatistics.getPercentile(90)));
        System.out.println();
        
        // printPercentiles(descriptiveStatistics, received);
    }
    
    private static void writeFile(List<List<Double>> valuesList) {
        int i = 1;
        for (List<Double> list : valuesList) {
            String fileName = i++ + "_stat_txt";
            final File statLog = new File(fileName);
            try {
                statLog.createNewFile();
                
                // write the header
                Files.write("Test:" + i + " \n", statLog, Charset.defaultCharset());
                
                for (Double doubleValue : list) {
                    Files.append(doubleValue + "\n", statLog, Charset.defaultCharset());
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("Wrote test " + (i - 1));
        }
    }
    
    private static class BytesSender implements Runnable {
        private final static int BUFFER_SIZE = Properties.std.getInt("RX_BUFFER_SIZE") + 1;
        private final static int TIMEOUT_MILLIS = 2000;
        private final DescriptiveStatistics descriptiveStatistics;
        private final List<Double> values;
        
        public BytesSender(DescriptiveStatistics descriptiveStatistics, List<Double> values) {
            this.descriptiveStatistics = descriptiveStatistics;
            this.values = values;
        }
        
        @Override
        public void run() {
            DatagramSocket socket = null;
            try {
                // get a socket on a free port
                socket = new DatagramSocket(0);
                socket.setReuseAddress(true);
                
                // set the millis of timeout
                socket.setSoTimeout(TIMEOUT_MILLIS);
                
                int gaussianValue = getPositiveGaussian(RESOURCES.length);
                DatagramPacket datagram = REQUESTS[gaussianValue];
                
                // send it over the UDP socket
                long sendingTimestamp = System.nanoTime();
                socket.send(datagram);
                
                // allocate buffer
                byte[] buffer = new byte[BUFFER_SIZE];
                
                // initialize new datagram
                datagram = new DatagramPacket(buffer, buffer.length);
                
                // receive datagram
                
                socket.receive(datagram);
                
                double rtt = (System.nanoTime() - sendingTimestamp) / 1000000d;
                values.add(rtt);
                descriptiveStatistics.addValue(rtt);
                
            } catch (Exception e) {
                // e.printStackTrace();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }
    }
    
    private static class CacheRunnable implements Runnable {
        
        @Override
        public void run() {
            // create a new request
            Request request = new DELETERequest();
            request.setURI(SERVER_URI + "/debug/cache");
            
            // enable response queue for blocking I/O
            request.enableResponseQueue(true);
            
            // get the token from the manager
            request.setToken(TokenManager.getInstance().acquireToken());
            
            // execute the request
            try {
                request.execute();
                
                // receive the response
                Response response = request.receiveResponse();
                
                if (response != null) {
                    System.out.println("Deleted cache");
                }
            } catch (IOException e) {
            } catch (InterruptedException e) {
            }
        }
    }
}
