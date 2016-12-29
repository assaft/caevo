package caevo.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import caevo.SieveSentence;
import caevo.Timex;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.Tree;

public class TimexCorefResolver {

	private final Map<String,List<String>> timexRefMap;

	private final static String valQuarterRegex = "\\d{4}-Q\\d";
	private final static Pattern valQuarter = Pattern.compile(valQuarterRegex);

	public TimexCorefResolver(List<SieveSentence> sentences) {
		// a set for sorting the timexes
		Set<TimeXInfo> sentenceTimeXInfo = new TreeSet<TimeXInfo>(timeXInfoComp);

		// go over all sentences
		for (int sid = 0 ; sid < sentences.size() ; sid++) {
			SieveSentence sentence = sentences.get(sid);

			// get the constituents in the tree of the sentence
			Tree tree = sentence.getParseTree();
			Set<Constituent> constituents = tree.constituents();
			

			System.out.println(constituents);
			System.out.println(sentence.getParseTree().toString());


			Map<Integer,String> tokens = new TreeMap<Integer, String>();
			
			// go over all timexes
			for (Timex timex : sentence.timexes()) {

				// Ensure timex passes eligibility criteria specified in validateTimex
				if (validateTimex(timex)) {

					int tokenOffset = timex.getTokenOffset()-1;

					tokens.put(tokenOffset,timex.getTid());
					
					// calculate the depth of the timex
					//int depth = findDepth(tokenOffset,tree);
					/*
        	int depth = 0;
        	for (Constituent c : constituents) {
        		if (c.start()<=tokenOffset && tokenOffset<=c.end()) {
        			depth++;
        		}
        	}

					// add the timex to the sorted set
					sentenceTimeXInfo.add(new TimeXInfo(sid,depth,tokenOffset,timex.getTid()));
					*/
				}
			}
			
			Map<Integer,TokenProperties> tokenPropertiesMap = analyze(tokens.keySet(), tree);
			for (Entry<Integer, TokenProperties> tokenEntry : tokenPropertiesMap.entrySet()) {
				TokenProperties tokenProperties = tokenEntry.getValue();
				sentenceTimeXInfo.add(new TimeXInfo(sid+tokenProperties.getSentence(),
						tokenProperties.getDepth(),tokenEntry.getKey(),tokens.get(tokenEntry.getKey())));
			}
			
		}

		// create a list of the sorted set
		List<TimeXInfo> list = new ArrayList<TimeXInfo>(sentenceTimeXInfo);

		// create the co-ref map
		timexRefMap = new HashMap<String, List<String>>();

		// default coref list
		List<String> corefList = Arrays.asList("t0");

		for (int i=0 ; i<list.size() ; i++) {

			// begin with the previous list and insert the last item at the head (if possible)
			corefList = new ArrayList<String>(corefList);
			if (i>0) {
				corefList.add(0,list.get(i-1).getTId());
			}

			// save item's coref list
			timexRefMap.put(list.get(i).getTId(), corefList);
		}

	}

	private Map<Integer,TokenProperties> analyze(Collection<Integer> tokenIds, Tree tree) {
		// prepare properties map
		Map<Integer,TokenProperties> tokenPropertiesMap = new TreeMap<Integer, TimexCorefResolver.TokenProperties>();
		
		// collect properties
		analyze(tree,0,0,0,new ArrayList<Integer>(tokenIds),tokenPropertiesMap);
		
		// normalize the sentence ids
		int lastSentenceId = -1;
		for (int tokenId : tokenPropertiesMap.keySet()) {
			TokenProperties tokenProperties = tokenPropertiesMap.get(tokenId); 
			if (tokenProperties.getSentence()>lastSentenceId) {
				lastSentenceId++;
				tokenPropertiesMap.put(tokenId,
						new TokenProperties(lastSentenceId,tokenProperties.getDepth()));
			}
		}
		return tokenPropertiesMap;
	}

	private void analyze(Tree tree, int offset, int aggDepth, int aggSentence,
			List<Integer> tokenIds, Map<Integer,TokenProperties> tokenPropertiesMap) {
		if (tree.numChildren()==0 && tokenIds.size()>0) {
			if (tokenIds.get(0)==offset) {
				tokenPropertiesMap.put(offset, new TokenProperties(aggSentence,aggDepth));
				tokenIds.remove(0);
			}
		} else {
			if (tree.label().value().equals("S")) {
				aggSentence++;
				aggDepth = 0;
			} else {
				aggDepth++;
			}
			int prevChildOffset = 0;
			for (int childId = 0 ; childId<tree.numChildren() && tokenIds.size()>0 ; childId++)	 {
				int childLength = tree.getChild(childId).getLeaves().size();
				analyze(tree.getChild(childId),offset+prevChildOffset,aggDepth,aggSentence,tokenIds,tokenPropertiesMap);
				prevChildOffset+=childLength;
			}
		}
	}

	private class TokenProperties {
		private final int sentence;
		private final int depth;

		public TokenProperties(int sentence, int depth) {
			super();
			this.sentence = sentence;
			this.depth = depth;
		}

		public int getSentence() {
			return sentence;
		}
		
		public int getDepth() {
			return depth;
		}

		public String toString() {
			return sentence + "/" + depth;
		}
	}

	public List<String> resolve(String tid) {
		return timexRefMap.get(tid);
	}

	// validateTime ensures that timex value meets criteria
	private Boolean validateTimex(Timex timex) {
		String val = timex.getValue();
		// Return false if timex value is not a date or is a quarter
		Matcher m = valQuarter.matcher(val);
		return !m.matches();
	}

	private class TimeXInfo {
		private final int sentence;
		private final int depth;
		private final int offset;
		private final String tid;

		public TimeXInfo(int sentence, int depth, int offset, String tid) {
			super();
			this.sentence = sentence;
			this.depth = depth;
			this.offset = offset;
			this.tid = tid;
		}

		public int getSentence() {
			return sentence;
		}

		public int getDepth() {
			return depth;
		}

		public int getOffset() {
			return offset;
		}

		public String getTId() {
			return tid;
		}

		@Override
		public String toString() {
			return tid + "(" + sentence + "/" + depth + "/" + offset + ")";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + depth;
			result = prime * result + offset;
			result = prime * result + sentence;
			result = prime * result + ((tid == null) ? 0 : tid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TimeXInfo other = (TimeXInfo) obj;
			if (depth != other.depth)
				return false;
			if (offset != other.offset)
				return false;
			if (sentence != other.sentence)
				return false;
			if (tid == null) {
				if (other.tid != null)
					return false;
			} else if (!tid.equals(other.tid))
				return false;
			return true;
		}

	}

	private static Comparator<TimeXInfo> timeXInfoComp = new Comparator<TimeXInfo>() {

		@Override
		public int compare(TimeXInfo e1, TimeXInfo e2) {
			int cmp = Integer.compare(e1.getSentence(), e2.getSentence());
			if (cmp==0) {
				cmp = Integer.compare(e1.getDepth(), e2.getDepth());
			}
			if (cmp==0) {
				cmp = Integer.compare(1000-e1.getOffset(), 1000-e2.getOffset());
			}
			return cmp;
		}
	};


}
