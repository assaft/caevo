package caevo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.jdom.Element;
import org.jdom.Namespace;

import caevo.Timex.Type;
import caevo.time.CorefParser;
import caevo.time.TimexCorefResolver;
import caevo.tlink.EventEventLink;
import caevo.tlink.EventTimeLink;
import caevo.tlink.TLink;
import caevo.tlink.TimeTimeLink;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreLabel.OutputFormat;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

/**
 * Timebank Corpus file that stores in an easier to read format, all the sentences and docs
 * from the TimeBank corpus, as well as event information on a per-sentence basis.
 * @author chambers
 */
public class SieveDocument {
	private String docname;
	private List<SieveSentence> sentences;
	private List<Timex> dcts;
	private List<TLink> tlinks;
	private List<TLink> timeRefTLinks;
	
	private HashMap<String, TextEvent> eiidToEvent;
	private HashMap<String, Timex> tidToTimex;
	
	private List<List<TextEvent>> eventCorefSets;
	
	private CorefParser corefParser;

	
  public SieveDocument(String name) {
  	docname = name;
  	eiidToEvent = new HashMap<String, TextEvent>();
  	tidToTimex = new HashMap<String, Timex>();
  	eventCorefSets = new ArrayList<List<TextEvent>>();
  	corefParser = new CorefParser();
  }
  
  /**
   * Adds a sentence to the file
   * @param tokens These are the tokens in the sentence, listed one per line. Each line has three parts: (1) characters before, (2) original token, (3) characters after.
   */
  public void addSentence(String text, List<CoreLabel> tokens, String parse, String deps, List<TextEvent> events, List<Timex> timexes, List<Timex> innerTimexes) {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();
  	int sid = sentences.size();
  	SieveSentence sent = new SieveSentence(this, sid, text, parse, deps, tokens, events, timexes, innerTimexes);
  	
  	sentences.add(sent);
  	addTimexesToTidMap(timexes);
  	addTimexesToTidMap(innerTimexes);
  	addEventsToEiidMap(events);
  }
  
  public void addSentence(SieveSentence sent) {
  	if( sentences == null ) sentences = new ArrayList<SieveSentence>();
  	sentences.add(sent);
  	addTimexesToTidMap(sent.timexes());
  	addTimexesToTidMap(sent.innerTimexes());
  	addEventsToEiidMap(sent.events());
  }

  /**
   * Add events to a particular sentence.
   */
  public void addEvents(int sid, List<TextEvent> events) {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();
  	sentences.get(sid).addEvents(events);
  	addEventsToEiidMap(events);
  }
  
  /** 
   * Add a set containing coreferring event mentions 
   */
  public void addEventCorefSet(List<String> corefMentionIds) {
  	List<TextEvent> events = new ArrayList<TextEvent>();
  	for (String eiid : corefMentionIds)
  		events.add(this.getEventByEiid(eiid));
  	eventCorefSets.add(events);
  }
  
  /**
   * Add timexes to a particular sentence.
   */
  public void addTimexes(int sid, List<Timex> timexes) {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();  	
  	sentences.get(sid).addTimexes(timexes);
  	addTimexesToTidMap(timexes);
  }

  /**
   * Add inner timexes to a particular sentence.
   */
  public void addInnerTimexes(int sid, List<Timex> innerTimexes) {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();  	
  	sentences.get(sid).addInnerTimexes(innerTimexes);
  	addTimexesToTidMap(innerTimexes);
  }

  private void addEventsToEiidMap (List<TextEvent> events) {
  	if( events != null ) {
  		for (TextEvent event : events) {
  			List<String> eiids = event.getAllEiids();
  			for (String eiid : eiids)
  				eiidToEvent.put(eiid, event);
  		}
  	}
  }
  
  private void addTimexesToTidMap(List<Timex> timexes) {
  	if( timexes != null ) {
  		for (Timex timex : timexes) {
  			tidToTimex.put(timex.getTid(), timex);
  		}
  	}
  }

  private void addTimexToTidMap(Timex timex) {
  	if( timex != null )
  		tidToTimex.put(timex.getTid(), timex);
  }

  /**
   * Adds a list of tlinks to the XML file
   * @param tlinks Vector of TLink objects
   */
  public void addTlinks(List<TLink> newlinks) {
  	if( tlinks == null ) 
  		tlinks = new ArrayList<TLink>();
  	tlinks.addAll(newlinks);
  	for (TLink tlink : newlinks)
  		tlink.setDocument(this);
  }

  /**
   * Adds a list of time-referring tlinks to the XML file
   * @param tlinks Vector of TLink objects
   */
  public void addTimeRefTlinks(List<TLink> newlinks) {
  	if( timeRefTLinks == null ) 
  		timeRefTLinks = new ArrayList<TLink>();
  	timeRefTLinks.addAll(newlinks);
  	for (TLink tlink : newlinks)
  		tlink.setDocument(this);
  }  
  
  public void addTlink(TLink newlink) {
  	if( tlinks == null ) 
  		tlinks = new ArrayList<TLink>();
  	tlinks.add(newlink);
  	newlink.setDocument(this);
  }

  public void removeTlink(TLink link) {
  	if( tlinks != null )
  		tlinks.remove(link);
  }
  
  /**
   * Deletes all the TLinks from the XML file
   */
  public void removeTlinks() {
  	if (tlinks == null)
  		return;
  	
  	for (TLink tlink : tlinks)
  		tlink.setDocument(null);
  	tlinks.clear();
  }

  /**
   * @return A List of TLink objects, except for those created from transitive closure.
   */
  public List<TLink> getTlinksNoClosures() {
    List<TLink> keep = new ArrayList<TLink>();

    if (tlinks == null)
    	return keep;
    
    for( TLink link : tlinks )
    	if( !link.isFromClosure() ) 
    		keep.add(link);
    
    return keep;
  }

  /**
   * @return A List of all TLink objects in the document.
   */
  public List<TLink> getTlinks() {
  		return tlinks;
  }

  /**
   * @return A List of all TLink objects in the document.
   */
  public List<TLink> getTimeRefTlinks() {
  		return timeRefTLinks;
  }
  /**
   * @return A list of lists. 
   *         This is a list of sentences, each sentence is a list of CoreLabels with character information.
   */
  public List<List<CoreLabel>> getTokensAllSentences(String file) {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();    
  	
  	List<List<CoreLabel>> tokens = new ArrayList<List<CoreLabel>>();
    for( SieveSentence sent : sentences )
    	tokens.add(sent.tokens());    
    return tokens;
  }
  

  /**
   * @return A List of Lists of all Timex objects, excluding the inner ones. 
   * 					This does not return the document creation time! 
   *         
   * 
   */
  public List<List<Timex>> getTimexesBySentence() {
  	return getTimexesBySentence(false);
  }
  
  /**
   * @return A List of Lists of all Timex objects. This does not 
   *         return the document creation time!
   * @param whether to include inner timexes or not
   */
  public List<List<Timex>> getTimexesBySentence(boolean incInnerTimexes) {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();    
  	
  	List<List<Timex>> timexes = new ArrayList<List<Timex>>();
    
    // Timexes in each sentence.
    for( SieveSentence sent : sentences ) {
      List<Timex> list = new ArrayList<Timex>(sent.timexes());
      if (incInnerTimexes) {
      	list.addAll(sent.innerTimexes());
      }
    	timexes.add(list);
    }
    return timexes;
  }
  
  /**
	 * @return A List of TLink objects Event-Event, no Event-Time links
	 */
	public List<TLink> getTlinksOfType(Class<?> linkClass) {
		List<TLink> keep = new ArrayList<TLink>();
	  for( TLink link : tlinks ) {
	  	if( link.getClass().equals(linkClass) )
	      keep.add(link);
	  }
	  return keep;
	}

	/**
   * @return A List of all Timex objects, including the document creation time.
   */
  public List<Timex> getTimexes() {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();
  	
    List<Timex> timexes = new ArrayList<Timex>();
    
    // Timexes in each sentence.
    for( SieveSentence sent : sentences )
      timexes.addAll(sent.timexes());
    
    // Document time stamps.
    if (dcts != null)
    	timexes.addAll(dcts);
    
    return timexes;
  }

  public Timex getTimexByTid(String tid) {
  	if (tidToTimex.containsKey(tid)) 
  		return tidToTimex.get(tid);
  	else
  		return null;
  }
  
  /**
   * @return A List of all Event objects in one document (file parameter)
   */
  public List<List<TextEvent>> getEventsBySentence() {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();
  	
    List<List<TextEvent>> allEvents = new ArrayList<List<TextEvent>>();
    for( SieveSentence sent : sentences )
    	allEvents.add(sent.events());
    return allEvents;
  }
  
  /**
   * @return A List of all Event objects in one document (file parameter)
   */
  public List<TextEvent> getEvents() {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();
  	
    List<TextEvent> events = new ArrayList<TextEvent>();
    for( SieveSentence sent : sentences )
    	events.addAll(sent.events());
    return events;
  }
  
  public List<List<TextEvent>> getEventCorefSets() {
  	return eventCorefSets;
  }
  
  public TextEvent getEventByEiid(String eiid) {
  	if (eiidToEvent.containsKey(eiid)) 
  		return eiidToEvent.get(eiid);
  	else
  		return null;
  }

  /**
   * @return A Timex object for the document creation time
   */
  public List<Timex> getDocstamp() {
  	return dcts;
  }

  public void setDocname(String name) { docname = name; }
  
  /**
   * @desc Adds the document stamp to the file, as a Timex
   */
  public void addCreationTime(Timex timex) {
  	if( dcts == null ) dcts = new ArrayList<Timex>();
  	dcts.add(timex);
  	addTimexToTidMap(timex);
  }

  /**
   * @return A Vector of Sentence objects
   */
  public List<SieveSentence> getSentences() {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();
  	
  	return sentences;
  }

  public String getDocname() { return docname; }
  
  /**
   * @return A List of Strings that are parse trees
   */
  public List<Tree> getAllParseTrees() {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();
  	
  	List<Tree> trees = new ArrayList<Tree>();
  	for( SieveSentence sent : sentences )
  		trees.add(sent.getParseTree());
  	return trees;
  }

  /**
   * @return A List of Strings, one string per sentence, representing dependencies.
   */
  public List<List<TypedDependency>> getAllDependencies() {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();
  	
  	List<List<TypedDependency>> alldeps = new ArrayList<List<TypedDependency>>();
  	for( SieveSentence sent : sentences )
  		alldeps.add(sent.getDeps());
  	return alldeps;
  }
    
  public String markupOriginalText() {
  	return markupOriginalText(TextEvent.NAME_ELEM, "eid", false, true, true, true);
  }
  
  /**
   * This function does not output a formal XML document, but instead just the raw text 
   * of a document annotated with XML markup around events and timexes.
   * @param eventElemName You can specify what you want the event's XML element to be (e.g., event or target)
   * @param idAttributeString You can specify what you want the event's XML attribute to be for its ID (e.g., id or eid)
   * @return A String which is the raw text with added XML markup around events and timexes.
   */
  public String markupOriginalText(String eventElemName, String idAttributeString, boolean numericIDOnly,
  		boolean showTense, boolean showAspect, boolean showClass) {
  	StringBuffer buf = new StringBuffer();
  	boolean firstToken = true;
  	
  	System.out.println("markup " + docname + " containing " + sentences.size() + " sentences.");
  	for( SieveSentence sent : sentences ) {
  		List<CoreLabel> tokens = sent.tokens();
  		// Grab the events.
  		Map<Integer,TextEvent> indexToEvents = new HashMap<Integer,TextEvent>();
  		if( sent.events() != null )
  			for( TextEvent event : sent.events() )
  				indexToEvents.put(event.getIndex(), event);
  		// Grab the timexes.
  		Map<Integer,Timex> indexToTimexes = new HashMap<Integer,Timex>();
  		if( sent.timexes() != null )
  			for( Timex timex : sent.timexes() )
  				indexToTimexes.put(timex.getTokenOffset(), timex);  		
  		Set<Integer> endTimexes = new HashSet<Integer>();
  		
      int ii = 1;
  		for( CoreLabel token : tokens ) {
  			if( firstToken ) {
  				buf.append(token.getString(CoreAnnotations.BeforeAnnotation.class));
  				firstToken = false;
  			}
  			boolean endevent = false;
  			
  			// If this token is marked as an event.
  			if( indexToEvents.containsKey(ii) ) {
  				TextEvent event = indexToEvents.get(ii);
  				String eventid = event.getId();
  				if( numericIDOnly && eventid.startsWith("e") ) eventid = eventid.substring(1);
  				buf.append("<" + eventElemName);
  				buf.append(" " + idAttributeString + "=\"" + eventid + "\"");
  				if( showTense )
  					buf.append(" tense=\"" + event.getTense() + "\"");
  				if( showAspect )
  					buf.append(" aspect=\"" + event.getAspect() + "\"");
  				if( showClass )
  					buf.append(" class=\"" + event.getTheClass() + "\"");
  				buf.append(">");
  				endevent = true;
  			}

  			// If this token starts a TIMEX.
  			if( indexToTimexes.containsKey(ii) ) {
  				Timex timex = indexToTimexes.get(ii);
//  				System.out.println("timex: " + timex);
  				buf.append(timex.toXMLString());
  				endTimexes.add(ii+timex.getTokenLength()-1);
  			}

  			// Print the token.
  			String str = token.getString(CoreAnnotations.OriginalTextAnnotation.class);
//  			System.out.println("token=" + str + " len=" + str.length());
  			str = str.replaceAll("&", "&amp;");
//  			System.out.println("\tnow=" + str);
  			buf.append(str);
  				
  			if( endevent ) buf.append("</" + eventElemName + ">");
  			if( endTimexes.contains(ii) ) buf.append("</TIMEX3>");
  			
  			buf.append(token.getString(CoreAnnotations.AfterAnnotation.class));
  			ii++;
  		}
  	}
  	return buf.toString();
  }
  
  private static TLink tlinkFromElement(Element el) {
    if( el.getAttributeValue(TLink.TLINK_TYPE_ATT).equals(TLink.EVENT_EVENT_TYPE_VALUE) )
      return new EventEventLink(el);
    else if( el.getAttributeValue(TLink.TLINK_TYPE_ATT).equals(TLink.EVENT_TIME_TYPE_VALUE) )
      return new EventTimeLink(el);
    else if( el.getAttributeValue(TLink.TLINK_TYPE_ATT).equals(TLink.TIME_TIME_TYPE_VALUE) )
      return new TimeTimeLink(el);
    System.err.println("ERROR: tlink element doesn't have a tlink type attribute");
    return null;
  }

  public static SieveDocument fromXML(Element el) {
  	String docname = el.getAttributeValue(SieveDocuments.FILENAME_ELEM);
  	SieveDocument newdoc = new SieveDocument(docname);
  	
    Namespace ns = Namespace.getNamespace(SieveDocuments.INFO_NS);
    try {
    	// Sentences
      List<Element> sentElems = el.getChildren(SieveDocuments.ENTRY_ELEM, ns);
      for( Element sentenceObj : sentElems ) {
      	SieveSentence sent = SieveSentence.fromXML(sentenceObj);
      	sent.setParent(newdoc);
      	newdoc.addSentence(sent);
      }    	
      
      // TLinks
      List children = el.getChildren(TLink.TLINK_ELEM,ns);
      for( Object obj : children ) {
      	newdoc.addTlink(tlinkFromElement((Element)obj));
      }
      
      // Document creation time(s)
      List<Element> elements = el.getChildren(Timex.TIMEX_ELEM,ns);
      for( Element stamp : elements )
        newdoc.addCreationTime(new Timex(stamp));

      if( newdoc.getDocstamp() == null ) System.out.println("WARNING: no docstamp in " + newdoc.getDocname());
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return newdoc;
  }

  public Element toXML() {
    Namespace ns = Namespace.getNamespace(SieveDocuments.INFO_NS);

    // Find the file's Element
    Element mainfile = new Element(SieveDocuments.FILE_ELEM, ns);
    mainfile.setAttribute(SieveDocuments.FILENAME_ELEM, docname);

    if( sentences != null )
    	for( SieveSentence sent : sentences )
    		mainfile.addContent(sent.toXML());
    
    // Check for duplicates as we add
    if( tlinks != null )
    	for( TLink tlink : tlinks )
    		mainfile.addContent(tlink.toElement(ns));
    
    if( dcts != null )
    	for( Timex dct : dcts )
    		mainfile.addContent(dct.toElement(ns));
    	
    return mainfile;
  }
  
  
  /**
   * Create a list of strings, each string is one TLink: <TLINK ... />
   * @return A list of tlink strings.
   */
  public List<String> createTLinkStrings() {
  	List<String> strings = new ArrayList<String>();
  	int counter = 1;
  	
  	List<TLink> tlinks = getTlinks();
  	for( TLink link : tlinks ) {
  		String str = "<TLINK lid=\"l" + counter + "\" relType=\"" + link.getRelation().toString() + "\" ";
  		
  		if( link instanceof EventEventLink ||
  				(link instanceof EventTimeLink && link.getId1().startsWith("e")) )
  			str += "eventInstanceID=\"";
  		else
  			str += "timeID=\"";
  		str += link.getId1() + "\" ";
  		
  		if( link instanceof EventEventLink ||
  				(link instanceof EventTimeLink && link.getId2().startsWith("e")) )
  			str += "relatedToEventInstance=\"";
  		else
  			str += "relatedToTime=\"";
  		str += link.getId2() + "\"";

  		str += " />";
  		
  		strings.add(str);
  		counter++;
  	}
  	
  	return strings;
  }

  /**
   * Create a list of strings, each string is one makeinstance: <MAKEINSTANCE eventID="3" ... />
   * @param docname The document from which you want to extract all makeinstances.
   * @return A list of makeinstance strings.
   */
  public List<String> createMakeInstanceStrings() {
  	if( sentences == null ) 
  		sentences = new ArrayList<SieveSentence>();
  	
    List<String> strings = new ArrayList<String>();

    for( SieveSentence sent : sentences ) {
      for( TextEvent event : sent.events() ) {
        for( String eiid : event.getAllEiids() ) {
          strings.add("<MAKEINSTANCE eventID=\"" + event.getId() + 
              "\" eiid=\"" + eiid + 
              "\" tense=\"" + event.getTense() + 
              "\" aspect=\"" + event.getAspect() + 
              ( event.getPolarity() != null ? ("\" polarity=\"" + event.getPolarity()) : "") + 
          "\" />");
        }
      }
    }

    return strings;
  }
  
  public void outputMarkedUp(String dirpath) {
  	try {
  		String outfile = this.docname;
  		// Strip off the ending ".TE3input" if it exists (for TempEval3)
  		//    	  if( outfile.endsWith(".TE3input") ) outfile = outfile.substring(0, outfile.lastIndexOf(".TE3input")); 

  		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(dirpath + File.separator + outfile)));

  		writer.write("<?xml version=\"1.0\" ?>\n");
  		writer.write("<TimeML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://timeml.org/timeMLdocs/TimeML_1.2.1.xsd\">\n\n");

  		writer.write("<DCT>");
  		if( dcts == null || dcts.size() == 0 )
  			System.err.println("ERROR: " + docname + " does not have a DCT to write.");
  		else {
  			writer.write(dcts.get(0).toXMLString());
  			writer.write(dcts.get(0).getText());
  		}
  		writer.write("</TIMEX3></DCT>\n\n");

  		// The text.
  		writer.write("<TEXT>");
  		writer.write(markupOriginalText());
  		writer.write("</TEXT>\n\n");

  		// Event makeinstance list.
  		List<String> makes = createMakeInstanceStrings();
  		for( String make : makes ) writer.write(make + "\n");

  		// TLinks.
  		List<String> tlinks = createTLinkStrings();
  		for( String tlink : tlinks ) writer.write(tlink + "\n");

  		writer.write("\n</TimeML>");
  		writer.close();
  	} catch( Exception ex ) { ex.printStackTrace(); }
  }
  
  
  /*
  public void resolveTimexCoref(List<CorefType> corefTypes) {
    TimexCorefResolver resolver = new TimexCorefResolver(sentences);
  	
    // go over the sentences
    for (SieveSentence sent : sentences) {

    	List<Timex> timexList = sent.timexes();
    	
    	
      System.out.println("printing timexes:");
      for (Timex timex : timexList) {
      	System.out.println(timex + ": " + timex.getTokenOffset() + "-" + timex.getTokenLength());
      }

      if (1==1) 
      	continue;
    	
    	// go over all timexes 
    	for (int timexId = 0, size=timexList.size() ; timexId<size ; timexId++) {
    		
    		// the current timex
    		Timex timex = sent.timexes().get(timexId);
    		
    		// find possible candidate references
    		List<String> candidates = resolver.resolve(timex.getTid());

    		// check each candidate
    		Timex newTimex = null;
    		for (int i=0, length=candidates.size() ; i<length && newTimex==null ; i++) {
    			// get the id and timex
    			String refTId = candidates.get(i);
    			Timex refTimex = getTimexByTid(refTId);
    			
    			// extend the span of text of the current timex (for cases where SUTime missed part of it)
    			Timex extTimex = getExtendedTimex(timex,sent.tokens());
    			
    			// check if resolvable
    			newTimex = null; //corefParser.check(extTimex, refTimex, corefTypes);
    		}
    		
    		// we expect to find a reference
  			if (newTimex!=null) {
  				
  				// we update only if the value has been changed
  				if (!newTimex.getValue().equals(timex.getValue())) {
      			//update list and map
      			sent.timexes().set(timexId, newTimex);
      			tidToTimex.put(timex.getTid(), newTimex);
  					System.out.println("Co-ref update: [" + timex + "] replaced by: [" + newTimex+"]");
  				}
  			} else {
					System.err.println("Unresolved co-reference: " + timex);
  			}
    	}      			
    }
  	
  }*/
  

  /*
	private void preprocess(List<CorefType> asList) {
		
    // go over the sentences
    for (SieveSentence sent : sentences) {

    	// go over all timexes 
    	for (int timexId = 0 ; timexId<sent.timexes().size() ; timexId++) {
    		
    		// the current timex
    		Timex timex = sent.timexes().get(timexId);
    		
  			// check if resolvable
    		Timex newTimex = null; //corefParser.checkArithmetic(timex);
    		
    		// we expect to find a reference
  			if (newTimex!=null) {
  				// we update only if the value has been changed
  				if (!newTimex.getValue().equals(timex.getValue())) {
      			//update list and map
      			sent.timexes().set(timexId, newTimex);
      			tidToTimex.put(timex.getTid(), newTimex);
  					System.out.println("Co-ref update: [" + timex + "] replaced by: [" + newTimex+"]");
  				}
  			}
    	}
    }
	}  
  */
  public void markupTimexDependencies() {
		
    // step 1 - resolve cases of TIMEREF ("in *that year*") and MERGE ("in *September*") 
  	//resolveTimexCoref(Arrays.asList(CorefType.TIMEREF, CorefType.MERGE));
  	
  	// step 2 identify pairs of events that are linked by an advcl or nmod:after dependency
  	//preprocess(Arrays.asList(CorefType.ARITHMETIC));
    
    // step 2 - sort duratives to TIMEREF and EVENTREF
    
    // step 3 - resolve TIMEREF
    
  }


	private static final List<List<String>> skippedTimexInitials;

	static {
		skippedTimexInitials = new ArrayList<List<String>>();
		skippedTimexInitials.add(Arrays.asList("that","same"));
		skippedTimexInitials.add(Arrays.asList("that"));
	}

  private Timex getExtendedTimex(Timex timex, List<CoreLabel> tokens) {

  	// Problem description:
  	// In some cases SUTime doesn't recognize the full span of a co-ref timex. This problem was observed
  	// with respect to "that same year", as in: In 1948, .... In that same year,..... SUTime analyzes 
  	// the co-ref as 'year' and ignores "that same". This causes two problems:
  	// 1. Our rules for parsing co-ref patterns do not handle this co-ref - 'year'.
  	// 2. caevo's rules that look for the preposition that comes before the timex find there 'same' 
  	// because this is what appears one word before the timex, instead of finding 'in'.
  	// Solution:
  	// We extend the span of text that SUTime captured and include articles that appear what SUTime found.
  	// Note that currently we are unaware of more limitations in addition to "that same year/month/etc" 
  	// but this code can be extended for future cases.
  	
  	Timex result = timex;
  	int tokenOffset = timex.getTokenOffset();
  	int initialsCount = skippedTimexInitials.size();
  	boolean initalFound = false;
  	for (int initialId=0 ; initialId<initialsCount && !initalFound ; initialId++) {
  		List<String> initial = skippedTimexInitials.get(initialId);
  		int initialSize = initial.size(); 
  		if (tokenOffset-1-initialSize>=0) {
  			boolean stop = false;
  			for (int i=0, length=initialSize ; i<length && !stop ; i++) {
  				stop = !tokens.get(tokenOffset-1-initialSize+i).word().toLowerCase().equals(initial.get(i));
  			}
  			if (!stop) {
  	  		initalFound = true;
  	  		String newText = String.join(" ",initial) + " " + timex.getText();
  	  		int newTokenOffset = timex.getTokenOffset()-initial.size();
  	  		int newTokenLength = timex.getTokenLength()+initial.size();
  	  		result = new Timex(timex,newText,newTokenOffset,newTokenLength);
  			}
  		}
  	}
  	return result;
  }

/**
   * This main function is only here to print out automatic time-time links!
   */
  public static void main(String[] args) {
    if( args.length < 1 ) {
      System.err.println("InfoFile <infopath> <markup-out-dir> markup");
    }

    else if( args[args.length-1].equals("markup") ) {
    	InfoFile info = new InfoFile();
      info.readFromFile(new File(args[0]));
      info.outputMarkedUp(args[1]);
    }
    
    // InfoFile <info> count
    else if( args[args.length-1].equals("count") ) {
      InfoFile info = new InfoFile();
      info.readFromFile(new File(args[0]));
//      info.countEventDCTLinks();
      info.countEventPairLinks();
    }
    
    else {
      InfoFile info = new InfoFile();
      info.readFromFile(new File(args[0]));

      // Do each file
      for( String filename : info.getFiles() ) {
        //	System.out.println(filename);
        Vector<TLink> newlinks = info.computeTimeTimeLinks(filename);
        // Print each new link
        for( TLink link : newlinks ) {
          System.out.println(filename + " " + link.getId1() + " " + 
              link.getId2() + " " + link.getRelation());
        }
      }
    }
  }

}	
