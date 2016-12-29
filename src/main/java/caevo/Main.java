package caevo;

import caevo.sieves.Sieve;
import caevo.tlink.TLink;
import caevo.tlink.TimeTimeLink;
import caevo.tlink.TLink.Type;
import caevo.util.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Controls all Sieve processing including TLink annotating, closure, and the core programming.
 *
 * REQUIREMENTS:
 * - An environment variable JWNL that points to your jwnl_file_properties.xml file.
 *
 * HOW TO RUN:
 * java Main -info <filepath> [-set all|train|dev] gauntlet
 * - Tests the sieves independently and calculates individual precision.
 *
 * java Main -info <filepath> parsed
 * - Run event, time, and TLink extraction. The given infofile contains parses.
 *
 * java Main -info <filepath> [-set all|train|dev]
 * - Runs only the tlink sieve pipeline. Assumes the given infofile has events and times.
 *
 * java Main -info <filepath> [-set all|train|dev] trainall
 * - Runs the train() function on all of the sieves.
 *
 * java Main <file-or-dir> raw
 * - Takes a text file and runs the NLP pipeline, then our event/timex/tlink extraction.
 *
 * @author chambers
 */
public class Main {
    public static final String serializedGrammar = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
    
	private TextEventClassifier eventClassifier;
	private TimexClassifier timexClassifier;
	public static WordNet wordnet;
	
	SieveDocuments thedocs;
	SieveDocuments thedocsUnchanged; // for evaluating if TLinks are in the input
	Closure closure;
	String outpath = "sieve-output.xml";
	boolean debug = true;
	boolean useClosure = true;
	boolean force24hrDCT = true;
	String dctHeuristic = "none";
	String infopath = null;
	String sieveList;
	boolean skipVague = false;

	// parser 
	LexicalizedParser parser;
	TreebankLanguagePack tlp;
	GrammaticalStructureFactory gsf;
	
	// Which dataset do we load?
  public static enum DatasetType { TRAIN, DEV, TEST, ALL };
  DatasetType dataset = DatasetType.TRAIN;
	
	// List the sieve class names in your desired order.
	private String[] sieveClasses;
    
	
	/**
	 * Constructor: give it the command-line arguments.
	 */
	public Main(String[] args) {
		Properties cmdlineProps = StringUtils.argsToProperties(args);

		initProperties();
		
		// -info on the command line?
		if( cmdlineProps.containsKey("info") )
			infopath = cmdlineProps.getProperty("info");
        
		if( infopath != null ) {
			System.out.println("Checking for infofile at " + infopath);
			thedocs = new SieveDocuments(infopath);
			thedocsUnchanged = new SieveDocuments(infopath);
		}

		// -set on the command line?
		if( cmdlineProps.containsKey("set") ) {
			String type = cmdlineProps.getProperty("set");
			dataset = DatasetType.valueOf(type.toUpperCase());
		}
		
		init();
	}
	
	/**
	 * Empty Constructor.
	 */
	public Main() {
		initProperties();
		init();
	}
	
	public String getDefaultFixedDct() {
		try {
			return dctHeuristic.equals("setFixedDateAsDCT") ? CaevoProperties.getString("Main.dct") : "";
		} catch (IOException e) {
			throw new RuntimeException("Failed to get default dct: " + e.getMessage());
		}
	}
	
	private void initProperties() {
		// Read the properties from disk at the location specified by -Dprops=XXXXX
		try {
			CaevoProperties.load();
			// Look for a given pre-processed InfoFile
			infopath = CaevoProperties.getString("Main.info", null);
			// Overwrite these globals if they are in the properties file.
			debug = CaevoProperties.getBoolean("Main.debug", debug);
			useClosure = CaevoProperties.getBoolean("Main.closure", useClosure);
			dataset = DatasetType.valueOf(CaevoProperties.getString("Main.dataset", dataset.toString()).toUpperCase());
			force24hrDCT = CaevoProperties.getBoolean("Main.force24hrdct", force24hrDCT);
			dctHeuristic = CaevoProperties.getString("Main.dctHeuristic", dctHeuristic);
			sieveList = CaevoProperties.getString("Main.sieves","");
			skipVague = CaevoProperties.getBoolean("Main.skipVague",skipVague);
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	private void init() {
		
		// Initialize the transitive closure code.
		try {
			closure = new Closure();
		} catch( IOException ex ) {
			System.out.println("ERROR: couldn't load Closure utility.");
			ex.printStackTrace();
			System.exit(1);
		}
		
		// Load WordNet for any and all sieves.
		wordnet = new WordNet();
		
		// Load the sieve list.
		sieveClasses = loadSieveList();
		
		// Initialize the parser.
		parser = Ling.createParser(serializedGrammar);
		if( parser == null ) {
			System.out.println("Failed to create parser from " + serializedGrammar);
			System.exit(1);
		}
		tlp = new PennTreebankLanguagePack();
		gsf = tlp.grammaticalStructureFactory();
		
		timexClassifier = new TimexClassifier();
		
		eventClassifier = new TextEventClassifier(wordnet);
		eventClassifier.loadClassifiers();
		
		System.out.println("Dataset:\t" + dataset);
		System.out.println("Using Closure:\t" + useClosure);
		System.out.println("Debug:\t\t" + debug);
	}

	private String[] loadSieveList() {
		String[] sieveArray;
		if (!sieveList.isEmpty()) {
			sieveArray = sieveList.split(",");
		} else {
			String filename = System.getProperty("sieves");
			if( filename == null ) filename = "default.sieves";
	
			System.out.println("Reading sieve list from: " + filename);
	
			List<String> sieveNames = new ArrayList<String>();
			try {
				BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
				String line;
				while( (line = reader.readLine()) != null ) {
					if( !line.matches("^\\s*$") && !line.matches("^\\s*//.*$") ) {
						// Remove trailing comments if they exist.
						if( line.indexOf("//") > -1 )
							line = line.substring(0, line.indexOf("//"));
						String name = line.trim();
						sieveNames.add(name);
					}
				}
				reader.close();
			} catch( Exception ex ) {
				System.out.println("ERROR: no sieve list found");
				ex.printStackTrace();
				System.exit(1);
			}
			String[] arr = new String[sieveNames.size()];
			sieveArray = sieveNames.toArray(arr);
		}
		return sieveArray;
	}

	/**
	 * Turns a string class name into an actual Sieve Instance of the class.
	 * @param sieveClass
	 * @return
	 */
	private Sieve createSieveInstance(String sieveClass) {
		try {
			Class<?> c = Class.forName("caevo.sieves." + sieveClass);
			Sieve sieve = (Sieve)c.newInstance();
			return sieve;
		} catch (InstantiationException e) {
			System.out.println("ERROR: couldn't load sieve: " + sieveClass);
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.out.println("ERROR: couldn't load sieve: " + sieveClass);
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.out.println("ERROR: couldn't load sieve: " + sieveClass);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("ERROR: couldn't load sieve: " + sieveClass);
			e.printStackTrace();
		}
		return null;
	}
	
	private Sieve[] createAllSieves(String[] stringClasses) {
		Sieve sieves[] = new Sieve[stringClasses.length];
		for( int xx = 0; xx < stringClasses.length; xx++ ) {
			sieves[xx] = createSieveInstance(stringClasses[xx]);
			System.out.println("Added sieve: " + stringClasses[xx]);
		}
		return sieves;
	}
	
	/**
	 * Assumes the global SieveDocuments is initialized and loaded.
	 * Resolve co-referring timexes on the given documents .
	 */
	private void markupTimexesDependencies(SieveDocuments thedocs) {
		SieveDocuments docs = getDataset(dataset, thedocs);
		for( SieveDocument doc : thedocs.getDocuments() ) {
			doc.markupTimexDependencies();
		}
	}
	
	/**
	 * Assumes the global SieveDocuments is initialized and loaded.
	 * Run all sieves!! On all documents!!
	 */
	public void runSieves() {
		runSieves(thedocs);
	}

	/**
	 * Run the sieve pipeline on the given documents.
	 */
	public void runSieves(SieveDocuments thedocs) {
		// Remove all TLinks because we will add our own.
		thedocs.removeAllTLinks();

		// Start with zero links.
		List<TLink> currentTLinks = new ArrayList<TLink>();
		LinkTable currentTLinksHash = new LinkTable();
        
		// Create all the sieves first.
		Sieve sieves[] = createAllSieves(sieveClasses);

		// Statistics collection.
		SieveStats stats[] = new SieveStats[sieveClasses.length+1];
		Map<String, SieveStats> sieveNameToStats = new HashMap<String, SieveStats>();
		for( int i = 0; i < sieveClasses.length; i++ ) {
			stats[i+1] = new SieveStats(sieveClasses[i]);
			sieveNameToStats.put(sieveClasses[i], stats[i+1]);
		}
		stats[0] = new SieveStats("TimeReference");
		sieveNameToStats.put("TimeReference", stats[0]);
		
		// Data
		SieveDocuments docs = getDataset(dataset, thedocs);
        
		// Do each file independently.
		for( SieveDocument doc : docs.getDocuments() ) {
			System.out.println("Processing " + doc.getDocname() + "...");
//			System.out.println("Number of gold links: " + thedocsUnchanged.getDocument(doc.getDocname()).getTlinks().size());
			
			// Add the time-time links to our current list.
			System.out.println("\tTime References");			
			if( debug ) System.out.println("\t\t" + doc.getTimeRefTlinks().size() + " new links.");
			addProposedToCurrentList("TimeReference", doc.getTimeRefTlinks(), currentTLinks, currentTLinksHash);

			// Run Closure
			if( useClosure ) {
				List<TLink> closedLinks = closureExpand("TimeReference", currentTLinks, currentTLinksHash);
				if( debug ) System.out.println("\t\tClosure produced " + closedLinks.size() + " links.");
				stats[0].addClosureCount(closedLinks.size());
			}
			
			// Loop over the sieves in order.
			for( int xx = 0; xx < sieves.length; xx++ ) {
				Sieve sieve = sieves[xx];
				if( sieve == null ) continue;
				System.out.println("\tSieve " + sieve.getClass().toString());
                
				// Run this sieve
				List<TLink> newLinks = sieve.annotate(doc, currentTLinks);
				
				// skip vague links if requested to
				if (skipVague) {
					Iterator<TLink> it = newLinks.iterator();
					int skippedCount = 0;
					while (it.hasNext()) {
					   TLink tlink = it.next();
					   if (tlink.getRelation()==Type.VAGUE) {
					  	 it.remove();
					  	 skippedCount++;
					   }
					}
					if( debug ) System.out.println("\t\tskipped " + skippedCount + " new VAGUE links.");
				}
				
				if( debug ) System.out.println("\t\t" + newLinks.size() + " new links.");
//				if( debug ) System.out.println("\t\t" + newLinks);
				stats[xx+1].addProposedCount(newLinks.size());
				
				// Verify the links as non-conflicting.
				int numRemoved = removeConflicts(currentTLinksHash, newLinks);
				if( debug ) System.out.println("\t\tRemoved " + numRemoved + " proposed links.");
//				if( debug ) System.out.println("\t\t" + newLinks);
				stats[xx+1].addRemovedCount(numRemoved);
				
				if( newLinks.size() > 0 ) {
					// Add the good links to our current list.
					addProposedToCurrentList(sieveClasses[xx], newLinks, currentTLinks, currentTLinksHash);//currentTLinks.addAll(newLinks);

					// Run Closure
					if( useClosure ) {
						List<TLink> closedLinks = closureExpand(sieveClasses[xx], currentTLinks, currentTLinksHash);
						if( debug ) System.out.println("\t\tClosure produced " + closedLinks.size() + " links.");
						//					if( debug ) System.out.println("\t\tclosed=" + closedLinks);
						stats[xx+1].addClosureCount(closedLinks.size());
					}
				}
				if( debug ) System.out.println("\t\tDoc now has " + currentTLinks.size() + " links.");
			}
			
			// Add links to InfoFile.
			doc.addTlinks(currentTLinks);
//			if( debug ) System.out.println("Adding links: " + currentTLinks);
			currentTLinks.clear();
			currentTLinksHash.clear();
		}
		
		try {
			if (CaevoProperties.getBoolean("Main.debug")) {
				System.out.println("Writing output: " + outpath);
				docs.writeToXML(new File(outpath));
			}
		} catch (IOException e) {}
		
		// Evaluate it if the input file had tlinks in it.
		if( thedocsUnchanged != null )
			Evaluate.evaluate(thedocsUnchanged, docs, sieveClasses, sieveNameToStats);
	}
    
	/**
	 * Test each sieve's precision independently.
	 * Runs each sieve and evaluates its proposed links against the input -info file.
	 * You must have loaded an -info file that has gold TLinks in it.
	 */
	public void runPrecisionGauntlet() {
		if( thedocs == null ) {
			System.out.println("ERROR: no info file given as input for the precision gauntlet.");
			System.exit(1);
		}
		thedocs.removeAllTLinks(); // because we will be adding our own
		
		// Create all the sieves first.
		Sieve sieves[] = createAllSieves(sieveClasses);
		
		// Data
		SieveDocuments docs = getDataset(dataset, thedocs);
		
		// Empty TLink list and counts.
		List<TLink> currentTLinks = new ArrayList<TLink>();
		Counter<String> numCorrect = new ClassicCounter<String>();
		Counter<String> numIncorrect = new ClassicCounter<String>();
		Counter<String> numIncorrectNonVague = new ClassicCounter<String>();
		Map<String,Counter<TLink.Type>> goldLabelCounts = new HashMap<String,Counter<TLink.Type>>();
		for( String sc : sieveClasses ) goldLabelCounts.put(sc, new ClassicCounter<TLink.Type>());
		Map<String,Counter<String>> guessCounts = new HashMap<String,Counter<String>>();
		for( String sc : sieveClasses ) guessCounts.put(sc, new ClassicCounter<String>());
    int totalGoldLinks = 0;
		
		// Loop over documents.
		for( SieveDocument doc : docs.getDocuments() ) {
//			Set<String> seenGolds = new HashSet<String>();
			
			System.out.println("doc: " + doc.getDocname());
			List<SieveSentence> sents = doc.getSentences();
			// Gold links.
			List<TLink> goldLinks = thedocsUnchanged.getDocument(doc.getDocname()).getTlinksNoClosures();
			Map<String, TLink> goldOrderedIdPairs = new HashMap<String, TLink>();
			for (TLink tlink : goldLinks) {
//				System.out.println("adding gold: " + tlink + " order=" + TLink.orderedIdPair(tlink));
				goldOrderedIdPairs.put(TLink.orderedIdPair(tlink), tlink);
			}
			totalGoldLinks += goldOrderedIdPairs.size();
      if( debug ) System.out.println("Gold links num=" + goldLinks.size() + " or num=" + goldOrderedIdPairs.size());      
			
			// Loop over sieves.
			for( int xx = 0; xx < sieveClasses.length; xx++ ) {
				String sieveName = sieveClasses[xx];
				Sieve sieve = sieves[xx];
				if( sieve != null ) {
                    
					// Run it.
					List<TLink> proposed = sieve.annotate(doc, currentTLinks);
					if( debug ) System.out.println(sieveName + " proposed " + proposed.size() + ": " + proposed);
					removeDuplicatesAndInvalids(proposed);
					
					// Check proposed links.
					if( proposed != null ) {
						for( TLink pp : proposed ) {
							String orderedIdPair = TLink.orderedIdPair(pp);
//							System.out.println("looking up gold for " + pp + " order=" + TLink.orderedIdPair(pp));
							TLink goldLink = goldOrderedIdPairs.get(orderedIdPair);

							if( goldLink != null ) {
								guessCounts.get(sieveName).incrementCount(goldLink.getOrderedRelation()+" "+pp.getOrderedRelation());
								goldLabelCounts.get(sieveName).incrementCount(goldLink.getRelation());
								
//								seenGolds.add(goldLink.getId1() + goldLink.getId2());
//								seenGolds.add(goldLink.getId2() + goldLink.getId1());
							}

							// Guessed link is correct!
							if( Evaluate.isLinkCorrect(pp, goldLinks) ) {
								numCorrect.incrementCount(sieveClasses[xx]);
							} 
							// Gold and guessed link disagree!
							// Only mark relations wrong if there's a conflicting human annotation.
							// (if there's no human annotation, we don't know if it's right or wrong)
							else if (goldLink != null) {
								if (!goldLink.getRelation().equals(TLink.Type.VAGUE)) {
									numIncorrectNonVague.incrementCount(sieveClasses[xx]);
								}
								numIncorrect.incrementCount(sieveClasses[xx]);
								if (debug) {
									System.out.printf("%s: %s: Incorrect Link: expected %s, found %s\nDebug info: %s\n",
											sieveClasses[xx], doc.getDocname(), goldOrderedIdPairs.get(orderedIdPair), pp,
											getLinkDebugInfo(pp, sents, doc));
								}
							}
							// No gold link.
							else {
								System.out.println("No gold link (" + sieveName + "): " + pp);
							}
						}
					}
				}
			}
			
			
			// Check for missed golds.
//			for (TLink tlink : goldLinks) {
//				if( !seenGolds.contains(tlink.getId1() + tlink.getId2()) ) {
//					System.out.println("ERROR: gold link not proposed: " + tlink);
//				}
//			}
		}

		
		// Calculate precision and output the sorted sieves.
		Counter<String> precision = new ClassicCounter<String>();
		Counter<String> precisionNonVague = new ClassicCounter<String>();
		for (String sieveClass : sieveClasses) {
			double correct = numCorrect.getCount(sieveClass);
			double incorrect = numIncorrect.getCount(sieveClass);
			double incorrectNonVague = numIncorrectNonVague.getCount(sieveClass);
			double total = correct + incorrect;
			precision.incrementCount(sieveClass, total > 0 ? correct / total : 0.0);
			double totalNonVague = correct + incorrectNonVague;
			precisionNonVague.incrementCount(sieveClass, totalNonVague > 0 ? correct / totalNonVague : 0.0);
		}
		List<String> sortedKeys = Util.sortCounterKeys(precision);
		for( String key : sortedKeys ) {
			double correct = numCorrect.getCount(key);
			int numtabs = Math.max(1, 4 - key.length() / 8);
			System.out.print(key);
			for( int tt = 0; tt < numtabs; tt++ ) System.out.print("\t");
			System.out.printf("p=%.2f\t%.0f of %.0f\tr=%.2f from %d\tNon-VAGUE:\tp=%.2f\t%.0f of %.0f\n",
                              precision.getCount(key), correct, correct + numIncorrect.getCount(key),
                              (correct / totalGoldLinks), (int)totalGoldLinks,
                              precisionNonVague.getCount(key), correct, correct + numIncorrectNonVague.getCount(key));
		}
		for( String key : sortedKeys ) {
			System.out.println("**** " + key + "****");
			Evaluate.printBaseline(goldLabelCounts.get(key));			
			Evaluate.printConfusionMatrix(guessCounts.get(key));
		}
	}

	
	
	/**
	 * Calls the train() function on all of the listed sieves.
	 */
	public void trainSieves() {
		if( thedocs == null ) {
			System.out.println("ERROR: no info file given as input for the precision gauntlet.");
			System.exit(1);
		}
        
		// Create all the sieves first.
		Sieve sieves[] = createAllSieves(sieveClasses);
		
		// Data
		SieveDocuments docs = getDataset(dataset, thedocs);
        
		// Train them!
		for( Sieve sieve : sieves ) {
			if( debug ) System.out.println("Training sieve: " + sieve.getClass().toString());
			sieve.train(docs);
		}
	}
	
	private String getLinkDebugInfo(TLink link, List<SieveSentence> sents, SieveDocument doc) {
		StringBuilder builder = new StringBuilder();
		
		if (link instanceof TimeTimeLink) {
			TimeTimeLink ttLink = (TimeTimeLink)link;
			Timex t1 = doc.getTimexByTid(ttLink.getId1());
			Timex t2 = doc.getTimexByTid(ttLink.getId2());
			
			builder.append("Time-Time " + ttLink.getRelation() + "\t");
			builder.append(t1.getTid() + ": " + t1.getValue() + " (" + t1.getText() + ")\t");
			builder.append(t2.getTid() + ": " + t2.getValue() + " (" + t2.getText() + ")");
		} else {
			// complex code because Timex and TextEvent don't share a common parent or common APIs
			String id1 = link.getId1();
			Timex t1 = doc.getTimexByTid(id1);
			TextEvent e1 = doc.getEventByEiid(id1);
			String normId1 = t1 != null ? t1.getTid() : e1.getId();
			String text1 = t1 != null ? t1.getText() : e1.getString();
			int sid1 = t1 != null ? t1.getSid() : e1.getSid();
			String id2 = link.getId2();
			Timex t2 = doc.getTimexByTid(id2);
			TextEvent e2 = doc.getEventByEiid(id2);
			String normId2 = t2 != null ? t2.getTid() : e2.getId();
			String text2 = t2 != null ? t2.getText() : e2.getString();
			int sid2 = t2 != null ? t2.getSid() : e2.getSid();
			builder.append(String.format("%s(%s[%s],%s[%s])", link.getRelation(),
                                         text1, normId1, text2, normId2));
			if (e1 != null && e2 != null) {
				TextEvent.Tense e1Tense = e1.getTense();
				TextEvent.Aspect e1Aspect = e1.getAspect();
				TextEvent.Class e1Class = e1.getTheClass();
				TextEvent.Tense e2Tense = e2.getTense();
				TextEvent.Aspect e2Aspect = e2.getAspect();
				TextEvent.Class e2Class = e2.getTheClass();
                // simple display of relation, anchor texts, and anchor ids.
                builder.append(String.format("\n%s[%s-%s-%s], %s[%s-%s-%s]",
                                             normId1, e1Tense, e1Aspect, e1Class, normId2, e2Tense, e2Aspect, e2Class));
			}
			builder.append(String.format("\n%s\n%s", sents.get(sid1).sentence(), sents.get(sid2).sentence()));
		}
		
		return builder.toString();
	}
	
	private static class LinkId {
		private final List<String> names;
		
		private LinkId(List<String> names) {
			this.names = names;
		}

		private static final List<List<Integer>> orders;
		
		static {
			orders = new ArrayList<List<Integer>>();
			List<Integer> ids = Arrays.asList(0,1,2);
			for (int i=0 ; i<3 ; i++) {
				for (int j=0 ; j<3 ; j++) {
					if (i!=j) {
						List<Integer> localIds = new ArrayList<Integer>(ids); 
						localIds.removeAll(Arrays.asList(i,j));
						orders.add(Arrays.asList(i,j,localIds.get(0)));
						orders.add(Arrays.asList(i,j));
					}
				}
			}
		}
		
		public static LinkId of(TLink link) {
			List<String> values = Arrays.asList(link.getId1(),link.getId2(),link.getIdM()!=null ? link.getIdM() : "");

			Set<String> names = new HashSet<String>();
			for (int i=0, ordersCount=orders.size() ; i<ordersCount ; i++) {
				List<Integer> order = orders.get(i);
				boolean missedItem = false;
				StringBuilder name = new StringBuilder();
				for (int j=0, itemsCount=order.size() ; j<itemsCount && !missedItem; j++) {
					String value = values.get(order.get(j));
					if (!value.isEmpty()) {
						name.append(value);
					} else {
						missedItem = true;
					}
				}
				if (!missedItem) {
					names.add(name.toString());
				}
			}
			
			return new LinkId(new ArrayList<String>(names));
		}
		
		public List<String> getNames() {
			return names;
		}
		
		public String toString() {
			return names.toString();
		}
		
		public boolean covers(LinkId otherId) {
			return names.containsAll(otherId.names);
		}
		
	}
	
	private static class LinkTable {
		private final Map<String,TLink> links;
		
		public LinkTable() {
			links = new HashMap<String, TLink>();
		}
		
		public boolean containsLink(LinkId id) {
			List<String> names = id.getNames();
			boolean result = true;
			for (int i=0, size=names.size() ; i<size && result ; i++) {
				result = result && links.containsKey(names.get(i));
			}
			return result;
		}
		
		public void put(LinkId id, TLink link) {
			List<String> names = id.getNames();
			for (int i=0, size=names.size() ; i<size ; i++) {
				links.put(names.get(i),link);	
			}
		}
		
		public TLink get(LinkId id) {
			return links.get(id.getNames().get(0));
		}
		
		public void clear() {
			links.clear();
		}
		
		public String toString() {
			return links.toString();
		}
		
	}
    
	private void addProposedToCurrentList(String sieveName, List<TLink> proposed, List<TLink> current, LinkTable currentHash) {
		Iterator<TLink> iter = proposed.iterator();
		while (iter.hasNext()) {
			TLink newlink = iter.next();
			LinkId id = LinkId.of(newlink);
			if( currentHash.containsLink(id)) {
				System.out.println("MAIN WARNING: overwriting " + currentHash.get(id) + " with " + newlink);
				current.remove(newlink);
			}
			current.add(newlink);
			currentHash.put(id, newlink);
			newlink.setOrigin(sieveName);
		}
	}

	/**
 	 * DESTRUCTIVE FUNCTION (proposedLinks will be modified)
	 * Remove a link from the given list if another link already exists in the list
	 * and covers the same event or time pair.
	 * @param proposedLinks A list of TLinks to check for duplicates.
	 * @return The number of duplicates found.
	 */
	private int removeDuplicatesAndInvalids(List<TLink> proposedLinks) {
		if( proposedLinks == null || proposedLinks.size() < 2 ) 
			return 0;
		
		List<TLink> removals = new ArrayList<TLink>();
		Set<String> seenNew = new HashSet<String>();
		
		for( TLink proposed : proposedLinks ) {
			// Make sure we have a valid link with 2 events!
			if( proposed.getId1() == null || proposed.getId2() == null || 
					proposed.getId1().length() == 0 || proposed.getId2().length() == 0 ) {
				removals.add(proposed);
				System.out.println("WARNING (proposed an invalid link): " + proposed);
			}			
			// Remove any proposed links that are duplicates of already proposed links.
			else if( seenNew.contains(proposed.getId1()+proposed.getId2()) ) {
				removals.add(proposed);
				System.out.println("WARNING (proposed the same link twice): " + proposed);
			}
			// Normal link. Keep it.
			else {
				seenNew.add(proposed.getId1()+proposed.getId2());
				seenNew.add(proposed.getId2()+proposed.getId1());
			}
		}
		
		for( TLink remove : removals )
			proposedLinks.remove(remove);
		
		return removals.size();
	}
	
	/**
	 * DESTRUCTIVE FUNCTION (proposedLinks will be modified)
	 * Removes any links from the proposed list that already have links between the same pairs in currentLinks.
	 * @param currentLinksHash The list of current "good" links.
	 * @param proposedLinks The list of proposed new links.
	 * @return The number of links removed.
	 */
	private int removeConflicts(LinkTable currentLinksHash, List<TLink> proposedLinks) {
		List<TLink> removals = new ArrayList<TLink>();

		// Remove duplicates.
		int duplicates = removeDuplicatesAndInvalids(proposedLinks);
		if( debug && duplicates > 0 ) System.out.println("\t\tRemoved " + duplicates + " duplicate proposed links.");
		
		for( TLink proposed : proposedLinks ) {

			// Look for a current link that conflicts with this proposed link.
			LinkId proposedId = LinkId.of(proposed);
			TLink current = currentLinksHash.get(proposedId);
			if (current!=null) {
				LinkId currentId = LinkId.of(current);
				if (currentId.covers(proposedId)) {
					switch (current.getRelation()) {
						case UNKNOWN:
						case VAGUE:
						case NONE:
							// no harm in overwriting this weak tlink
							break;
						default:
							removals.add(proposed);
	
					}
				}
			}
		}

		for( TLink remove : removals )
			proposedLinks.remove(remove);

		return removals.size() + duplicates;
	}
	
	/**
	 * DESTRUCTIVE FUNCTION (links may have new TLink objects appended to it)
	 * Run transitive closure and add any inferred links.
	 * @param links The list of TLinks to expand with transitive closure.
	 * @return The list of new links from closure (these are already added to the given lists)
	 */
	private List<TLink> closureExpand(String sieveName, List<TLink> links, LinkTable linksHash) {
		List<TLink> newlinks = closure.computeClosure(links, true);
		addProposedToCurrentList(sieveName, newlinks, links, linksHash);
		return newlinks;
	}
    
	/**
	 * Given a path to a file or directory, assumes file(s) have proper XML markup with only 
	 * the TEXT and DCT elements present. It is assumed the text in TEXT is not marked up and
	 * contains just raw unlabeled sentences. If a directory, assumes all files in the 
	 * directory are of this XML form. This function will treat each text file as a separate 
	 * document and perform complete event, time, and tlink markup.
	 * @param path Single file or directory of text files.
	 */
	public void markupRawXML(String path) {
		markupRawXML(path,getDefaultFixedDct());
	}
	public void markupRawXML(String path, String dct) {
		SieveDocuments docs = new SieveDocuments();
		
		// If a directory: parse a directory of XML files.
		if( Directory.isDirectory(path) ) {
			for( String file : Directory.getFilesSorted(path) ) {
				String subpath = path + File.separator + file;
				try {
					SieveDocument doc = Tempeval3Parser.rawXMLtoSieveDocument(subpath, parser, gsf);
					docs.addDocument(doc);
				} catch( Exception ex ) {
					System.out.println("ERROR while processing " + subpath);
					ex.printStackTrace();
				}
			}
		}
		// If a single file: parse it.
		else {
			try {
				SieveDocument doc = Tempeval3Parser.rawXMLtoSieveDocument(path, parser, gsf);
				docs.addDocument(doc);
			} catch( Exception ex ) {
				System.out.println("ERROR while processing " + path);
				ex.printStackTrace();
			}
		}
		
		// Markup events, times, and tlinks.
		markupAll(docs, dct);
		
    // Output the documents.
		String outpath = path + ".info.xml";
		if( Directory.isDirectory(path) ) outpath = Directory.lastSubdirectory(path) + "-dir.info.xml";
		docs.writeToXML(outpath);
		System.out.println("Created " + outpath);
	}
	
	/**
	 * Given a path to a file or directory, assumes file(s) have no XML markup.
	 * If a directory, assumes all files in the directory are to be processed.
	 * This function will treat each text file as a separate document and perform complete
	 * event, time, and tlink markup.
	 * @param path Single file or directory of text files.
	 */
	public SieveDocuments markupRawText(String path) {
		return markupRawText(path,getDefaultFixedDct());
	}
	public SieveDocuments markupRawText(String path, String dct) {
		return markupRawText(path,true,dct);
	}
	public SieveDocuments markupRawText(String input, boolean isPath) {
		return markupRawText(input,isPath,getDefaultFixedDct());
	}
	public SieveDocuments markupRawText(String input, boolean isPath, String dct) {
		return markupRawText(input,isPath,dct,"input.txt");
	}	
	public SieveDocuments markupRawText(String input, boolean isPath, String dct, String dummyFileName) {
		SieveDocuments docs = new SieveDocuments();

		// If a directory: parse a directory of XML files.
		if (isPath) {
			String path = input;
			if( Directory.isDirectory(path) ) {
				for( String file : Directory.getFilesSorted(path) ) {
					String subpath = path + File.separator + file;
					SieveDocument doc = Tempeval3Parser.rawTextFileToParsed(subpath, parser, gsf);
					docs.addDocument(doc);
				}
			}
			// If a single file: parse it.
			else {
				SieveDocument doc = Tempeval3Parser.rawTextFileToParsed(path, parser, gsf);
				docs.addDocument(doc);
			}
		} else {
			SieveDocument doc = Tempeval3Parser.rawTextToParsed(dummyFileName, input, parser, gsf);
			docs.addDocument(doc);
		}

		// Markup events, times, and tlinks.
		markupAll(docs,dct);
		
		if (isPath) {
			String path = input;

			// Output the InfoFile with the events in it.
			String outpath = path + ".info.xml";
			if( Directory.isDirectory(path) ) outpath = Directory.lastSubdirectory(path) + "-dir.info.xml";
			docs.writeToXML(outpath);
			System.out.println("Created " + outpath);
		}
		
		return docs;
	}
	
	/**
	 * Assumes the InfoFile has its text parsed.
	 */
	
	public void markupAll() {
		markupAll(getDefaultFixedDct());
	}
	public void markupAll(String dct) {
		markupAll(thedocs,dct);
	}
	public void markupAll(SieveDocuments docs, String dct) {
		markupEvents(docs);
    // Try to determine DCT based on relevant property settings
		// TODO: use reflection method parallel to how sieves are chosen to choose the right DCTHeuristic method
		if (dctHeuristic.equals("setFirstDateAsDCT")) {
			for (SieveDocument doc : docs.getDocuments()) {
				DCTHeursitics.setFirstDateAsDCT(doc);  // only if there isn't already a DCT specified!
			}
		} else if (dctHeuristic.equals("setFixedDateAsDCT")) {
			String date = dct;
			for (SieveDocument doc : docs.getDocuments()) {
				DCTHeursitics.setFixedDateAsDCT(doc, date);
			}
		}
		markupTimexes(docs);
		markupTimexesDependencies(docs);
		runSieves(docs);
	}
	
	/**
	 * Assumes the SieveDocuments has its text parsed.
	 */
	public void markupEvents(SieveDocuments info) {
		eventClassifier.extractEvents(info);
	}
	
	/**
	 * Assumes the SieveDocuments has its text parsed.
	 */
	public void markupTimexes(SieveDocuments info) {
		if( timexClassifier == null ) {
			timexClassifier = new TimexClassifier();
			try {
				timexClassifier.setDebug(CaevoProperties.getBoolean("Main.debug", false));
			} catch (IOException ignore) {
			}
		}
		timexClassifier.markupTimex3(info);
	}
	
	public SieveDocuments getDataset(DatasetType type, SieveDocuments docs) {
		SieveDocuments dataset;
		if( type == DatasetType.TRAIN )
			dataset = Evaluate.getTrainSet(docs);
		else if( type == DatasetType.DEV )
			dataset = Evaluate.getDevSet(docs);
		else if( type == DatasetType.TEST )
			dataset = Evaluate.getTestSet(docs);
		else // ALL
			dataset = docs;
		
		// Fix DCTs that aren't 24-hour days.
		if( force24hrDCT ) force24hrDCTs(docs);
		
		return dataset;
	}
	
	private void force24hrDCTs(SieveDocuments docs) {
		if( docs != null ) {
			for( SieveDocument doc : docs.getDocuments() ) {
				List<Timex> dcts = doc.getDocstamp();
				if( dcts != null ) {
					for( Timex dct : dcts )
						Util.force24hrTimex(dct);
				}
			}
		}
	}
	
	/**
	 * Main. Multiple run modes. See comments on the top of this class definition.
	 */
	public static void main(String[] args) {
        //		Properties props = StringUtils.argsToProperties(args);
		Main main = new Main(args);

		// Test each sieve's precision independently.
		// Runs each sieve and evaluates its proposed links against the input -info file.
		if( args.length > 0 && args[args.length-1].equalsIgnoreCase("gauntlet") ) {
			main.runPrecisionGauntlet();
		}
		
		// The given SieveDocuments only has text and parses, so extract events/times first.
		else if( args.length > 0 && args[args.length-1].equalsIgnoreCase("parsed") ) {
			main.markupAll();
		}
        
		// Give a text file or a directory of text files. Parses and marks it up.
		else if( args.length > 1 && args[args.length-1].equalsIgnoreCase("raw") ) {
			main.dataset = DatasetType.ALL;
			main.markupRawText(args[args.length-2]);
		}
        
		// Give an XML file or a directory of text files. Parses and marks it up.
		else if( args.length > 1 && args[args.length-1].equalsIgnoreCase("rawxml") ) {
			main.dataset = DatasetType.ALL;
			main.markupRawXML(args[args.length-2]);
		}
		
		// The given SieveDocuments only has text and parses, so extract events/times first.
		else if( args.length > 0 && args[args.length-1].equalsIgnoreCase("trainall") ) {
			main.trainSieves();
		}
        
		// Run just the TLink Sieve pipeline. Events/Timexes already in the given SieveDocuments.
		else {
			main.runSieves();
		}
	}
}
