package caevo.time;

import java.util.List;

import caevo.Timex;
import caevo.time.pattern.DurationPattern;
import caevo.time.pattern.PatternResult;
import caevo.time.pattern.RelationPattern;
import caevo.time.pattern.TimePointPattern;

public class CorefParser {

	private static final DurationPattern durationPattern;
	private static final RelationPattern relationPattern;
	private static final TimePointPattern timePointPattern;
	private static final TemporalFunction temporalFunction;
	
	static {
		durationPattern = new DurationPattern();
		relationPattern = new RelationPattern();
		timePointPattern = new TimePointPattern();
		temporalFunction = new TemporalFunction();
	}

	public PatternResult<DurationData> checkDurationPattern(String text) {
		return durationPattern.check(text);
	}
	
	public PatternResult<RelationData> checkRelationPattern(String text) {
		return relationPattern.check(text);
	}

	public TimePointData parseTimePoint(String text, List<Timex> refTimexList) {
		return timePointPattern.resolve(text,refTimexList);
	}

	public RangeData applyTemporalFunction(DurationData durationData, RelationData relationData,DateTimeData dateTimeData) {
		return temporalFunction.apply(durationData,relationData,dateTimeData);
	}
	
	/*
	
	public PatternData checkTimeRef(Timex timex) {
		PatternData patternData = null;
		String text = timex.getText();

		List<CorefPattern> patternList = patterns.get(CorefType.TIMEREF);
		for (int pId=0, pCount=patternList.size() ; pId<pCount && patternData==null ; pId++) {
			patternData = patternList.get(pId).check(text);
		}
		
		return patternData;
	}

	
	public Timex check(Timex timex, Timex refTimex, List<CorefType> types) {

		Timex result = null;

		// we support only references to time points (not to durations)
		TimeData refTimeData = timeParser.parse(refTimex.getValue());
		if (refTimeData.getFormat().isTimePoint()) {

			// reference date time object
			DateTimeData refDateTimeData = (DateTimeData)refTimeData; 

			// first we check if the referring timex is a durative co-ref such as
			// 'the next year' or 'two days before' (if ARITHMETIC was resuested) or 
			// 'the same hour' (if TIMEREF was requested). we do this by parsing the 
			// text of the timex.
		
			
			if (types.contains(CorefType.TIMEREF) || types.contains(CorefType.ARITHMETIC)) {
			
				PatternData patternData = null;
				String text = timex.getText();
				for (int typeId=0, typesCount=types.size() ; typeId<typesCount && patternData==null ; typeId++) {
					List<CorefPattern> patternList = patterns.get(types.get(typeId));
					if (patternList!=null) {
						for (int pId=0, pCount=patternList.size() ; pId<pCount && patternData==null ; pId++) {
							patternData = patternList.get(pId).check(text);
						}
					}
				}
	
				// check if we found any supported pattern of durative co-ref
				if (patternData!=null) {
					
					TimeOperation operation = patternData.getOperation();
	
					DurationData duration = patternData.getDuration();
					TimeUnit unit = duration.getTimeUnit();
					Timex.Type type = unit.accept(dateTimeGetter);
					String newValue = null;
	
					if (types.contains(CorefType.TIMEREF) && operation==TimeOperation.GET) {
						
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
	
					} else if (types.contains(CorefType.ARITHMETIC) && 
										 (operation==TimeOperation.PLUS || operation==TimeOperation.MINUS)) {
	
						// this path is for cases like 'the next year' or 'two days before' where we
						// need to do some math to resolve the coref
	
						// make sure the reference is arithmetic-able
						if (refTimeData.getFormat().isArithmeticable()) {
	
							// apply the arithmetic operation
							DateTimeData newData = refDateTimeData.apply(operation,duration);
							newValue = newData.toString();
						}  else {
							// for error reporting
							throw new RuntimeException("Cannot perform co-ref operation " + operation + " on the reference time " + refTimex);
						}									
					} else {
						// for error reporting
						throw new RuntimeException("Unexpected operation " + operation + " on the reference time " + refTimex);
					}
					result = new Timex(timex, newValue, type);
				} 
			} else if (types.contains(CorefType.MERGE)) {
				
				// We check if this is a case of merging like: 'September' or 'September 17' or 
				// even a full time like 18:34, where the reference is to a date. It can also be 
				// an hour like 15 merged into a full date.
				// Note that the timex we create is up to the resolution requested by the referencing
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

			} else {
				// for error reporting
				throw new RuntimeException("None of the coref-types given are supported" + types + " on the reference time " + refTimex);
			}
			
		}
		
		return result;
	}
	*/

	/*	

	private static abstract class CorefPattern {

		
		public abstract PatternData check(String input);
	}	


	private static class TimeRef3 extends CorefPattern {

		private final static DurationPattern durationPattern;
		private final static RegExPattern regExPattern;
		
		static {
			durationPattern = new DurationPattern();
			regExPattern = new RegExPattern("^([ ]*)"+listToRegExChoice(Arrays.asList(PostUnitModifier.values())));			
		}

		@Override
		public PatternData check(String input) {
			PatternData patternData = null;
			
			// first check if we have a duration pattern, e.g. '2 years' or 'at least 4 hours'
			PatternResult<DurationData> durationResult = durationPattern.check(input);
			String remaining;
			int offset = 0;
			DurationData duration = null;
			if (durationResult!=null) {
				offset = durationResult.getLength();
				duration = durationResult.getData();
			}
			remaining = input.substring(offset);
			
			// now check if we have a post-duration modifier - 'after' or 'before' 
			Matcher matcher = regExPattern.check(remaining);
			if (matcher.find()) {
				PostUnitModifier mod = PostUnitModifier.valueOf(matcher.group(2));
				TimeOperation operation = mod.getOperation();
				int length = offset + matcher.end();
				patternData = new PatternData(duration, operation, length);
			}
			
			return patternData;
		}
	}
*/
	

	/*

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
	};*/


/*
	 * 

	private static class Pattern1 extends CorefPattern {

		private final static String pattern = "(THE )?" + preUnitModifiersChoice + " " + timeUnitsSingularChoice;
		
		private final static RegExPattern regExPattern = new RegExPattern(pattern);

		@Override
		public PatternData check(String input) {
			PatternData patternData = null;
			Matcher matcher = regExPattern.check(input.toUpperCase());
			if (matcher.matches()) {
				PreUnitModifier mod = PreUnitModifier.valueOf(matcher.group(2));
				TimeOperation operation = mod.getOperation();
				DurationData duration = new DurationData(1,null,TimeUnit.valueOf(matcher.group(3))); 
				
				patternData = new PatternData(duration,operation,length);
			}
			return patternData;
		}

	}
	private static class Pattern4 extends CorefPattern {

		private final static String pattern = timeUnitsPluralChoice + " " + postUnitModifiersChoice;

		private final static NumberPattern numberPattern = new NumberPattern();
		private final static NumberWordPattern numberWordPattern = new NumberWordPattern();
		private final static RegExPattern regExPattern = new RegExPattern(pattern.toUpperCase());

		@Override
		public PatternData check(String input) {
			PatternData patternData = null;
			
			// first we look for a number - either in digits - '132' or in words - 'twenty five' 
			NumberPatternResult numberResult = numberPattern.check(input);
			if (numberResult.getLength()<=0) {
				numberResult = numberWordPattern.check(input);
			}
			
			// if we found a number, and there is more room in the input string we check the regex
			if (numberResult.getLength()>0 && input.length()>numberResult.getLength()+1) {
				String remaining = input.substring(numberResult.getLength()+1);
				Matcher matcher = regExPattern.check(remaining.toUpperCase());
				if (matcher.matches()) {
					Format format = Format.valueOf(matcher.group(1));
					DurationData duration = new DurationData(numberResult.getValue(),null,format.accept(timeUnitGetter)); 
					PostUnitModifier mod = PostUnitModifier.valueOf(matcher.group(2));
					TimeOperation operation = mod.getOperation();
					int length
					patternData = new PatternData(duration,operation,length);
				}
			}
			return patternData;
		}

	}

	private static class Pattern5 extends CorefPattern {

		private final static String pattern = "(A|ONE) " + timeUnitsSingularChoice + " " + postUnitModifiersChoice;
		
		private final static RegExPattern regExPattern = new RegExPattern(pattern);

		@Override
		public PatternData check(String input) {
			PatternData patternData = null;
			Matcher matcher = regExPattern.check(input);
			if (matcher.matches()) {
				Format format = Format.valueOf(matcher.group(1));
				DurationData duration = new DurationData(1,null,format.accept(timeUnitGetter)); 
				PostUnitModifier mod = PostUnitModifier.valueOf(matcher.group(2));
				TimeOperation operation = mod.getOperation();
				int length
				patternData = new PatternData(duration,operation,length); 
			}			
			return patternData;
		}

	}
*/

	//patterns = new HashMap<CorefParser.CorefType, List<CorefParser.CorefPattern>>();
	/*
	patterns.put(CorefType.TIMEREF, Arrays.asList(
//			new TimeRef1(),	// {this,that} (same) {year,month,day,hour...}
//			new TimeRef2(),	// yesterday/tomorrow
			(CorefPattern)new TimeRef3()) // At least/at most/... (<quantity> {days,year..}) after Tuesday 
		); 
*/
	/*
	patterns.put(CorefType.ARITHMETIC, Arrays.asList(
			new Pattern1(), // (the) {same,next,previous,...} {year,month,day,hour...}
			new Pattern4(), // <quantity> {years,months,days,hours,...} {before,ago,after,later}
			new Pattern5()) // {a,one} {year,month,day,hour,...} {before,ago,after,later}
			// to add: "the year later"
		);
		*/

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

	/*
	public enum CorefType {
		TIMEREF,
		ARITHMETIC,
		MERGE,
	}
	

	private static final Map<CorefType,List<CorefPattern>> patterns;
*/
	
	
}
