package caevo;

import caevo.util.CaevoProperties;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * Black-box test suite for Caevo.
 * Created by ymishory on 22/01/16.
 */
public class MainTest {

    public static final String EXPECTED_SUFFIX_KEY = "expectedSuffix";

    @Test
    public void testMain() throws Exception {
        // load custom properties
        CaevoProperties.load("./src/test/resources/test.properties");
        // set the needed environment variable
        setEnvironmentVariable(CaevoProperties.getString("jwnl"));

        File inputDir = new File(CaevoProperties.getString("inputPath", ""));
        File[] instances, expected;
        if (!inputDir.exists()) fail();
        if (!inputDir.isDirectory()) {
            File inputFile = inputDir;
            inputDir = inputFile.getParentFile();
            instances = new File[]{ inputFile };
        }
        else {
            instances = inputDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    try {
                        return pathname.getName().endsWith(CaevoProperties.getString("testSuffix"));
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }
            });
        }
        expected = inputDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                try {
                    return pathname.getName().endsWith(CaevoProperties.getString(EXPECTED_SUFFIX_KEY));
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        });
        Map<File, File> tests = mapTestToExpected(instances, expected);

        double[] accumulatedStats = new double[9];
        File output = new File(CaevoProperties.getString("outputPath"));
        if (!output.exists()) output.createNewFile();
        StringBuilder sb = new StringBuilder();
        for (File test : instances) {
            String[] args = {test.getAbsolutePath(), "raw"};
            Main.main(args);
            if (tests.keySet().contains(test)) {
                // run all tests, accumulate statistics
                double[] stats = checkResult(test.getAbsolutePath(), tests.get(test));
                sb.append(printStats("Stats for " + test.getAbsolutePath(), stats));
                for (int i = 0; i < stats.length; i++) {
                    accumulatedStats[i] += stats[i];
                }
            }
        }
        if (instances.length > 1) {
            for (int i = 0; i < accumulatedStats.length; i++) {
                accumulatedStats[i] /= instances.length;
            }
            sb.append(printStats("Batch statistics", accumulatedStats));
        }
        System.out.println(sb.toString());
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));
        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }

    private String printStats(String headline, double[] stats) {
        StringBuilder sb = new StringBuilder(headline).append('\n');
        sb.append('\t').append("Events recall:").append("\t\t").append(stats[0]).append('\n');
        sb.append('\t').append("Events precision:").append('\t').append(stats[1]).append('\n');
        sb.append('\t').append("Events F1:").append("\t\t\t").append(stats[2]).append('\n');
        sb.append('\t').append("Timex recall:").append("\t\t").append(stats[3]).append('\n');
        sb.append('\t').append("Timex precision:").append('\t').append(stats[4]).append('\n');
        sb.append('\t').append("Timex F1:").append("\t\t\t").append(stats[5]).append('\n');
        sb.append('\t').append("TLinks recall:").append("\t\t").append(stats[6]).append('\n');
        sb.append('\t').append("TLinks precision:").append('\t').append(stats[7]).append('\n');
        sb.append('\t').append("TLinks F1:").append("\t\t\t").append(stats[8]).append('\n');
        sb.append("Overall score: ").append((stats[2] + stats[5] + stats[8]) / 3).append("\n\n");

        return sb.toString();
    }

    private double[] checkResult(String testName, File expected) throws Exception {
        File resultFile = new File(testName+CaevoProperties.getString("resultSuffix"));
        if (!resultFile.exists()) return new double[9];
        CaevoResult result = new CaevoResult(resultFile, false);
        CaevoResult expectedResult = new CaevoResult(expected, true);
        return collectStatistics(expectedResult, result);
    }

    private double[] collectStatistics(CaevoResult expected, CaevoResult actual) {
        double[] stats = new double[9];

        // events precision and recall
        double counter = 0;
        for (Map.Entry<String, String> actualEvent : actual.events.entrySet()) {
            if (expected.events.entrySet().contains(actualEvent)) counter++;
        }
        //event recall
        stats[0] = expected.events.isEmpty() ? 1 : counter / expected.events.size();
        // event precision
        stats[1] = actual.events.isEmpty() ? 1 : counter / actual.events.size();
        // event F1
        stats[2] = (stats[0] + stats[1] == 0) ? 0 : 2 * stats[1] * stats[0] / (stats[1] + stats[0]);

        // timex precision and recall
        counter = 0;
        for (Map.Entry<String, String> actualTimex : actual.timexes.entrySet()) {
            if (expected.timexes.entrySet().contains(actualTimex)) counter++;
        }
        //  timex recall
        stats[3] = expected.timexes.isEmpty() ? 1 : counter / expected.timexes.size();
        // timex precision
        stats[4] = actual.timexes.isEmpty() ? 1 : counter / actual.timexes.size();
        // timex F1
        stats[5] = (stats[3] + stats[4] == 0) ? 0 : 2 * stats[3] * stats[4] / (stats[3] + stats[4]);

        // tlinks precision and recall
        counter = 0;
        for (CaevoResult.TestTLink actualTLink : actual.tlinks) {
            if (CaevoResult.containsTLink(expected.tlinks, actualTLink)) counter++;
        }
        // links recall
        stats[6] = expected.tlinks.isEmpty() ? 1 : counter / expected.tlinks.size();
        // links precision
        stats[7] = actual.tlinks.isEmpty() ? 1 : counter / actual.tlinks.size();
        // links F1
        stats[8] = (stats[6] + stats[7] == 0) ? 0 : 2 * stats[6] * stats[7] / (stats[6] + stats[7]);

        return stats;
    }

    private Map<File, File> mapTestToExpected(File[] instances, File[] expected) throws IOException {
        Map<String, File> expectedMap = new HashMap<String, File>();
        for (File f : expected) {
            expectedMap.put(f.getName(), f);
        }

        Map<File, File> map = new HashMap<File, File>();
        for (File f : instances) {
            File exp = expectedMap.get(f.getName()+ CaevoProperties.getString(EXPECTED_SUFFIX_KEY));
            if (exp != null) {
                map.put(f, exp);
            }
        }
        return map;
    }

    /**
     * Hack for putting the environment variable in place. Would have used mocking to simulate it, except that
     * the method that reads the environment variable is static.
     * @param s the new value of the variable
     * @throws Exception if something unexpected occurs while accessing the variables map.
     */
    private void setEnvironmentVariable(String s) throws Exception {
        Class[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for (Class c1 : classes) {
            if ("java.util.Collections$UnmodifiableMap".equals(c1.getName())) {
                Field field = c1.getDeclaredField("m");
                field.setAccessible(true);
                Object obj = field.get(env);
                Map<String, String> map = (Map<String, String>) obj;
                map.put("JWNL", s);
                break;
            }
        }
    }

}