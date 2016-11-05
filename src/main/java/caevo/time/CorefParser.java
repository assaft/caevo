package caevo.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import caevo.Timex;
import caevo.Timex.Type;

public class CorefParser {

	// Pattern 1 - (the) {same,next,previous,that,this} {year,month,day,hour,minute,second}
	// same year
	// next year
	// previous month
	// that day
	// next month
	// next hour
	// previous hour

	// (the) same/next/previous/last/coming/forthcoming
	// (that|this) [same] 
	
	// Pattern 2 - "(the) next day" and "previous day"
	// yesterday
	// tomorrow

	// Pattern 3 - <quantity> {years,months,days,hours,minutes,seconds} {before,ago,after,later}

	// Pattern 4 - {a,one} {year,month,day,hour,minute,second} {before,ago,after,later}

	private enum PreUnitModifier {
		SAME 				(TimeOperation.GET),
		NEXT 				(TimeOperation.PLUS),
		COMING 			(TimeOperation.PLUS),
		FORTHCOMING (TimeOperation.PLUS),
		PREVIOUS 		(TimeOperation.MINUS),
		LAST 				(TimeOperation.MINUS);

		private final TimeOperation operation;

		PreUnitModifier(TimeOperation operation) {
			this.operation = operation;
		}

		public TimeOperation getOperation() {
			return operation;
		}

	}

	private enum PreSelfUnitModifier {
		THIS 				(TimeOperation.GET),
		THAT 				(TimeOperation.GET);
		
		private final TimeOperation operation;

		PreSelfUnitModifier(TimeOperation operation) {
			this.operation = operation;
		}

		public TimeOperation getOperation() {
			return operation;
		}
		
	}
	
	private enum PostUnitModifier {
		BEFORE 	(TimeOperation.MINUS),
		AGO 		(TimeOperation.MINUS),
		AFTER 	(TimeOperation.PLUS), 
		LATER		(TimeOperation.PLUS);

		private final TimeOperation operation;
		
		PostUnitModifier(TimeOperation operation) {
			this.operation = operation;
		}

		public TimeOperation getOperation() {
			return operation;
		}

	}

	private static final TimeParser timeParser = new TimeParser();

	private static final List<CorefPattern> patterns = Arrays.asList(
			new Pattern1(),  // (the) {same,next,previous,...} {year,month,day,hour...}
			new Pattern2(),  // {this,that} (same) {year,month,day,hour...}
			new Pattern3(),  // yesterday/tomorrow 
			new Pattern4(),  // <quantity> {years,months,days,hours,...} {before,ago,after,later} 
			new Pattern5()); // {a,one} {year,month,day,hour,...} {before,ago,after,later}
	

	public Timex check(Timex timex, Timex refTimex) {
		Timex result = null;

		// we support only references to time points (not to durations)
		TimeData refTimeData = timeParser.parse(refTimex.getValue());
		if (refTimeData.getFormat().isTimePoint()) {

			// reference date time object
			DateTimeData refDateTimeData = (DateTimeData)refTimeData; 

			// first we check if the referring timex is a durative co-ref such as
			// 'the next year' or 'two days before', 'the same hour', etc. 
			// we do this by parsing the text of the timex.

			String text = timex.getText();
			DurativePatternData patternData = null;
			for (int i=0, size=patterns.size() ; i<size && patternData==null ; i++) {
				patternData = patterns.get(i).check(text); 
			}

			// check if we found any supported pattern of durative co-ref
			if (patternData!=null) {

				// resolve the durative co-ref

				TimeOperation operation = patternData.getOperation();
				DurationData duration = patternData.getDuration();
				TimeUnit unit = duration.getTimeUnit();
				Timex.Type type = unit.accept(dateTimeGetter);
				String newValue = null;

				// it's a case of a plus/minus operation
				if ((operation==TimeOperation.PLUS || operation==TimeOperation.MINUS)) {

					// this path is for cases like 'the next year' or 'two days before' where we
					// need to do some math to resolve the coref

					// make sure the reference is arithmetic-able
					if (refTimeData.getFormat().isArithmeticable()) {

						// apply the arithmetic operation
						newValue = refDateTimeData.apply(operation,duration);
					}  else {
						// for error reporting
						throw new RuntimeException("Cannot perform co-ref operation " + operation + " on the reference time " + refTimex);
					}									
				} else if (operation==TimeOperation.GET) {

					// this path is for cases like 'the same year' - where we only need to retrieve info
					// from the referenced timex.

					// make sure it is possible to extract the requested unit from the reference
					if (refTimeData.getFormat().has(unit)) {

						// retrieve the requested info up to the resolution requested by the referring
						// timex. For example, when a timex like 'the same day' refers to a full 
						// date-time like 2012-10-04 14:11:23, we return 2012-10-04. 
						DateTimeData extracted = refDateTimeData.extract(unit);
						newValue = extracted.toString();
					} else {
						// for error reporting
						throw new RuntimeException("Cannot extract the time unit " + unit + " from the reference time " + refTimex);
					}
				} else {
					// for error reporting
					throw new RuntimeException("Unexpected operation " + operation + " on the reference time " + refTimex);
				}
				result = new Timex(timex, newValue, type);

			} else {

				// we didn't find any durative pattern so we will check if this is a case of merging
				// like: 'September' or 'September 17' or even a full time like 18:34, where the 
				// reference is to a date. It can also be an hour like 15 merged into a full date.
				// note that the timex we create is up to the resolution requested by the referencing
				// item. So for example in the case of the hour 15 referring to a full date like
				// 2015-04-03 16:12:11 we will return 2015-04-03 15. 

				// verify that we deal with a time point in the referring timex
				TimeData timeData = timeParser.parse(timex.getValue());
				if (timeData.getFormat().isTimePoint()) {
					DateTimeData dateTimeData = (DateTimeData)timeData;
					DateTimeData merged = refDateTimeData.merge(dateTimeData);
					String newValue = merged.toString();
					Timex.Type type = merged.getFormat().hasDate() ? Type.DATE : Type.TIME;
					result = new Timex(timex, newValue, type);
				} 
			}
		}

		return result;
	}

	private static class DurativePatternData {
		private final DurationData duration;
		private final TimeOperation operation;

		public DurativePatternData(DurationData duration, TimeOperation operation) {
			super();
			this.duration = duration;
			this.operation = operation;
		}

		public DurationData getDuration() {
			return duration;
		}

		public TimeOperation getOperation() {
			return operation;
		}
	}

	private static abstract class CorefPattern {

		protected static String timeUnitsSingularChoice;
		protected static String timeUnitsPluralChoice;

		protected static String preUnitModifiersChoice;
		protected static String preSelfUnitModifiersChoice;
		protected static String postUnitModifiersChoice;

		private static <A> String listToRegExChoice(List<A> list) {
			StringBuilder buffer = new StringBuilder();
			buffer.append("(");
			for (A item : list) {
				buffer.append(item.toString());
				buffer.append("|");
			}
			buffer.setCharAt(buffer.length()-1, ')');
			return buffer.toString();
		}
		
		static {
			List<TimeUnit> timeUnitList = Arrays.asList(TimeUnit.values());
			List<String> timeUnitSingularStrList = new ArrayList<String>();
			List<String> timeUnitPluralStrList = new ArrayList<String>();
			for (TimeUnit timeUnit : timeUnitList) {
				timeUnitSingularStrList.add(timeUnit.toString());
				timeUnitPluralStrList.add(timeUnit.toString()+"s");			
			}

			timeUnitsSingularChoice = listToRegExChoice(timeUnitSingularStrList);
			timeUnitsPluralChoice = listToRegExChoice(timeUnitPluralStrList);

			preUnitModifiersChoice = listToRegExChoice(Arrays.asList(PreUnitModifier.values()));
			preSelfUnitModifiersChoice = listToRegExChoice(Arrays.asList(PreSelfUnitModifier.values()));
			postUnitModifiersChoice = listToRegExChoice(Arrays.asList(PostUnitModifier.values()));
		}		
		
		public abstract DurativePatternData check(String input);
	}	

	private static class Pattern1 extends CorefPattern {

		private final static String pattern = "(THE )?" + preUnitModifiersChoice + " " + timeUnitsSingularChoice;
		
		private final static RegExPattern regExPattern = new RegExPattern(pattern);

		@Override
		public DurativePatternData check(String input) {
			DurativePatternData patternData = null;
			Matcher matcher = regExPattern.check(input.toUpperCase());
			if (matcher.matches()) {
				PreUnitModifier mod = PreUnitModifier.valueOf(matcher.group(2));
				TimeOperation operation = mod.getOperation();
				DurationData duration = new DurationData(1,TimeUnit.valueOf(matcher.group(3))); 
				patternData = new DurativePatternData(duration,operation);
			}
			return patternData;
		}

	}

	private static class Pattern2 extends CorefPattern {

		private final static String pattern = preSelfUnitModifiersChoice + " (SAME )?" + timeUnitsSingularChoice;
		
		private final static RegExPattern regExPattern = new RegExPattern(pattern);

		@Override
		public DurativePatternData check(String input) {
			DurativePatternData patternData = null;
			Matcher matcher = regExPattern.check(input.toUpperCase());
			if (matcher.matches()) {
				PreSelfUnitModifier mod = PreSelfUnitModifier.valueOf(matcher.group(1));
				TimeOperation operation = mod.getOperation();
				DurationData duration = new DurationData(1,TimeUnit.valueOf(matcher.group(3))); 
				patternData = new DurativePatternData(duration,operation);
			}
			return patternData;
		}

	}	
	private static class Pattern3 extends CorefPattern {

		private final static String pattern = "(the)+ (yesterday|tomorrow)";
		
		private final static RegExPattern regExPattern = new RegExPattern(pattern);

		@Override
		public DurativePatternData check(String input) {
			DurativePatternData patternData = null;
			Matcher matcher = regExPattern.check(input.toUpperCase());
			if (matcher.matches()) {
				DurationData duration = new DurationData(1,TimeUnit.DAY); 
				TimeOperation operation = input.equals("YESTERDAY") ? TimeOperation.MINUS : TimeOperation.PLUS;
				patternData = new DurativePatternData(duration,operation); 
			}
			return patternData;
		}
	}

	private static class Pattern4 extends CorefPattern {

		private final static String pattern = timeUnitsPluralChoice + " " + postUnitModifiersChoice;
		
		private final static NumberWordPattern numberPattern = new NumberWordPattern();
		private final static RegExPattern regExPattern = new RegExPattern(pattern.toUpperCase());

		@Override
		public DurativePatternData check(String input) {
			DurativePatternData patternData = null;
			NumberWordPatternResult numberWordResult = numberPattern.check(input);
			if (numberWordResult.getLength()>0) {
				String remaining = input.substring(numberWordResult.getLength()+1);
				Matcher matcher = regExPattern.check(remaining.toUpperCase());
				if (matcher.matches()) {
					Format format = Format.valueOf(matcher.group(1));
					DurationData duration = new DurationData(numberWordResult.getValue(),format.accept(timeUnitGetter)); 
					PostUnitModifier mod = PostUnitModifier.valueOf(matcher.group(2));
					TimeOperation operation = mod.getOperation();
					patternData = new DurativePatternData(duration,operation);
				}
			}
			return patternData;
		}

	}

	private static class Pattern5 extends CorefPattern {

		private final static String pattern = "(A|ONE) " + timeUnitsSingularChoice + " " + postUnitModifiersChoice;
		
		private final static RegExPattern regExPattern = new RegExPattern(pattern);

		@Override
		public DurativePatternData check(String input) {
			DurativePatternData patternData = null;
			Matcher matcher = regExPattern.check(input.toUpperCase());
			if (matcher.matches()) {
				Format format = Format.valueOf(matcher.group(1));
				DurationData duration = new DurationData(1,format.accept(timeUnitGetter)); 
				PostUnitModifier mod = PostUnitModifier.valueOf(matcher.group(2));
				TimeOperation operation = mod.getOperation();
				patternData = new DurativePatternData(duration,operation); 
			}			
			return patternData;
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
