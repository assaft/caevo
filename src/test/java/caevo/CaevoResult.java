package caevo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
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

class CaevoResult {

    Map<String, TestEvent> events = new HashMap<String, TestEvent>();
    Map<String, String> timexes = new HashMap<String, String>();
    Map<String, TestTLink> tlinks = new HashMap<String, TestTLink>();

    @Override
    public String toString() {
    	return 	"events:  " + events.toString() + "\n" + 
    					"timexes: " + timexes.toString() + "\n" +
    					"tlinks:  " + tlinks.toString();
    }

    public String toSTF() {
    	StringBuilder buffer = new StringBuilder();
    	
    	Map<String,TestEvent> sortedEvents = new TreeMap<String,TestEvent>(events);
    	for (Entry<String, TestEvent> entry : sortedEvents.entrySet()) {
    		buffer.append(entry.getKey() + "," + entry.getValue()+ "\n");
    	}
    	
    	Map<String,String> sortedTimes = new TreeMap<String,String>(timexes);
    	for (Entry<String, String> entry : sortedTimes.entrySet()) {
    		buffer.append(entry.getKey() + "," + entry.getValue() + "\n");
    	}

    	Map<String,TestTLink> sortedLinks = new TreeMap<String,TestTLink>(tlinks);
    	for (Entry<String, TestTLink> entry : sortedLinks.entrySet()) {
    		buffer.append(entry.getKey() + "," + entry.getValue().toSTF()+ "\n");
    	}
    	
    	return buffer.toString();
    }
    
    
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
    
    public CaevoResult(String resultFileContent, boolean expected) throws Exception {
      if (expected) {
          init(resultFileContent);
      } else {
          initXml(resultFileContent);
      }
    }

    private void init(File resultFile) throws IOException {
    	init(new FileReader(resultFile));
    }

    private void init(String resultFileContent) throws IOException {
    	init(new InputStreamReader(new ByteArrayInputStream(resultFileContent.getBytes("UTF-8"))));
    }

    private void init(Reader reader) throws IOException {
    	Set<String> lids = new HashSet<String>();
    	BufferedReader bufferedReader = new BufferedReader(reader);
    	String line = bufferedReader.readLine();
    	while (line != null && !line.trim().isEmpty()) {
    		String[] st = line.split(",");
    		switch (line.charAt(0)) {
    		case 'e':
    			String eid = st[0].trim();
    			String etext = st[1].trim();
                TextEvent.Class eClass = null;
                if (st.length > 2) {
                    String eClassName = st[2].trim();
                    eClass = TextEvent.Class.valueOf(eClassName);
                }
                if (!events.containsKey(eid)) {
                    events.put(eid, new TestEvent(eid, etext, eClass));
                } else {
    				throw new RuntimeException("Event " + eid + " is defined twice");
    			}
    			break;
    		case 't':
    			String tid = st[0].trim();
    			String ttext = st[1].trim();
    			if (!timexes.containsKey(tid)) {
    				timexes.put(tid,ttext);
    			} else {
    				throw new RuntimeException("TimeX " + tid + " is defined twice");
    			}
    			break;
    		case 'l':
    			String lid = st[0].trim();
    			if (!lids.contains(lid)) {
    				String magnitude;
    				String relation;
    				if (st.length==4) {
    					magnitude = null;
    					relation = st[3].trim();
    				} else {
    					magnitude = st[3].trim();
    					relation = st[4].trim();
    				}
    				tlinks.put(lid,new TestTLink(st[1].trim(), st[2].trim(), magnitude, relation));
    				lids.add(lid);
    			} else {
    				throw new RuntimeException("Link " + lid + " is defined twice");
    			}
    			break;
    		default:
    			throw new RuntimeException("Unexpected line format: [" + line + "]");
    		}
    		line = bufferedReader.readLine();
    	}
    	reader.close();
    }

    private void initXml(String resultFileContent) throws Exception {
      initXml( new ByteArrayInputStream(resultFileContent.getBytes("UTF-8")));
    }

    private void initXml(File resultFile) throws Exception {
      initXml(new FileInputStream(resultFile));
    }
    
    private void initXml(InputStream resultFile) throws Exception {
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
            TextEvent.Class eventClass = TextEvent.Class.valueOf(attributes.getNamedItem("class").getTextContent());
            events.put(eventId, new TestEvent(eventId, eventString, eventClass));
        }

        nodeList = (NodeList) timexExpression.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            String timexId = attributes.getNamedItem("tid").getTextContent();
            //if (timexId.equals("t0")) continue;
            String timexValue = attributes.getNamedItem("value").getTextContent();
            timexes.put(timexId, timexValue);
        }

        nodeList = (NodeList) tlinkExpression.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NamedNodeMap attributes = node.getAttributes();
            String tlinkEvent1 = attributes.getNamedItem("event1").getTextContent();
            String tlinkEvent2 = attributes.getNamedItem("event2").getTextContent();
            Node magnitudeAttribute = attributes.getNamedItem("magnitude"); 
            String tlinkMagnitude = magnitudeAttribute!=null ? magnitudeAttribute.getTextContent() : null;
            //if (tlinkEvent1.equals("t0") || tlinkEvent2.equals("t0")) continue;
            String tlinkRelation = attributes.getNamedItem("relation").getTextContent();
            tlinks.put("l"+(i+1),new TestTLink(tlinkEvent1, tlinkEvent2, tlinkMagnitude, tlinkRelation));
        }
    }
    
    public static class ElementPair {
    	
    	private final String element1;
    	private final String element2;
    	
    	public ElementPair(String element1, String element2) {
    		this.element1 = element1;
    		this.element2 = element2;
    	}

			public String getElement1() {return element1;}
			public String getElement2() {return element2;}
			
			public String toString() {
				return "(" + element1 + "," + element2 + ")";
			}
			
      @Override
    	public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElementPair p = (ElementPair)o;
        return ((p.element1.equals(element1) && p.element2.equals(element2)) ||
						 	  (p.element1.equals(element2) && p.element2.equals(element1)));
    	}
    	
      @Override
      public int hashCode() {
      	return element1.hashCode()*element2.hashCode();
      }
    }
    
    public Set<ElementPair> checkDensity(){
    	Set<ElementPair> missingLinks = new HashSet<ElementPair>();
    	List<String> allElements = new ArrayList<String>();
    	allElements.addAll(events.keySet());
    	allElements.addAll(timexes.keySet());
    	Set<ElementPair> links = new HashSet<ElementPair>();
      for (TestTLink t : tlinks.values()){
      	links.add(new ElementPair(t.id1,t.id2));
      	if (t.id3!=null) {
      		links.add(new ElementPair(t.id1,t.id3));
      		links.add(new ElementPair(t.id2,t.id3));
      	}
    	}
    	for (int i=0 ; i<allElements.size() ; i++) {
  			String ei1 = allElements.get(i);
    		for (int j=0 ; j<allElements.size() ; j++) {
    			String ei2 = allElements.get(j);
    			ElementPair currentPair = new ElementPair(ei1,ei2);
    			if (i!=j && !links.contains(currentPair)) {
    				missingLinks.add(currentPair);
    			}
    		}
    	}
    	return missingLinks;
    }

    
    public enum MeasureType {Precision,Recall,F1}
    
    public static class Measures {
    	private final Map<MeasureType,Double> measures = new HashMap<CaevoResult.MeasureType, Double>();

    	public Measures(double precision, double recall) {
				measures.put(MeasureType.Precision, precision);
				measures.put(MeasureType.Recall, recall);
				measures.put(MeasureType.F1, precision==0 && recall==0 ? 0 : 2 * precision * recall / (precision + recall));
			}
    	
			public double getMeasure(MeasureType type) 	{return measures.get(type);}
    }
    
    public enum EntityType {Event,Time,Link}
    
    public static class TimelineMeasures {
    	private final Map<EntityType,Measures> measures = new HashMap<CaevoResult.EntityType, CaevoResult.Measures>();
    	
			public TimelineMeasures(Measures eventMeasures, Measures timeMeasures,
					Measures linkMeasures) {
				super();
				measures.put(EntityType.Event, eventMeasures);
				measures.put(EntityType.Time, timeMeasures);
				measures.put(EntityType.Link, linkMeasures);
			}

			public Measures getMeasures(EntityType type) 	{return measures.get(type);}
    }
    
    public static Measures measure(Set<String> goldSet, Set<String> expectedSet, Map<String,String> mapping) {
    	return new Measures(
    			(double)mapping.size()/(double)expectedSet.size(),
    			(double)mapping.size()/(double)goldSet.size());
    }
    
    
    public static TimelineMeasures calcMeasures(CaevoResult gold, CaevoResult expected, Map<String,String> mapping) {

    	/* example:
    	 * 
    	 * gold		expected	mapping
    	 * ----		--------	-------
    	 * ei1		ei1				ei2-ei1
    	 * ei2		t0				t0-t0
    	 * t0			t1				t1-t2
    	 * t1			t2				i3-i1
    	 * t2			i1				
    	 * l1			i2
    	 * l2
    	 * l3
    	 * l4
    	 */
    	
    	Map<String, String> eventMapping = new HashMap<String, String>();
    	Map<String, String> timeMapping = new HashMap<String, String>();
    	Map<String, String> linkMapping = new HashMap<String, String>();
    	
    	for (Map.Entry<String, String> entry : mapping.entrySet()) {
    		Map<String, String> map = null;
    		if (!entry.getKey().isEmpty()) {
	    		switch (entry.getKey().charAt(0)) {
	    		case 'e':
	    			map = eventMapping;
	    			break;
	    		case 't':
	    			map = timeMapping;
	    			break;
	    		case 'l':
	    			map = linkMapping;
	    			break;
	    		}
    		}
    		if (map!=null) {
    			map.put(entry.getKey(),entry.getValue());
    		} else {
    			throw new RuntimeException("Unexpected line format: [" + entry.getKey() + "]");
    		}
    	}
    	
    	return new TimelineMeasures(
    			measure(gold.events.keySet(), expected.events.keySet(), 	eventMapping), 
    			measure(gold.timexes.keySet(),expected.timexes.keySet(), timeMapping),
    			measure(gold.tlinks.keySet(), expected.tlinks.keySet(), 	linkMapping));
    }

    private static class TestEvent {
        String id;
        String value;
        TextEvent.Class clasS;

        public TestEvent(String id, String value, TextEvent.Class clasS) {
            this.id = id;
            this.value = value;
            this.clasS = clasS;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestEvent testEvent = (TestEvent) o;

            if (clasS != testEvent.clasS) return false;
            if (!id.equals(testEvent.id)) return false;
            if (!value.equals(testEvent.value)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + value.hashCode();
            result = 31 * result + (clasS != null ? clasS.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            String str = id + "," + value;
            if (clasS != null) str += "," + clasS;
            return str;
        }
    }

    private static class TestTLink {
    	
    	public TestTLink(String event1, String event2, String magnitude, String relation) {
            this.id1 = event1;
            this.id2 = event2;
            this.id3 = magnitude;
            this.relation = TLink.Type.valueOf(relation);
        }

        String id1;
        String id2;
        String id3; // magnitude
        TLink.Type relation;

        @Override
        public String toString() {
        	return id1 + " " + relation + " " + id2 + (id3!=null ? "by " + id3 : "");
        }
        
        public String toSTF() {
        	return id1 + "," + id2 + "," + (id3!=null ? id3 + "," : "") + relation;
        }        
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestTLink tLink = (TestTLink) o;
            if (!id1.equals(tLink.id1)) return false;
            if (!id2.equals(tLink.id2)) return false;
            if ((id3==null && tLink.id3!=null) || 
            		(id3!=null && tLink.id3==null)) return false;
            if (id3!=null && tLink.id3!=null) {
            	if (!id3.equals(tLink.id3)) return false;
            }
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
