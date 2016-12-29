package caevo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.w3c.dom.Element;

import caevo.Timex.Mod;
import caevo.Timex.Type;
import caevo.time.CorefParser;
import caevo.time.DateTimeData;
import caevo.time.DurationData;
import caevo.time.Interval;
import caevo.time.Interval.IntervalData;
import caevo.time.RangeData;
import caevo.time.RelationData;
import caevo.time.SUTimeParser;
import caevo.time.TimeData;
import caevo.time.TimeIdGen;
import caevo.time.TimeOperation;
import caevo.time.TimePointData;
import caevo.time.pattern.PatternResult;
import caevo.tlink.TLink;
import caevo.tlink.TimeTimeLink;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.POSTaggerAnnotator;
import edu.stanford.nlp.pipeline.TokenizerAnnotator;
import edu.stanford.nlp.pipeline.WordsToSentencesAnnotator;
import edu.stanford.nlp.time.SUTimeMain;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.time.TimeExpression;
import edu.stanford.nlp.util.CoreMap;


/**
 * This is just a wrapper around Stanford's SUTime tagger.
 * It includes some specific rules about fiscal quarters that fixes incorrect SUTime
 * performance on the finance genre.
 *
 * @author chambers
 */
public class TimexClassifier {
    String posTaggerData = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
//String _serializedGrammar = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
//  private String _nerPath = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";

  boolean debug = true;
  
  //AnnotationPipeline timexPipeline = null;
  
  private final SUTimeParser sutimeParser;
  private final CorefParser corefParser;


  public TimexClassifier() {
  	sutimeParser = new SUTimeParser(); 
  	corefParser = new CorefParser();
  }
  
    /**
     * Mash the given list of CoreLabel objects into a single space-delimited string.
     */
    private String buildStringFromCoreLabels(List<CoreLabel> sentence, int starti, int endi) {
        StringBuffer buf = new StringBuffer();
        for (int xx = starti; xx < endi; xx++) {
            if (xx > starti) buf.append(' ');
            buf.append(sentence.get(xx).getString(CoreAnnotations.OriginalTextAnnotation.class));
        }
        return buf.toString();
    }

    /**
     * Sometimes a phrase is marked as a TIMEX, but it was already labeled as an EVENT.
     * In these cases, we remove the TIMEX and assume the event is correct.
     * DESTRUCTIVE: alters the given list of timexes.
     *
     * @param timexes List of predicted timex instances.
     * @param sent    The sentence containing the timex predictions.
     */
    private void removeConflictingTimexesWithEvents(List<Timex> timexes, SieveSentence sent) {
        List<Timex> removals = new ArrayList<Timex>();
        for (Timex timex : timexes) {
            for (TextEvent event : sent.events()) {
                // Event token is already labeled and part of this Timex.
                if (event.getIndex() >= timex.getTokenOffset() && event.getIndex() <= timex.getTokenOffset() + timex.getTokenLength() - 1)
                    removals.add(timex);
            }
        }
        // Remove timexes that contained events...assume the event is correct and timex is incorrect.
        for (Timex remove : removals)
            timexes.remove(remove);
    }


//  "URL"
  
  /**
   * Use the given documents object to destructively mark it up for time expressions.
   */
  public void markupTimex3(SieveDocuments docs) {
    for( SieveDocument doc : docs.getDocuments() ) {
      if( debug ) System.out.println("doc = " + doc.getDocname());
      List<SieveSentence> sentences = doc.getSentences();
      List<Timex> dcts = doc.getDocstamp();
      if( dcts != null && dcts.size() > 1 ) {
        System.out.println("markupTimex3 dct size is " + dcts.size());
        System.exit(1);
      }
      Timex docDCT = (dcts != null && dcts.size() > 0) ? dcts.get(0) : null;
      String docDate = docDCT != null ? docDCT.getValue() : null;
      if( debug ) System.out.println("markupTimex3 docDate = " + docDate);
      //      System.out.println(sentences.size() + " sentences.");
      int tid = 1;

      // Loop over each sentence and get TLinks.
      int sid = 0;
      for (SieveSentence sent : sentences) {

      	//      	List<CoreLabel> theTokens = preprocessTokens(sent.tokens());

      	System.out.println("TimexClassifier markupTimex3 tokens = " + sent.tokens());
      	TimexMarkupResult timexMarkupResult = markupTimex3(sent.tokens(), tid, docDCT); 
      	List<Timex> timexList = timexMarkupResult.getTimexList();
      	List<Timex> innerTimexList = timexMarkupResult.getInnerTimexList();
      	List<TLink> tlinkList = timexMarkupResult.getTLinkList();

      	//myRevisedTimex3(stanfordTimex, docDate);
      	tid += timexList.size();

      	// Remove any TIMEX phrases that contain EVENT objects.
      	removeConflictingTimexesWithEvents(timexList, sent);

      	System.out.println("Full timex objects:");
      	for (Timex timex : timexList) {
      		System.out.println(timex + ": " + timex.getTokenOffset() + "-" + timex.getTokenLength());
      	}
      	
      	System.out.println("Inner timex objects:");
      	for (Timex timex : innerTimexList) {
      		System.out.println(timex + ": " + timex.getTokenOffset() + "-" + timex.getTokenLength());
      	}
      	
      	System.out.println("Links between timex objects:");
      	for (TLink tlink : tlinkList) {
      		System.out.println(tlink);
      	}
      	
      	//        System.out.println("GOT " + stanfordTimex.size() + " new timexes.");
      	doc.addTimexes(sid, timexList);
      	doc.addInnerTimexes(sid, innerTimexList);
      	doc.addTimeRefTlinks(tlinkList);
      	sid++;
      }
    }
  }

  
    private void myRevisedTimex3(List<Timex> timexes, String docDate) {
        if (docDate != null) {
            docDate = docDate.replaceAll("-", "");
            if (docDate.length() == 8) {
                int year = Integer.parseInt(docDate.substring(0, 4));
                int month = Integer.parseInt(docDate.substring(4, 6));

                for (Timex timex : timexes) {
                    String text = timex.getText().toLowerCase();

                    // 1.2 F1 improvement on Tempeval-3 training, value attribute.
//          if( text.equals("a year ago") || text.contains("a year earlier") || text.contains("last year") ) {
                    if (text.equals("a year ago") || text.equals("a year earlier")) {
                        int quarter = determineFiscalQuarter(year, month);
                        if (quarter > 0) {
                            String newvalue = (year - 1) + "-Q" + quarter;
                            timex.setValue(newvalue);
//              System.out.println("Changing timex " + timex.text() + " value: " + timex.value() + " to " + newvalue);
                        }
                    }

                    // 0.2 F1 improvement with the following two if statements.
                    // SUTime is overly specific on years. Strip off the month and day.
                    if (text.equals("last year"))
                        timex.setValue(timex.getValue().substring(0, 4));
                    if (text.endsWith("years ago"))
                        timex.setValue(timex.getValue().substring(0, 4));

                    // 0.4 F1 improvement. This fixed ~8 errors, and didn't add any errors of its own.
                    // SUTime sometimes does "PXM" when there is a clear quarter to choose.
                    if (text.equals("the latest quarter") && timex.getValue().equals("PXM")) {
                        int quarter = determineFiscalQuarter(year, month);
                        if (quarter > 0) {
                            String newvalue = year + "-Q" + quarter;
                            timex.setValue(newvalue);
//              System.out.println("Changing timex " + timex.text() + " value: " + timex.value() + " to " + newvalue);
                        }
                    }
                }
            }
        }
    }

    private int determineFiscalQuarter(int year, int month) {
        int current;
        if (month >= 10 && month <= 12)
            current = 1;
        else if (month >= 1 && month <= 3)
            current = 2;
        else if (month >= 4 && month <= 7)
            current = 3;
        else if (month >= 7 && month <= 9)
            current = 4;
        else return -1;

        // Subtract 2 quarters. News discusses two quarters ago when the reports come out.
        current = current - 2;
        if (current < 1) current = current + 4;
        return current;
    }

    private static class TimexMarkupResult {
    	private final List<Timex> timexList;
    	private final List<Timex> innerTimexList;
    	private final List<TLink> tlinkList;
    	
			public TimexMarkupResult(List<Timex> timexList, List<Timex> innerTimexList, List<TLink> tlinkList) {
				super();
				this.timexList = timexList;
				this.innerTimexList = innerTimexList;
				this.tlinkList = tlinkList;
			}
			
			public List<Timex> getTimexList() {
				return timexList;
			}

			public List<Timex> getInnerTimexList() {
				return innerTimexList;
			}

			public List<TLink> getTLinkList() {
				return tlinkList;
			}
    	
    }

    
    /**
     * Given a single sentence (represented as a pre-tokenized list of HasWord objects), use stanford's
     * SUTime to identify temporal entities and mark them up as TIMEX3 elements.
     * <p/>
     * This function should preserve the given words, and result in the same number of words, just returning
     * Timex objects based on the given word indices. Timex objects start 1 indexed: the first word is at
     * position 1, not 0.
     *
     * @param words     A single sentence's words.
     * @param idcounter A number to use for an ID of the first timex, and increment from there.
     * @param docDate   A string version of the document's creation time, e.g., "19980807"
     * @return A list of Timex objects with resolved time values, and a list of links indicating relations
     * between Timex objects.
     */
    public TimexMarkupResult markupTimex3(List<CoreLabel> words, int idcounter, Timex dct) {

    	String fullLine = buildStringFromCoreLabels(words, 0, words.size());
    	
    	List<CoreMap> labels = sutimeParser.parseSentence(fullLine, "");    	

    	// Create my Timex objects from Stanford's Timex objects.
    	List<Timex> newTimexList = new ArrayList<Timex>();
    	List<Timex> newInnerTimexList = new ArrayList<Timex>();
    	List<Timex> refTimexList = new ArrayList<Timex>();
    	List<TLink> newTLinkList = new ArrayList<TLink>(); 

    	//TimexIdGen id = new TimexIdGen();
    	
    	TimeIdGen timeIdGen = new TimeIdGen(1);

    	/*
    	System.out.println("SUTime output:");
    	for (CoreMap label : labels) {
    		edu.stanford.nlp.time.Timex stanfordTimex = label.get(TimeAnnotations.TimexAnnotation.class);
    		Element stanfordElement = stanfordTimex.toXmlElement();
    		System.out.println(label + "; " + stanfordElement);	
    	}
    	*/

    	refTimexList.add(dct);

    	int nextId = 1;
    	for (CoreMap label : labels) {
    		edu.stanford.nlp.time.Timex stanfordTimex = label.get(TimeAnnotations.TimexAnnotation.class);
    		Element stanfordElement = stanfordTimex.toXmlElement();
    		int startToken = label.get(CoreAnnotations.TokenBeginAnnotation.class)+1;
    		int endToken = label.get(CoreAnnotations.TokenEndAnnotation.class)+1;

    		Timex durationTimex = null;
    		Timex timePointTimex = null;

    		TimePointData timePointData = null;
    		DurationData durationData = null;
    		
    		// the main timex text
    		String text = stanfordElement.getTextContent();

    		int parsedTokens = 0;

    		// look for a duration pattern (e.g. '2 years' or 'at least 4 minutes')
    		PatternResult<DurationData> duration = corefParser.checkDurationPattern(text);
    		if (duration!=null) {
    			String durationText = duration.getText();
    			int durationTokens = durationText.split(" ").length;
    			System.out.println("Identified a duration: [" + durationText+"]");
    			text = text.substring(durationText.length());
    			parsedTokens += durationTokens;

    			// start processing the data
    			durationData = duration.getData();
    			
    			// create a timex for the duration that was found
    			durationTimex = new Timex();
    			durationTimex.setText(duration.getText());
    			durationTimex.setValue(durationData.toString());
    			durationTimex.setMod(durationData.getModifier());
    			durationTimex.setType(Type.DURATION);
    			durationTimex.setSpan(startToken, startToken+durationTokens);

    			// check if we have more text after the pattern
    			if (!text.isEmpty()) {

    				// check if we have a space
    				if (text.startsWith(" ")) {

    					// skip it and continue to parse the text
    					text = text.substring(1);

    				} else {
    					// this is unexpected 
    					System.out.println("Unexpected character after relation in: " + text);
    				}

    			}
    		}

    		PatternResult<RelationData> relation = null;
    		String relationText = null;
    		if (!text.isEmpty()) {

    			// look for a relation pattern (e.g. after/before/ago)
    			relation = corefParser.checkRelationPattern(text);
    			if (relation!=null) {
    				relationText = relation.getText();
    				parsedTokens += relationText.split(" ").length;
    				System.out.println("Identified a relation: [" + relationText+"]");
    				text = text.substring(relationText.length());

    				// check if we have more text
    				if (!text.isEmpty()) {

    					// check if we have a space
    					if (text.startsWith(" ")) {

    						// skip it and continue to parse the text
    						text = text.substring(1);

    					} else { 
    						// this is unexpected 
    						System.out.println("Unexpected character after relation in: " + text);
    					}

    				} else{
    					// we reached the end of the text, but if we deal with an 'ago' case then we
    					// want to reinterpret it as 'before today' or 'before now'. otherwise, we 
    					// are done.
    					if (relation.getData().isAgo()) {
    						System.out.println("We have an 'ago' timex-ending case");
    						if (duration!=null) {
    							relationText = "before";
    							if (duration.getData().getTimeUnit().isDateUnit()) {
    								System.out.println("Text is reinterpreted as 'before today'");
    								text = "today";
    							} else {
    								System.out.println("Text is reinterpreted as 'before now'");
    								text = "now";
    							}
    						}
    					}
    				}        				
    			}
    		}

    		if (!text.isEmpty()) {
    			// resolve the remaining text
    			timePointData  = corefParser.parseTimePoint(text,refTimexList);
    			if (timePointData!=null) {
    				timePointTimex = timePointData.getTimex(); 
    				timePointTimex.setSpan(startToken+parsedTokens, endToken);
    				System.out.println("Identified a timepoint: [" + timePointTimex + "]");
    			}
    		}
    		
    		if (timePointData==null && durationTimex!=null) {
    			// cases with a duration only '2 years', 'after 2 hours', 'for 3 days', 'at least 3 minutes before'
    			// these are durations that indicate the span of time between two events or the span of time that
    			// an event took. examples: <event> 2 years after <event>, or <event> for 3 days.
    			// we only store the timex and expect a sieve to use it for creating the right tlink(s).
 					addTimex(durationTimex,newTimexList,timeIdGen);
    		} else {
    			// cases with a time point
					DateTimeData dateTimeData = timePointData.getDateTimeData();
    			if (durationTimex==null) {
      			// but without a duration; so for example: yesterday, 2017-08, September 17, 'after 2010' 
  					addTimex(timePointTimex,newTimexList,timeIdGen);
  					addTimex(timePointTimex,refTimexList,timeIdGen);
    			} else if (durationTimex!=null && relation!=null && dateTimeData!=null) {
    				// cases with a duration and a relation '2 years after 2010' or 'at least 2 hours before 16:30'

       			// create a new timex for the calculated time
       			Timex newTimePointTimex = new Timex();
       			newTimePointTimex.setText(stanfordElement.getTextContent());
       			newTimePointTimex.setType(dateTimeData.getTimexType());
       			newTimePointTimex.setSpan(startToken, endToken);
    				
    				if (durationData.getModifier()==null) {
    					// for cases without a modifier, we can calculate the new time point. 
    					// for example, we calculate that '2 years after 2010' equals 2012 
     					RelationData relationData = relation.getData();
         			TimeOperation operation = relationData.getOperation();
         			DateTimeData newDateTimeData = dateTimeData.apply(operation,durationData);

         			// set the value of the new timex according to the calculated time
         			newTimePointTimex.setValue(newDateTimeData.toString());

         			// add the new timex
    					addTimex(newTimePointTimex,newTimexList,timeIdGen);
    				} else {
         			// the value of the new timex will be determined by a link
         			newTimePointTimex.setValue("SET-BY-LINK");
         			
         			// add the anchoring timex
    					addTimex(timePointTimex,newInnerTimexList,timeIdGen);

         			// add the duration
     					addTimex(durationTimex,newInnerTimexList,timeIdGen);

     					// add the new timex
    					addTimex(newTimePointTimex,newTimexList,timeIdGen);
     					
     					// create a link that defines the relation between the duration and the new timex
     					newTLinkList.add(new TimeTimeLink(newTimePointTimex.getTid(), timePointTimex.getTid(), 
     							relation.getData().getType(),durationTimex.getTid()));
    				}
  					addTimex(newTimePointTimex,refTimexList,timeIdGen);
    			}
    		}
    	}
    	
    	return new TimexMarkupResult(newTimexList,newInnerTimexList,newTLinkList);
    }

    public void addTimex(Timex timex, List<Timex> list, TimeIdGen id) {
    	if (timex.getTid()==null) {
    		timex.setTid(id.getNextTid());
    	}
    	list.add(timex);
    }
	
    
/*            

				for (CoreMap label : labels) {
            edu.stanford.nlp.time.Timex stanfordTimex = label.get(TimeAnnotations.TimexAnnotation.class);
            Element stanfordElement = stanfordTimex.toXmlElement();
            Timex newtimex = new Timex();
            String value = stanfordElement.getAttribute("value");
            String altValue = stanfordElement.getAttribute("altVal");
            if (!value.isEmpty()) {
            	newtimex.setValue(value);
            }
            else if (value.isEmpty() && !altValue.isEmpty()) {
            	String offsetStr = "OFFSET ";
            	int offsetLength = offsetStr.length();
            	if (altValue.startsWith(offsetStr)) {
            		altValue = altValue.substring(offsetLength);
            	}
            	newtimex.setValue(altValue);
            }
            newtimex.setType(Timex.Type.valueOf(stanfordElement.getAttribute("type")));
            newtimex.setText(stanfordElement.getTextContent());
            String docFnStr = stanfordElement.getAttribute("functionInDocument");
            if (docFnStr != null && !docFnStr.isEmpty()) {
                newtimex.setDocumentFunction(Timex.DocumentFunction.valueOf(docFnStr));
            }
            // Stanford Timex starts at index 0 in the sentence, not index 1.
            newtimex.setSpan(label.get(CoreAnnotations.TokenBeginAnnotation.class) + 1, label.get(CoreAnnotations.TokenEndAnnotation.class) + 1);
            if (!falseSecond(newtimex, words) && !falseDays(newtimex)) {
                // workaround for sutime nonsense
                newtimex.setTid("t" + idcounter++);
                if (debug) System.out.println("NEW SUTIME TIMEX: " + newtimex);
                System.out.println("NEW SUTIME " + newtimex.getTokenOffset() + "-" + newtimex.getTokenLength());
                newTimexList.add(newtimex);
            }
        }
        */


    /*
		private Timex createTimex(String text, int id, String docDate) {
      Annotation annotation = SUTimeMain.textToAnnotation(timexPipeline, text, docDate);
      
      List<CoreMap> newLabels = annotation.get(TimeAnnotations.TimexAnnotations.class);
      removeRangesAndNested(newLabels);
      CoreMap label = newLabels.get(0);
      edu.stanford.nlp.time.Timex stanfordTimex = label.get(TimeAnnotations.TimexAnnotation.class);
      Element stanfordElement = stanfordTimex.toXmlElement();

      Timex newtimex = new Timex();
      String value = stanfordElement.getAttribute("value");
      String altValue = stanfordElement.getAttribute("altVal");
      if (!value.isEmpty()) {
      	newtimex.setValue(value);
      }
      else if (value.isEmpty() && !altValue.isEmpty()) {
      	String offsetStr = "OFFSET ";
      	int offsetLength = offsetStr.length();
      	if (altValue.startsWith(offsetStr)) {
      		altValue = altValue.substring(offsetLength);
      	}
      	newtimex.setValue(altValue);
      }
      newtimex.setType(Timex.Type.valueOf(stanfordElement.getAttribute("type")));
      newtimex.setText(stanfordElement.getTextContent());
      String docFnStr = stanfordElement.getAttribute("functionInDocument");
      if (docFnStr != null && !docFnStr.isEmpty()) {
          newtimex.setDocumentFunction(Timex.DocumentFunction.valueOf(docFnStr));
      }

      // Stanford Timex starts at index 0 in the sentence, not index 1.
      newtimex.setSpan(label.get(CoreAnnotations.TokenBeginAnnotation.class) + 1, label.get(CoreAnnotations.TokenEndAnnotation.class) + 1);

//      if (!falseSecond(newtimex, words) && !falseDays(newtimex)) {
          // workaround for sutime nonsense
      newtimex.setTid("t" + id);
      if (debug) System.out.println("NEW SUTIME TIMEX: " + newtimex);
    //      newtimexes.add(newtimex);
  //    }
      
    
      return newtimex;
		}*/

    
    private void removeRangesAndNested(List<CoreMap> labels) {

      // look for elaborate ranges, break them up to their endpoints
      List<CoreMap> rejects = new LinkedList<CoreMap>();
      for (CoreMap label : labels) {
          if (!Timex.Type.DURATION.name().equals(label.get(TimeAnnotations.TimexAnnotation.class).timexType())) {
              continue;
          }
          List<? extends CoreMap> children = label.get(TimeExpression.ChildrenAnnotation.class);
          if (children.size() == 4 && children.get(0) instanceof CoreLabel && children.get(2) instanceof CoreLabel) {
              // fits the pattern of "from... to..." or "between... and..."
          		System.out.println("REMOVING SUTIME TIMEX: " + children);
              rejects.add(label);
          }
      }
      
    	// filter out nested timexes
      for (CoreMap label : labels) {
          List<? extends CoreMap> children = label.get(TimeExpression.ChildrenAnnotation.class);
          for (CoreMap child : children) {
          		if (labels.contains(child) && !rejects.contains(child)) {
                  rejects.add(child);

                  /*
                  edu.stanford.nlp.time.Timex stanfordTimex = child.get(TimeAnnotations.TimexAnnotation.class);
                  Element stanfordElement = stanfordTimex.toXmlElement();
                  Timex newtimex = new Timex();
                  newtimex.setType(Timex.Type.valueOf(stanfordElement.getAttribute("type")));
                  newtimex.setValue(stanfordElement.getAttribute("value"));
                  newtimex.setText(stanfordElement.getTextContent());
              		System.out.println("REMOVING SUTIME TIMEX: [" + child + "] from: " + labels + "(" + newtimex + ")");
              		*/
              }
          }
      }
      labels.removeAll(rejects);
    }

    /**
     * Adapted this from javanlp's SUTimeMain.java.
     * We could better integrate this with the parsing of the sentences, rather than starting from scratch again.
     * Performance gains would basically just avoid tokenizing and POS tagging.
     */
    private AnnotationPipeline getPipeline(boolean tokenize) {
        Properties props = new Properties();
        props.setProperty("sutime.includeRange", "true");
        props.setProperty("sutime.includeNested", "true");
        props.setProperty("sutime.markTimeRanges", "true");
        AnnotationPipeline pipeline = new AnnotationPipeline();
        if (tokenize) {
            pipeline.addAnnotator(new TokenizerAnnotator(false));
            pipeline.addAnnotator(new WordsToSentencesAnnotator(false));
        }
        pipeline.addAnnotator(new POSTaggerAnnotator(false));
        pipeline.addAnnotator(new TimeAnnotator("sutime", props));

        return pipeline;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private boolean falseDays(Timex newTimex) {
        if (newTimex.getType().equals(Timex.Type.DURATION) && newTimex.getValue().equals("P1D")) {
            if (newTimex.getText().contains("days")) {
                return true;
            }
        }
        return false;
    }

    private boolean falseSecond(Timex newTimex, List<CoreLabel> words) {
        if (newTimex.getType().equals(Timex.Type.DURATION) && newTimex.getValue().equals("PT1S")) {
            // false positive timex? "second" might be adjective
            int secondIx = 0;
            String[] timexWords = newTimex.getText().split(" ");
            for (int i = 0; i < timexWords.length; i++) {
                String timexWord = timexWords[i];
                if (timexWord.equalsIgnoreCase("second")) {
                    secondIx = i + newTimex.getTokenOffset(); // location of 'second' in the sentence (1-based)
                    break;
                }
            }
            if (secondIx == 0) return false;
            CoreLabel secondToken = words.get(secondIx - 1);
            String pos = secondToken.get(CoreAnnotations.PartOfSpeechAnnotation.class);
            return pos.equalsIgnoreCase("JJ");
        } else return false;
    }
}



// the 'ago' cases: we replace 'ago' by 'before' and add 'today' if 'ago' was at the end.
// basically we interpret 'ago' as 'before today' unless a different time reference time
// was given, like 'ago yesterday'. this means that we won't support cases with time like
// '2 minutes ago' because we always add 'today'. to support such cases we need to model
// 'now' and then add 'now' to get that '2 minutes ago' = '2 minutes before now'.

/*
Matcher matcher = agoPattern.matcher(text);
 if (matcher.find()) {
 	System.out.println("Handling an 'ago' case");
   boolean atEnd = matcher.end()==text.length();
   String newText = matcher.replaceAll("before");
   if (atEnd) {
   	newText = newText + " today";
   }
 	System.out.println("Updated text to: " + newText);
   Annotation newAnnotation = SUTimeMain.textToAnnotation(timexPipeline, newText, "");
   List<CoreMap> newLabels = newAnnotation.get(TimeAnnotations.TimexAnnotations.class);
   removeRangesAndNested(newLabels);
   label = newLabels.get(0);
   stanfordTimex = label.get(TimeAnnotations.TimexAnnotation.class);
   stanfordElement = stanfordTimex.toXmlElement();
   text = stanfordElement.getTextContent();
 }*/
/*

// extract the last child info
List<? extends CoreMap> children = label.get(TimeExpression.ChildrenAnnotation.class);
int childrenCount = children.size();

// check the number of sub-timexes  
if (childrenCount==0) {
	// the main timex is not a composite of sub-timexes, meaning that it is a self- 
	// contained time-point like a date, a time, a day, a month, etc. Note that it
	// can't be a duration like '2 years' because that would have been a composite 
	// timex, and anyway we would have recognized it already above and created an
	// instance for it.
	remainingText = text;
} else {
 // the main timex is a composite timex, so extract the remaining text and then
 // check the pattern
 StringBuilder remainingText = new StringBuilder();
 String[] textTokens = text.split(" ");
 for (int i=tokenOffset+parsedTokens+1, length=textTokens.length ; i<length ; i++) {
	 remainingText.append(textTokens);
	 remainingText.append(" ");
 }
 remainingText.deleteCharAt(remainingText.length()-1);
 remainingText = remainingText.toString();
}


 CoreMap lastChild = children.get(childrenCount-1);
 int lastChildTokenId = lastChild.get(CoreAnnotations.TokenBeginAnnotation.class) + 1;
 if (tokenOffset+parsedTokens+1==lastChildTokenId) {
   edu.stanford.nlp.time.Timex childStanfordTimex = lastChild.get(TimeAnnotations.TimexAnnotation.class);
   Element childStanfordElement = childStanfordTimex.toXmlElement();
   //remainingText = 
   String value = childStanfordElement.getAttribute("value");
   String altValue = childStanfordElement.getAttribute("altVal");
   System.out.println("Last child: " + lastChild + "; value="+value+"; altValue=" +altValue);
 }
 */


/*
int startPos = label.get(CoreAnnotations.TokenBeginAnnotation.class) + 1;
int endPos = 	label.get(CoreAnnotations.TokenEndAnnotation.class) + 1;
System.out.println("Main: [" + label + "] " + startPos + " - " + endPos);

List<String> childrenList = new ArrayList<String>();
for (CoreMap child : children) {
childrenList.add(child.toString());
startPos = child.get(CoreAnnotations.TokenBeginAnnotation.class) + 1;
endPos = 	child.get(CoreAnnotations.TokenEndAnnotation.class) + 1;
System.out.println("child: [" + child + "] " + startPos + " - " + endPos);
}
System.out.println("children list: " + childrenList);
}
*/

/*
// check if we have an anchor-able child like today/2010,12:32,etc.
int childrenCount = children.size();
for (int childId=0, childrenCount=children.size() ; childId<childrenCount ; childId++) {
CoreMap childLabel = children.get(childId);
edu.stanford.nlp.time.Timex childStanfordTimex = childLabel.get(TimeAnnotations.TimexAnnotation.class);
if (childStanfordTimex!=null) {
  Element childStanfordElement = childStanfordTimex.toXmlElement();
  Timex.Type type = Timex.Type.valueOf(childStanfordElement.getAttribute("type"));
	if (type==Type.DATE || type==Type.TIME) {
		Timex timex = createTimex(childStanfordElement.getTextContent(),nextId,docDate);
		nextId++;
	}
}
}*/
/*
	String childText = childStanfordElement.getAttribute("")
  Annotation newAnnotation = SUTimeMain.textToAnnotation(timexPipeline, newText, "");
  List<CoreMap> newLabels = newAnnotation.get(TimeAnnotations.TimexAnnotations.class);
  removeRangesAndNested(newLabels);
  label = newLabels.get(0);
  stanfordTimex = label.get(TimeAnnotations.TimexAnnotation.class);
  text = stanfordElement.getTextContent();
	
	
	// resolve value
	// create a new timex
}

// the string before the date/time
String prefix = "";

// a durative timex for the delta

// the new timex that stands for the global label

}
*/



/*      				
// do the math
Timex newTimePointTimex = corefParser.applyTemporalFunction(durationData,relation.getData(),timePointTimex);



if (duration.getData().getModifier()!=null) {
	// case with a modified duration - 'at least 2 years after 2010'
	
	// at or after 2010
	
	// the time that the whole timex refers to is not computable  
	Timex referantTimex = new Timex();
	referantTimex.setTid("t"+nextId++);
	referantTimex.setText(stanfordElement.getTextContent());
	referantTimex.setValue("");
	referantTimex.setType(Type.DATE);
	referantTimex.setSpan(startToken, endToken);

	refTimexList.add(referantTimex);
	newTimexList.add(referantTimex);
	newInnerTimexList.add(timePointTimex);
	TLink newLink = new TimeTimeLink(referantTimex.getTid(), timePointTimex.getTid(), relation.getData().getType());
		newLink.setIdM(durationTimex.getTid());
		newInnerTimexList.add(durationTimex);
	newTLinkList.add(newLink);
}
}


Timex referantTimex = new Timex();
referantTimex.setTid("t"+nextId++);
referantTimex.setText(stanfordElement.getTextContent());
referantTimex.setValue("");
referantTimex.setType(Type.DATE);
referantTimex.setSpan(startToken, endToken);

refTimexList.add(referantTimex);
newTimexList.add(referantTimex);
newInnerTimexList.add(timePointTimex);

// create a link either with or without a magnitude
TLink newLink = new TimeTimeLink(referantTimex.getTid(), timePointTimex.getTid(), relation.getData().getType());
if (durationTimex!=null) {
newLink.setIdM(durationTimex.getTid());
newInnerTimexList.add(durationTimex);
}
newTLinkList.add(newLink);
}


}
}*/

/*
// calculate the new time point (i.e. find that '2 years after 2010' means 2012) 
	RelationData relationData = relation.getData();
	TimeOperation operation = relationData.getOperation();
	DateTimeData newDateTimeData = dateTimeData.apply(operation,durationData);
	Timex.Type type = newDateTimeData.getTimexType();
	int modTokens = durationData.getModText()!=null ? durationData.getModText().split(" ").length : 0;

	// create a new timex for the calculated time
	Timex newTimePointTimex = new Timex();
	newTimePointTimex.setText(durationData.toText(false) + " " + relationData.toString() + " " + timePointData.toText());
	newTimePointTimex.setValue(newDateTimeData.toString());
	newTimePointTimex.setType(type);
	newTimePointTimex.setSpan(startToken+modTokens, endToken);

	if (durationData.getModifier()==null) {
		// the new time point timex is all we have
		addTimex(newTimePointTimex,newTimexList,timeIdGen);
		addTimex(newTimePointTimex,refTimexList,timeIdGen);
	}
	else {
		addTimex(newTimePointTimex,newInnerTimexList,timeIdGen);

		String newEndPointTid = newTimePointTimex.getTid();
		String oldEndPointTid = timePointTimex.getTid();

		Interval.EndPoint.Type newEndPointType = null, oldEndPointType = null;
		Mod mod = durationData.getModifier();
		if (mod==Mod.EQUAL_OR_LESS) {
			newEndPointType = Interval.EndPoint.Type.CLOSED;
			oldEndPointType = relation.getData().getType()==TLink.Type.AFTER || relation.getData().getType()==TLink.Type.BEFORE
										 	? Interval.EndPoint.Type.OPEN : Interval.EndPoint.Type.CLOSED;
		} else if (mod==Mod.LESS_THAN) {
			newEndPointType = Interval.EndPoint.Type.OPEN;
			oldEndPointType = relation.getData().getType()==TLink.Type.AFTER || relation.getData().getType()==TLink.Type.BEFORE
										 	? Interval.EndPoint.Type.OPEN : Interval.EndPoint.Type.CLOSED;
		} else if (mod==Mod.EQUAL_OR_MORE) {
			newEndPointType = Interval.EndPoint.Type.CLOSED;
			oldEndPointType = Interval.EndPoint.Type.INF;
		} else if (mod==Mod.MORE_THAN) {
			newEndPointType = Interval.EndPoint.Type.OPEN;
			oldEndPointType = Interval.EndPoint.Type.INF;
		}       					

		Interval.EndPoint lowerEndPoint, higherEndPoint;
		if (operation==TimeOperation.PLUS) {
			lowerEndPoint = new Interval.EndPoint(newEndPointTid, newEndPointType);
			higherEndPoint = new Interval.EndPoint(oldEndPointTid, oldEndPointType);
		} else {
			lowerEndPoint = new Interval.EndPoint(oldEndPointTid, oldEndPointType);
			higherEndPoint = new Interval.EndPoint(newEndPointTid, newEndPointType);
		}
		
		Interval interval = new Interval(lowerEndPoint,higherEndPoint);
		
		IntervalData intervalData = interval.createData(timeIdGen);
		       		
		List<Timex> intervalInnerTimex = intervalData.getInnerTimexes();
		for (Timex innerTimex : intervalInnerTimex) {
			addTimex(innerTimex,newInnerTimexList,timeIdGen);
		}
		
		Timex mainTimex = intervalData.getMainTimex();
		mainTimex.setText(stanfordElement.getTextContent());
		mainTimex.setValue("");
		mainTimex.setType(type);
		mainTimex.setSpan(startToken, endToken);
		addTimex(mainTimex,newTimexList,timeIdGen);
		
		newTLinkList.addAll(intervalData.getLinks());

		/*
		// create a new timex that stands for the whole text we are analyzing
		Timex rangeTimex = new Timex();
		rangeTimex.setText(stanfordElement.getTextContent());
		rangeTimex.setValue("");
		rangeTimex.setType(type);
		rangeTimex.setSpan(startToken, endToken);

		// add it to the list
		addTimex(rangeTimex,newTimexList,id);
		addTimex(rangeTimex,refTimexList,id);

		Mod mod = durationData.getModifier();
		if (mod==Mod.EQUAL_OR_LESS) {


		} else if (mod==Mod.EQUAL_OR_MORE) {

			TLink.Type linkType = operation==TimeOperation.PLUS ? TLink.Type.BEFORE_OR_OVERLAP: TLink.Type.BEFORE;
			newTLinkList.add(new TimeTimeLink(rangeTimex.getTid(), newTimePointTimex.getTid(), linkType));

		} else if (mod==Mod.LESS_THAN) {
			// add the starting timexes to the inner list
			addTimex(timePointTimex,newInnerTimexList,id);
			TLink.Type type1, type2;
			if (operation==TimeOperation.PLUS) {
				type1 = TLink.Type.AFTER;
				type2 = TLink.Type.BEFORE;
			} else {
				type1 = TLink.Type.BEFORE;
				type2 = TLink.Type.AFTER;
			}
			newTLinkList.add(new TimeTimeLink(rangeTimex.getTid(), timePointTimex.getTid(), type1));
			newTLinkList.add(new TimeTimeLink(rangeTimex.getTid(), newTimePointTimex.getTid(), type2));
		} else if (mod==Mod.MORE_THAN) {
			TLink.Type linkType = operation==TimeOperation.PLUS ? TLink.Type.AFTER : TLink.Type.BEFORE;
			newTLinkList.add(new TimeTimeLink(rangeTimex.getTid(), newTimePointTimex.getTid(), linkType));
		}*/

