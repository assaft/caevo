package caevo;

import caevo.sieves.*;
import caevo.tlink.TLink;
import caevo.util.CaevoProperties;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

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

        File inputDir = new File(CaevoProperties.getString("inputDirPath"));
        File[] instances, expected;
        if (!inputDir.exists()) {
            File inputFile = new File(CaevoProperties.getString("inputFilePath"));
            if (!inputFile.exists()) fail();
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


        List<File> failed = new ArrayList<File>();
        for (File test : instances) {
            String[] args = {test.getAbsolutePath(), "raw"};
            Main.main(args);
            if (tests.keySet().contains(test)) {
                // run all tests, assert only for those with 'expected' files.
                boolean passed = checkResult(test.getAbsolutePath(), tests.get(test));
                if (!passed) failed.add(test);
            }
        }
        if (!failed.isEmpty()) {
            System.out.println("Failed:" +failed);
            fail();
        }
    }

    private boolean checkResult(String testName, File expected) throws Exception {
        File resultFile = new File(testName+CaevoProperties.getString("resultSuffix"));
        if (!resultFile.exists()) return false;
        CaevoResult result = new CaevoResult(resultFile, false);
        CaevoResult expectedResult = new CaevoResult(expected, true);
        if (!result.equals(expectedResult)) return false;
        return true;
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
                map.clear();
                map.put("JWNL", s);
                break;
            }
        }
    }

    class CaevoResult {

        Map<String, String> events = new HashMap<String, String>();
        Map<String, String> timexes = new HashMap<String, String>();
        List<TestTLink> tlinks = new ArrayList<TestTLink>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CaevoResult that = (CaevoResult) o;
            if (!events.equals(that.events)) return false;
            if (!timexes.equals(that.timexes)) return false;
            return tlinks.equals(that.tlinks);
        }

        @Override
        public int hashCode() {
            int result = events.hashCode();
            result = 31 * result + timexes.hashCode();
            result = 31 * result + tlinks.hashCode();
            return result;
        }

        public CaevoResult(File resultFile, boolean expected) throws Exception {
            if (expected) {
                init(resultFile);
            } else {
                initXml(resultFile);
            }
        }

        private void init(File resultFile) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(resultFile));
            String line = reader.readLine();
            while (line != null) {
                String[] st = line.split(",");
                switch (line.charAt(0)) {
                    case 'e':
                        events.put(st[0], st[1]);
                        break;
                    case 't':
                        timexes.put(st[0], st[1]);
                        break;
                    case 'l':
                        tlinks.add(new TestTLink(st[1], st[2], st[3], st[4]));
                        break;
                    default:
                        throw new RuntimeException("Unexpected line format in expected result file "+resultFile.getName());
                }
                line = reader.readLine();
            }
        }

        private void initXml(File resultFile) throws Exception {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(resultFile);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression eventExpression = xPath.compile("//event");
            XPathExpression timexExpression = xPath.compile("//timex");
            XPathExpression tlinkExpression = xPath.compile("//tlink");

            NodeList nodeList = (NodeList) eventExpression.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                NamedNodeMap attributes = node.getAttributes();
                String eventId = attributes.getNamedItem("eiid").getTextContent();
                String eventString = attributes.getNamedItem("string").getTextContent();
                events.put(eventId, eventString);
            }

            nodeList = (NodeList) timexExpression.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                NamedNodeMap attributes = node.getAttributes();
                String timexId = attributes.getNamedItem("tid").getTextContent();
                String timexValue = attributes.getNamedItem("value").getTextContent();
                timexes.put(timexId, timexValue);
            }

            nodeList = (NodeList) tlinkExpression.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                NamedNodeMap attributes = node.getAttributes();
                String tlinkEvent1 = attributes.getNamedItem("event1").getTextContent();
                String tlinkEvent2 = attributes.getNamedItem("event2").getTextContent();
                String tlinkRelation = attributes.getNamedItem("relation").getTextContent();
                String tlinkOrigin = attributes.getNamedItem("origin").getTextContent();
                tlinks.add(new TestTLink(tlinkEvent1, tlinkEvent2, tlinkRelation, tlinkOrigin));
            }
        }

        class TestTLink {
            public TestTLink(String event1, String event2, String relation, String origin) {
                this.event1 = event1;
                this.event2 = event2;
                this.relation = TLink.Type.valueOf(relation);
                if (!types.contains(origin)) throw new RuntimeException("Unknown Sieve: "+origin);
                this.origin = origin;
            }

            String event1;
            String event2;
            TLink.Type relation;
            String origin;

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                TestTLink tLink = (TestTLink) o;
                if (!event1.equals(tLink.event1)) return false;
                if (!event2.equals(tLink.event2)) return false;
                if (relation != tLink.relation) return false;
                return origin.equals(tLink.origin);
            }

            @Override
            public int hashCode() {
                int result = event1.hashCode();
                result = 31 * result + event2.hashCode();
                result = 31 * result + relation.hashCode();
                result = 31 * result + origin.hashCode();
                return result;
            }
        }
    }
    static Set<String> types = new HashSet<String>();
    static {
        String[] typeNames = new String[] {
                AdjacentVerbTimex.class.getSimpleName(),
                QuarterSieveReporting.class.getSimpleName(),
                FrequencyVagueSieve.class.getSimpleName(),
                Dependencies182.class.getSimpleName(),
                TimeTimeSieve.class.getSimpleName(),
                InternalEvents.class.getSimpleName(),
                DependencyAnalyze.class.getSimpleName(),
                WordFeatures5.class.getSimpleName(),
                WordNet209.class.getSimpleName(),
                MLVagueSieve.class.getSimpleName(),
                MLEventTimeDiffSent.class.getSimpleName(),
                MLEventEventSameSent.class.getSimpleName(),
                EventCorefSieve.class.getSimpleName(),
                CleartkTimemlSieve_ImplBase.class.getSimpleName(),
                ReichenbachDG13_update.class.getSimpleName(),
                EventCreationTimeSieve.class.getSimpleName(),
                AllVagueSieve.class.getSimpleName(),
                XCompDepSieve.class.getSimpleName(),
                MLEventTimeSameSent.class.getSimpleName(),
                NominalEvents.class.getSimpleName(),
                BaselineEventDCT.class.getSimpleName(),
                WordFeatures64.class.getSimpleName(),
                MLEventDCT.class.getSimpleName(),
                MLEventEventDiffSent.class.getSimpleName(),
                RepCreationDay.class.getSimpleName(),
                EventEventVagueSieve.class.getSimpleName(),
                StupidSieve.class.getSimpleName(),
                RepEventGovEvent.class.getSimpleName(),
                MLEventGovDep.class.getSimpleName()
        };
        types.addAll(Arrays.asList(typeNames));
    }
}