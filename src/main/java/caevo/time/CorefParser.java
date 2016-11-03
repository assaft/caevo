package caevo.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import caevo.Timex;

public class CorefParser {

	// Pattern 1 - (the) {same,next,previous,that,this} {year,month,day,hour,minute,second}
	// same year
	// next year
	// previous month
	// that day
	// next month
	// next hour
	// previous hour

	// Pattern 2 - "(the) next day" and "previous day"
	// yesterday
	// tomorrow

	// Pattern 3 - <quantity> {years,months,days,hours,minutes,seconds} {before,ago,after,later}

	// Pattern 4 - {a,one} {year,month,day,hour,minute,second} {before,ago,after,later}

	private static final String pattern1;
	private static final String pattern2;
	private static final String pattern3;
	private static final String pattern4;
	
	private enum Direction {NONE,POS,NEG}
	
	private enum PreUnitModifier {
		SAME 				(Direction.NONE),
		THIS 				(Direction.NONE),
		THAT 				(Direction.NONE),
		NEXT 				(Direction.POS),
		COMING 			(Direction.POS),
		FORTHCOMING (Direction.POS),
		PREVIOUS 		(Direction.NEG),
		LAST 				(Direction.NEG);
		
		private final Direction direction;
		PreUnitModifier(Direction direction) {
			this.direction = direction;
		}
		
		public Direction getDirection() {
			return direction;
		}
	
	}
	private enum PostUnitModifier {
		BEFORE 	(Direction.NEG),
		AGO 		(Direction.NEG),
		AFTER 	(Direction.POS), 
		LATER		(Direction.POS);
		
		private final Direction direction;
		PostUnitModifier(Direction direction) {
			this.direction = direction;
		}
		
		public Direction getDirection() {
			return direction;
		}
		
	}
	
	private static final List<String> preUnitModifiers = 
			Arrays.asList("same","next","previous","last","coming","forthcoming","this","that");

	private static List<String> postUnitModifiers = 
			Arrays.asList("before","ago","after","later");

	private static String listToRegExChoice(List<String> list) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("(");
		for (String s : list) {
			buffer.append(s);
			buffer.append("|");
		}
		buffer.setCharAt(buffer.length()-1, ')');
		return buffer.toString();
	}

	private static List<CorefPattern> patterns;
	
	static {
		List<TimeUnit> timeUnitList = Arrays.asList(TimeUnit.values());
		List<String> timeUnitSingularStrList = new ArrayList<String>();
		List<String> timeUnitPluralStrList = new ArrayList<String>();
		for (TimeUnit timeUnit : timeUnitList) {
			timeUnitSingularStrList.add(timeUnit.toString());
			timeUnitPluralStrList.add(timeUnit.toString()+"s");			
		}

		String timeUnitsSingularChoice = listToRegExChoice(timeUnitSingularStrList);
		String timeUnitsPluralChoice = listToRegExChoice(timeUnitPluralStrList);

		String preUnitModifiersChoice = listToRegExChoice(preUnitModifiers);
		String postUnitModifiersChoice = listToRegExChoice(postUnitModifiers);

		pattern1 = "(the)+ " + preUnitModifiersChoice + " " + timeUnitsSingularChoice;
		pattern2 = "(the)+ (yesterday|tomorrow)";
		pattern3 = timeUnitsPluralChoice + " " + postUnitModifiersChoice;
		pattern4 = "(a|one) " + timeUnitsSingularChoice + " " + postUnitModifiersChoice;
		
		patterns = Arrays.asList(new Pattern1(),new Pattern2(), new Pattern3(), new Pattern4());
	}

	public CorefHandler parse(String input) {
		CorefHandler corefHandler = null;
		for (int i=0, size=patterns.size() ; i<size && corefHandler==null ; i++) {
			corefHandler = patterns.get(i).check(input); 
		}
		return corefHandler;
	}
	
	public interface PatternContext {
		Timex apply(Timex timex, Timex refTimex);
	}
	
	public interface CorefHandler {
		public Timex apply(Timex timex, Timex refTimex);
	}
	
	private static abstract class CorefPattern {
		public abstract CorefHandler check(String input);

		private static final TimeParser timeParser = new TimeParser();
		
		protected CorefHandler createCorefHandler(final int quantity, final TimeUnit unit,
				final Direction direction, final int tokenCount) {
			return new CorefHandler() {
				@Override
				public Timex apply(Timex timex, Timex refTimex) {
					Timex result = null;
					TimeData refTimeData = timeParser.parse(refTimex.getValue());
					if (refTimeData.getFormat().has(unit)) {
						int refValue = ((DateTimeData)refTimeData).get(unit);
						int newValue = direction==Direction.NONE ? refValue
								: refValue + quantity * (direction==Direction.POS ? +1 : -1);
						Timex.Type type = unit.accept(dateTimeGetter);
						result = new TimexCoref(timex, Integer.toString(newValue), type, tokenCount);
					}
					return result;
				}
			};
		}
		
	}

	private static class Pattern1 extends CorefPattern {
		
		private final static RegExPattern regExPattern = new RegExPattern(pattern1);
		
		@Override
		public CorefHandler check(String input) {
			CorefHandler corefHandler = null;
			Matcher matcher = regExPattern.check(input.toUpperCase());
			if (matcher.matches()) {
				final int quantity = 1;
				final PreUnitModifier mod = PreUnitModifier.valueOf(matcher.group(2));
				final TimeUnit unit = TimeUnit.valueOf(matcher.group(3));
				final Direction direction = mod.getDirection();
				final int tokenCount = input.split(" ").length;
				corefHandler = createCorefHandler(quantity,unit,direction,tokenCount);
			}
			return corefHandler;
		}
		
	}
	
	private static class Pattern2 extends CorefPattern {
		
		private final static RegExPattern regExPattern = new RegExPattern(pattern2);
		
		@Override
		public CorefHandler check(String input) {
			CorefHandler corefHandler = null;
			Matcher matcher = regExPattern.check(input.toUpperCase());
			if (matcher.matches()) {
				int quantity = 1;
				TimeUnit unit = TimeUnit.DAY;
				Direction direction = input.equals("YESTERDAY") ? Direction.NEG : Direction.POS;
				int tokenCount = 1;
				corefHandler = createCorefHandler(quantity,unit,direction,tokenCount);
			}
			return corefHandler;
		}
	}
	
	private static class Pattern3 extends CorefPattern {
		
		private final static NumberWordPattern numberPattern = new NumberWordPattern();
		private final static RegExPattern regExPattern = new RegExPattern(pattern3.toUpperCase());
		
		@Override
		public CorefHandler check(String input) {
			CorefHandler corefHandler = null;
			NumberWordPatternResult numberWordResult = numberPattern.check(input);
			if (numberWordResult.getLength()>0) {
				String remaining = input.substring(numberWordResult.getLength()+1);
				Matcher matcher = regExPattern.check(remaining.toUpperCase());
				if (matcher.matches()) {
					int quantity = numberWordResult.getValue();
					Format format = Format.valueOf(matcher.group(1));
					TimeUnit unit = format.accept(timeUnitGetter);
					PostUnitModifier mod = PostUnitModifier.valueOf(matcher.group(2));
					Direction direction = mod.getDirection();
					int tokenCount = input.split(" ").length;
					corefHandler = createCorefHandler(quantity,unit,direction,tokenCount);
				}
			}
			return corefHandler;
		}

	}
	private static class Pattern4 extends CorefPattern {
		
		private final static RegExPattern regExPattern = new RegExPattern(pattern4);
		
		@Override
		public CorefHandler check(String input) {
			CorefHandler corefHandler = null;
			Matcher matcher = regExPattern.check(input.toUpperCase());
			if (matcher.matches()) {
				int quantity = 1;
				Format format = Format.valueOf(matcher.group(1));
				TimeUnit unit = format.accept(timeUnitGetter);
				PostUnitModifier mod = PostUnitModifier.valueOf(matcher.group(2));
				Direction direction = mod.getDirection();
				int tokenCount = input.split(" ").length;
				corefHandler = createCorefHandler(quantity,unit,direction,tokenCount);
			}			
			return corefHandler;
		}

	}

	private static class RegExPattern {
		private final Pattern pattern;

		public RegExPattern(String regex) {
			this.pattern = Pattern.compile(regex);
		}
		
		public Matcher check(String input) {
			return pattern.matcher(input);
		}
	}

	private static class NumberWordPattern {
		private static Set<String> allowedStrings = new TreeSet<String>(Arrays.asList(
				"zero","one","two","three","four","five","six","seven",
				"eight","nine","ten","eleven","twelve","thirteen","fourteen",
				"fifteen","sixteen","seventeen","eighteen","nineteen","twenty",
				"thirty","forty","fifty","sixty","seventy","eighty","ninety",
				"hundred","thousand","million","billion","trillion"));

		public NumberWordPatternResult check(String input) {

			int finalResult = -1;				

			boolean inPrefix = true;
			int prefixWords = 0;
			int prefixLength = 0;
			
			if(input != null && input.length()> 0) {
				input = input.replaceAll("-", " ");
				input = input.toLowerCase().replaceAll(" and", " ");
				String[] splittedParts = input.trim().split("\\s+");


				for (int i=0 ; i<splittedParts.length && inPrefix; i++) {
					inPrefix = allowedStrings.contains(splittedParts[i]);
					if (inPrefix) {
						prefixWords++;
						prefixLength+=splittedParts[i].length()+1;
					}
				}

				int result = 0;
				
				finalResult = 0;				
				for (int i=0 ; i<=prefixWords ; i++) {
					String str = splittedParts[i];
					if(str.equalsIgnoreCase("zero")) 					{result += 0;}
					else if(str.equalsIgnoreCase("one")) 			{result += 1;}
					else if(str.equalsIgnoreCase("two")) 			{result += 2;}
					else if(str.equalsIgnoreCase("three")) 		{result += 3;}
					else if(str.equalsIgnoreCase("four")) 		{result += 4;}
					else if(str.equalsIgnoreCase("five")) 		{result += 5;}
					else if(str.equalsIgnoreCase("six")) 			{result += 6;}
					else if(str.equalsIgnoreCase("seven")) 		{result += 7;}
					else if(str.equalsIgnoreCase("eight")) 		{result += 8;}
					else if(str.equalsIgnoreCase("nine")) 		{result += 9;}
					else if(str.equalsIgnoreCase("ten")) 			{result += 10;}
					else if(str.equalsIgnoreCase("eleven")) 	{result += 11;}
					else if(str.equalsIgnoreCase("twelve")) 	{result += 12;}
					else if(str.equalsIgnoreCase("thirteen")) {result += 13;}
					else if(str.equalsIgnoreCase("fourteen")) {result += 14;}
					else if(str.equalsIgnoreCase("fifteen")) 	{result += 15;}
					else if(str.equalsIgnoreCase("sixteen")) 	{result += 16;}
					else if(str.equalsIgnoreCase("seventeen")){result += 17;}
					else if(str.equalsIgnoreCase("eighteen")) {result += 18;}
					else if(str.equalsIgnoreCase("nineteen")) {result += 19;}
					else if(str.equalsIgnoreCase("twenty")) 	{result += 20;}
					else if(str.equalsIgnoreCase("thirty")) 	{result += 30;}
					else if(str.equalsIgnoreCase("forty"))		{result += 40;}
					else if(str.equalsIgnoreCase("fifty")) 		{result += 50;}
					else if(str.equalsIgnoreCase("sixty")) 		{result += 60;}
					else if(str.equalsIgnoreCase("seventy")) 	{result += 70;}
					else if(str.equalsIgnoreCase("eighty")) 	{result += 80;}
					else if(str.equalsIgnoreCase("ninety")) 	{result += 90;}
					else if(str.equalsIgnoreCase("hundred")) 	{result *= 100;}
					else if(str.equalsIgnoreCase("thousand")) {
						result *= 1000;
						finalResult += result;
						result=0;
					}	else if(str.equalsIgnoreCase("million")) {
						result *= 1000000;
						finalResult += result;
						result=0;
					}					}
				finalResult += result;
			}
			return new NumberWordPatternResult(finalResult, prefixLength-1);
		}
	}
	
	private static class NumberWordPatternResult {
		private final int value;
		private final int length;
		
		public NumberWordPatternResult(int value, int length) {
			super();
			this.value = value;
			this.length = length;
		}

		public int getValue() {
			return value;
		}

		public int getLength() {
			return length;
		}
		
	}

	private static TimeUnit.Visitor<Timex.Type> dateTimeGetter = new TimeUnit.Visitor<Timex.Type>() {
		@Override	public Timex.Type visitYear() 		{return Timex.Type.DATE;}
		@Override public Timex.Type visitMonth() 		{return Timex.Type.DATE;}
		@Override public Timex.Type visitDay() 			{return Timex.Type.DATE;}
		@Override public Timex.Type visitHour() 		{return Timex.Type.TIME;}
		@Override public Timex.Type visitMinute() 	{return Timex.Type.TIME;}
		@Override public Timex.Type visitSecond() 	{return Timex.Type.TIME;}
	};
	
	private static Format.DurationVisitor<TimeUnit> timeUnitGetter = new Format.DurationVisitor<TimeUnit>() {
		@Override	public TimeUnit visitYears() 		{return TimeUnit.YEAR;}
		@Override public TimeUnit visitMonths() 	{return TimeUnit.MONTH;}
		@Override public TimeUnit visitDays() 		{return TimeUnit.DAY;}
		@Override public TimeUnit visitHours() 		{return TimeUnit.HOUR;}
		@Override public TimeUnit visitMinutes() 	{return TimeUnit.MINUTE;}
		@Override public TimeUnit visitSeconds() 	{return TimeUnit.SECOND;}
	};
	
}
