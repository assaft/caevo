package caevo.time;

import caevo.CorefWord;
import caevo.CorefWord.Visitor;
import caevo.Timex;

public class TimexCoref extends Timex {
	public enum Direction {NEXT, PREV}

	private final static TimeParser timeParser = new TimeParser();

	//private final String temporalOperation;
	private final int leftSpanLength;

	public TimexCoref(Timex timex, String value, Timex.Type type, int leftSpanLength) {
		super(timex);
		setValue(value);
		setType(type);
		//this.temporalOperation = temporalOperation;
		this.leftSpanLength = leftSpanLength;
	}

	public static TimexCoref of(final Timex timex,final Timex timexRef, final CorefWord corefWord, int leftSpanLength, final boolean calculate) {

		String value = timex.getValue();
		String operation = null;
		Timex.Type type = null;

		TimeData timexData = timeParser.parse(timex.getValue());
		TimeData timexRefData = timeParser.parse(timexRef.getValue());
		if (timexData!=null && timexRefData!=null) {
			if (timexRefData.getFormat().isAbsolute()) {

				// case 1: using co-ref to resolve under-specification of date/time.
				// e.g. September 17 is resolved by August 11, 1980 to September 17, 1980. 
				if (timexData.getFormat().isAbsolute()) {
					/*
					if (timexData.getFormat()==MonthDay) {
						temporalOperation = "set("+timex.getTid()+","+timexRef.getTid()+")";
						DateTimeData dateTimeData = (DateTimeData)timexData;
						DateTimeData dateTimeDataRef = (DateTimeData)timexRefData;
						DateTimeData newDateTimeData = new DateTimeData(
								dateTimeDataRef.getYear(),dateTimeDataRef.getMonth(),dateTimeDataRef.getDay(),
								dateTimeDataRef.getHour(),dateTimeData.getMinute(),dateTimeData.getSecond(),
								dateTimeDataRef.getFormat());

					} else if (timexData.getFormat()==MinuteSecond) {
					} 
					} else if (timexData.getFormat().isDurative()) {
					}*/

					// case 2: using co-ref to calculate/retrieve a value of a time unit. For example,
					// when we have "In 1939,....." and then "Two years later,..", the coref "two years
					// layer" is to be calculated based on the timex '1939'.  	
				} else if (timexData.getFormat().isDurative()) {
					int quantity = ((DurationData)timexData).getQuantity();
					int refValue;

					String opSymbol = corefWord.accept(opSymbolGetter);
					TimeUnit unit = timexData.getFormat().accept(timeUnitGetter);
					type = timexData.getFormat().accept(dateTimeGetter);
					
					String funcName = unit.toString().toLowerCase();
					operation = funcName +"(" + timexRef.getTid() + ")";
					if (opSymbol!=null) {
						operation = operation + opSymbol + quantity;
					}
					
					if (calculate) {
						if (timexRefData.getFormat().has(unit)) {
							refValue = ((DateTimeData)timexRefData).get(unit);
							if (opSymbol!=null) {
								value = Integer.toString(refValue + quantity * (opSymbol.equals("-") ? -1 : +1));
							} else {
								value = Integer.toString(refValue);
							}
						}
					}

				}
			}
		}
		
		return operation!=null ? new TimexCoref(timex, value, /*operation, */type, leftSpanLength) : null; 
	}

	/*
	public String getTemporalOperation() {
		return temporalOperation;
	}*/
	
	public int getLeftSpanLength() {
		return leftSpanLength;
	}

	// for cases like "in the same year"
	// for cases like "two years later/before"
	// for cases like "On 17 May" - where the year is under-specified

	private static CorefWord.Visitor<String> opSymbolGetter = new CorefWord.Visitor<String>() {
		@Override public String visitSame() 		{return null;}
		@Override public String visitPrevious() {return "-";}
		@Override public String visitBefore() 	{return "-";}						
		@Override public String visitNext() 		{return "+";}
		@Override public String visitLater() 		{return "+";}
	};
	
	private static Format.DurationVisitor<Timex.Type> dateTimeGetter = new Format.DurationVisitor<Timex.Type>() {
		@Override	public Timex.Type visitYears() 		{return Timex.Type.DATE;}
		@Override public Timex.Type visitMonths() 	{return Timex.Type.DATE;}
		@Override public Timex.Type visitDays() 		{return Timex.Type.DATE;}
		@Override public Timex.Type visitHours() 		{return Timex.Type.TIME;}
		@Override public Timex.Type visitMinutes() 	{return Timex.Type.TIME;}
		@Override public Timex.Type visitSeconds() 	{return Timex.Type.TIME;}
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
