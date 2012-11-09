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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
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

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.io.Files;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Ints;

/**
 * @author Francesco Corazza
 * 
 */
public class Gaussian {
    private static final TimeUnit DELAY_TIME_UNIT = TimeUnit.MILLISECONDS;
    
    private static final long TIME_FRAME_DIVISOR = DELAY_TIME_UNIT.convert(1, TimeUnit.SECONDS);
    // TimeUnit.SECONDS.toMillis(1);
    
    private static final int NUM_OF_REPEATS = 5;
    // private static final int WAITING_MILLIS = 3000;
    private final static int BUFFER_SIZE = Properties.std.getInt("RX_BUFFER_SIZE") + 1;
    private final static int SO_TIMEOUT_MILLIS = Properties.std.getInt("DEFAULT_OVERALL_TIMEOUT");
    
    private static final boolean TEST_PROXY = true;
    private static final String[] RESOURCES = { "helloWorld", "seg1/seg2/seg3", "query?a=1", "obs",
                    "separate" };
    private final static DatagramPacket[] REQUESTS = new DatagramPacket[RESOURCES.length];
    
    /** The server uri. */
    private final static String SERVER_URI = "coap://localhost:5684";
    /** The proxy uri. */
    private final static String PROXY_URI = "coap://localhost:5683";
    
    private static final int MAX_THREADS = 300;
    private static ScheduledExecutorService requestScheduler;
    
    private static final int[] NUM_OF_REQUESTS =
                    { 10, 50, 100, 200, 300, 400, 500, 600, 800, 1000 };
    // private static final int[] NUM_OF_REQUESTS = { 200, 400, 800, 1600, 3200, 5000 };
    private static final int NUM_OF_TESTS = NUM_OF_REQUESTS.length;
    private static final int RTT_COLUMN = 0;
    private static final int LOST_RATIO_COLUMN = 1;
    private static final int LOST_AVG_COLUMN = 2;
    private static final int RTT_STD_COLUMN = 3;
    
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
        // set the level high to speedup the process
        Log.setLevel(Level.SEVERE);
        Log.init();
        
        // create the 2-column tables that will be filled for each test with the values of
        // aggregated stats
        Table<Integer, Integer, Double> cumulativeStatsTable = TreeBasedTable.create();
        // double[][] lostPackets = new double[NUM_OF_TESTS][2];
        // double[][] avgRtt = new double[NUM_OF_TESTS][2];
        
        // create a matrix of the thread-safe version of the statistic handler
        DescriptiveStatistics[] responseStats = new SynchronizedDescriptiveStatistics[NUM_OF_TESTS];
        
        // create the thread-safe "matrix" (queue of queue) to contain all values gathered
        // Queue<Queue<Double>> valuesList = new ConcurrentLinkedQueue<Queue<Double>>();
        
        // create the table to contain all values
        Table<Long, Integer, Double> rttTable = TreeBasedTable.create();
        // fill the table
        for (int i = 0; i < TIME_FRAME_DIVISOR; i++) {
            for (int j = 0; j < NUM_OF_TESTS; j++) {
                rttTable.put((long) i, j, (double) 0);
            }
        }
        
        long startTime = System.nanoTime();
        
        // repeat the batch of requests for each test
        for (int i = 0; i < NUM_OF_TESTS; i++) {
            System.out.println("\n********** TEST " + (i + 1) + "/" + NUM_OF_TESTS + " **********");
            
            // calculate the number of request
            int totRequests = getNumOfRequest(i);
            
            // create the statistics for the current test and add it to the "matrix"
            DescriptiveStatistics currentTestStats = new SynchronizedDescriptiveStatistics();
            responseStats[i] = currentTestStats;
            
            // get the distribution of the requests within the second handled
            final int[] timings = getRequestsDistribution(totRequests, TIME_FRAME_DIVISOR);
            System.out.println("Created distribution array, tot requests: " + totRequests);
            
            // execute the actual test/s
            Map<Long, Double> timingRttMap = doTest(totRequests, timings, currentTestStats);
            
            // fill the table with the entries: timing (row), test number (column) and rtt (cell
            // value)
            for (Entry<Long, Double> entry : timingRttMap.entrySet()) {
                rttTable.put(entry.getKey(), i, entry.getValue());
            }
            
            // System.out.println("\nTest lasted " + (System.nanoTime() - startTime) / 1000);
            System.out.println("Test " + (i + 1) + " completed");
            
            // add the data for the cumulative stats
            long receivedResponses = currentTestStats.getN();
            if (receivedResponses != 0) {
                double lostPackets =
                                ((double) totRequests * NUM_OF_REPEATS - receivedResponses)
                                                / NUM_OF_REPEATS;
                double lostPacketsRatio = lostPackets / totRequests;
                double meanRtt = currentTestStats.getMean();
                double stdRtt = currentTestStats.getStandardDeviation();
                cumulativeStatsTable.put(totRequests, RTT_COLUMN, meanRtt);
                cumulativeStatsTable.put(totRequests, RTT_STD_COLUMN, stdRtt);
                cumulativeStatsTable.put(totRequests, LOST_RATIO_COLUMN, lostPacketsRatio);
                cumulativeStatsTable.put(totRequests, LOST_AVG_COLUMN, lostPackets);
                
                System.out.println();
                System.out.println(String.format("Response received: %d/%d", receivedResponses,
                                totRequests * NUM_OF_REPEATS));
                System.out.println(String.format("Avg rtt: %.2f\n", meanRtt));
            }
        } // for (int i = 0; i < NUM_OF_TESTS; i++)
        
        // write the statistics
        writeStatsToFile(rttTable, startTime);
        
        // write the cumulative statistics
        writeCumulativeStatsToFile(cumulativeStatsTable, startTime);
        
        // write the percentiles
        writePercentiles(responseStats, startTime);
        
        // print the statistic of all tests
        System.out.println("********** SINGLE STATS **********");
        for (int i = 0; i < responseStats.length; i++) {
            DescriptiveStatistics descriptiveStatistics = responseStats[i];
            printStats(descriptiveStatistics, i);
        }
        
        // System.out.println("********** CUMULATIVE STATS **********");
        
        // printCumulativeStats(titles, statsMatrix);
        
        // writeFile(valuesList);
        
        System.exit(0);
    }
    
    private static Map<Long, Double> doTest(int totRequests, int[] timings,
                    DescriptiveStatistics responseStats) {
        // create the table to contain the measures
        ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<Double>> measurationTable =
                        new ConcurrentSkipListMap<Long, ConcurrentLinkedQueue<Double>>();
        
        // repeat the current test with the same distribution to calculate the average of the values
        for (int k = 0; k < NUM_OF_REPEATS; k++) {
            System.out.println("Interation: " + (k + 1) + "/" + NUM_OF_REPEATS);
            
            // reset the cache
            // requestScheduler.execute(new CacheResetRunnable());
            
            // reset the thread pool
            int totThreads = totRequests > MAX_THREADS ? MAX_THREADS
                            : totRequests;
            requestScheduler = Executors.newScheduledThreadPool(totThreads);
            
            // reset the process to ensure the equality of the tests
            resetProcesses();
            
            // the previous time needed to calculate the delay
            long previousTime = 0;
            
            // make the requests
            for (int j = 0; j < totRequests; j++) {
                long currentTime = timings[j];
                
                // get the current queue, containing the measures for the current timing
                ConcurrentLinkedQueue<Double> currentQueue = measurationTable.get(currentTime);
                if (currentQueue == null) {
                    currentQueue = new ConcurrentLinkedQueue<Double>();
                    measurationTable.put(currentTime, currentQueue);
                }
                
                // calculate the delay of the next request
                long delay = currentTime - previousTime;
                
                // execute the request
                requestScheduler.schedule(new BytesSender(currentQueue), delay, DELAY_TIME_UNIT);
                
                // update the time
                previousTime = currentTime;
            }
            
            // wait for the end of the receptions
            requestScheduler.shutdown();
            int alpha = 16;
            while (!requestScheduler.isTerminated() && alpha != 1) {
                try {
                    requestScheduler.awaitTermination(SO_TIMEOUT_MILLIS / alpha, TimeUnit.SECONDS);
                    alpha /= 2;
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    break;
                } finally {
                    requestScheduler.shutdownNow();
                }
            }
            
            // try {
            // Thread.sleep(WAITING_MILLIS);
            // } catch (InterruptedException e) {
            // }
            // System.out.println("Waiting for " + WAITING_MILLIS + " millis");
        } // for (int k = 0; k < NUM_OF_REPEATS; k++)
        
        // calculate the average for each timings
        Map<Long, Double> resultMap = new TreeMap<Long, Double>();
        
        for (long timing : measurationTable.keySet()) {
            int responseReceived = 0;
            double totRtt = 0;
            
            // get all the rtt from the response received
            for (Double measturation : measurationTable.get(timing)) {
                responseStats.addValue(measturation);
                responseReceived++;
                totRtt += measturation;
            }
            
            // calculate the average
            if (responseReceived != 0) {
                resultMap.put(timing, totRtt / responseReceived);
            }
        }
        
        return resultMap;
    }
    
    private static double getAvgBytes(int numberOfRequests) {
        int numOfBytes = 0;
        for (DatagramPacket element : REQUESTS) {
            numOfBytes += element.getLength();
        }
        
        double packetAvgSize = (double) numOfBytes / REQUESTS.length;
        return packetAvgSize * numberOfRequests;
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
    private static int[] getRequestsDistribution(int numOfRequests, long intervalFrame) {
        // compute the distribution of the requests
        int[] timings = new int[numOfRequests];
        Random random = new SecureRandom();
        for (int j = 0; j < numOfRequests; j++) {
            timings[j] = random.nextInt(Ints.checkedCast(intervalFrame));
        }
        
        Arrays.sort(timings);
        return timings;
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
            System.out.println(String.format("regression:\ty = %.2f * x + %.2f",
                            regression.getSlope(), regression.getIntercept()));
            // System.out.println(String.format("slope error:\t%.2f", regression.getSlopeStdErr()));
            System.out.println(String.format("Pearson Correlation:\t%.2f", correlation));
            System.out.println(String.format("covariance:\t%.2f", covariance));
        }
        
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
        System.out.println(String.format("ratio:\t%.2f", received / (double) sent));
        System.out.println(String.format("max:\t%.2f", descriptiveStatistics.getMax()));
        System.out.println(String.format("min:\t%.2f", descriptiveStatistics.getMin()));
        System.out.println(String.format("mean:\t%.2f", descriptiveStatistics.getMean()));
        System.out.println(String.format("geom:\t%.2f", descriptiveStatistics.getGeometricMean()));
        System.out.println(String.format("var:\t%.2f", descriptiveStatistics.getVariance()));
        System.out.println(String.format("std:\t%.2f", descriptiveStatistics.getStandardDeviation()));
        System.out.println(String.format("median:\t%.2f", descriptiveStatistics.getPercentile(50)));
        System.out.println(String.format("90-cen:\t%.2f", descriptiveStatistics.getPercentile(90)));
        System.out.println();
        
        // printPercentiles(descriptiveStatistics, received);
    }
    
    private static void resetProcesses() {
        // TODO Auto-generated method stub
        
    }
    
    private static void writeCumulativeStatsToFile(
                    Table<Integer, Integer, Double> cumulativeStatsTable, long startTime) {
        String fileName = startTime + File.separator + "cumulative_stats.txt";
        final File statLog = new File(fileName);
        
        try {
            Files.createParentDirs(statLog);
            statLog.createNewFile();
            
            // write the header
            Files.write("req/s\tB/s\tRTT\tstd\t#losts\t%lost\n", statLog, Charset.defaultCharset());
            
            for (Integer numberOfRequests : cumulativeStatsTable.rowKeySet()) {
                double bytesPerSecond = getAvgBytes(numberOfRequests);
                double rtt = cumulativeStatsTable.get(numberOfRequests, RTT_COLUMN);
                double rttStd = cumulativeStatsTable.get(numberOfRequests, RTT_STD_COLUMN);
                double lostPackets = cumulativeStatsTable.get(numberOfRequests, LOST_AVG_COLUMN);
                double lostPacketRatio =
                                cumulativeStatsTable.get(numberOfRequests, LOST_RATIO_COLUMN);
                
                Files.append(String.format("%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\n", numberOfRequests,
                                bytesPerSecond, rtt, rttStd, lostPackets, lostPacketRatio),
                                statLog, Charset.defaultCharset());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        System.out.println("Cumulative stats written to file");
    }
    
    /**
     * @param descriptiveStatisticsArray
     * @param received
     */
    private static void writePercentiles(DescriptiveStatistics[] descriptiveStatisticsArray,
                    long startTime) {
        String fileName = startTime + File.separator + "percentile_stats.txt";
        final File statLog = new File(fileName);
        
        try {
            Files.createParentDirs(statLog);
            statLog.createNewFile();
            
            // write the header
            Files.write("r/s\t", statLog, Charset.defaultCharset());
            for (int j = 10; j <= 100; j += 10) {
                Files.append(j + "\t", statLog, Charset.defaultCharset());
            }
            Files.append("\n", statLog, Charset.defaultCharset());
            
            // write the values
            for (int i = 0; i < descriptiveStatisticsArray.length; i++) {
                DescriptiveStatistics descriptiveStatistics = descriptiveStatisticsArray[i];
                StringBuilder builder = new StringBuilder();
                builder.append(NUM_OF_REQUESTS[i] + "\t");
                for (int j = 10; j <= 100; j += 10) {
                    builder.append(String.format("%.2f\t", descriptiveStatistics.getPercentile(j)));
                }
                Files.append(builder.toString() + "\n", statLog, Charset.defaultCharset());
                
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        System.out.println("Percentiles stats written to file");
    }
    
    private static void writeStatsToFile(Table<Long, Integer, Double> rttTable, long startTime) {
        String fileName = startTime + File.separator + "test_stats.txt";
        final File statLog = new File(fileName);
        
        try {
            Files.createParentDirs(statLog);
            statLog.createNewFile();
            
            // write the header
            Files.write("timing\t", statLog, Charset.defaultCharset());
            for (Integer columnName : rttTable.columnKeySet()) {
                Files.append(NUM_OF_REQUESTS[columnName] + "\t", statLog, Charset.defaultCharset());
            }
            Files.append("\n", statLog, Charset.defaultCharset());
            
            // write the values
            for (Long timing : rttTable.rowKeySet()) {
                StringBuilder builder = new StringBuilder();
                builder.append(timing + "\t");
                for (Integer columnName : rttTable.columnKeySet()) {
                    Double rtt = rttTable.get(timing, columnName);
                    if (rtt != null && rtt != 0) {
                        builder.append(String.format("%.2f\t", rtt));
                    } else {
                        builder.append("\t");
                    }
                }
                Files.append(builder.toString() + "\n", statLog, Charset.defaultCharset());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        System.out.println("Tests timings written to file");
    }
    
    private static class BytesSender implements Runnable {
        private final Queue<Double> values;
        
        public BytesSender(Queue<Double> values) {
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
                socket.setSoTimeout(SO_TIMEOUT_MILLIS);
                
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
            } catch (Exception e) {
                // e.printStackTrace();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }
    }
    
    private static class CacheResetRunnable implements Runnable {
        
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
