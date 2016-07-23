package caevo;

import caevo.tlink.TLink;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

class CaevoResult {

    Map<String, String> events = new HashMap<String, String>();
    Map<String, String> timexes = new HashMap<String, String>();
    Set<TestTLink> tlinks = new HashSet<TestTLink>();



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CaevoResult that = (CaevoResult) o;
        if (!events.equals(that.events)) return false;
        if (!timexes.equals(that.timexes)) return false;
        return compareTlinks(that.tlinks);
    }

    private boolean compareTlinks(Set<TestTLink> otherTlinksSet) {
        List<TestTLink> otherTlinks = new ArrayList<TestTLink>(otherTlinksSet);
        if (tlinks.equals(otherTlinks)) return true;
        for (TestTLink testTLink : tlinks) {
            if (!containsTLink(otherTlinksSet, testTLink)) return false;
        }
        return true;
    }

    static boolean containsTLink(Set<TestTLink> set, TestTLink testTLink) {
        if (set.contains(testTLink)) return true;
        TestTLink reverseTestTlink = new TestTLink(testTLink.id2, testTLink.id1, TLink.invertRelation(testTLink.relation));
        return set.contains(reverseTestTlink);
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
                    tlinks.add(new TestTLink(st[1], st[2], st[3]));
                    break;
                default:
                    throw new RuntimeException("Unexpected line format in expected result file " + resultFile.getName());
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
            if (timexId.equals("t0")) continue;
            String timexValue = attributes.getNamedItem("value").getTextContent();
            timexes.put(timexId, timexValue);
        }

        nodeList = (NodeList) tlinkExpression.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            String tlinkEvent1 = attributes.getNamedItem("event1").getTextContent();
            String tlinkEvent2 = attributes.getNamedItem("event2").getTextContent();
            if (tlinkEvent1.equals("t0") || tlinkEvent2.equals("t0")) continue;
            String tlinkRelation = attributes.getNamedItem("relation").getTextContent();
            tlinks.add(new TestTLink(tlinkEvent1, tlinkEvent2, tlinkRelation));
        }
    }

    static class TestTLink {
        public TestTLink(String event1, String event2, String relation) {
            this.id1 = event1;
            this.id2 = event2;
            this.relation = TLink.Type.valueOf(relation);
        }

        TestTLink(String id1, String id2, TLink.Type relation) {
            this.id1 = id1;
            this.id2 = id2;
            this.relation = relation;
        }

        String id1;
        String id2;
        TLink.Type relation;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestTLink tLink = (TestTLink) o;
            if (!id1.equals(tLink.id1)) return false;
            if (!id2.equals(tLink.id2)) return false;
            if (relation != tLink.relation) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = id1.hashCode();
            result = 31 * result + id2.hashCode();
            result = 31 * result + relation.hashCode();
            return result;
        }
    }
}
