package caevo.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import caevo.Timex;

public class DateTimeData implements TimeData {

	private final Map<TimeUnit,Integer> values;
	private final Format format;

	public DateTimeData(Map<TimeUnit,Integer> values) {
		this.values = new HashMap<TimeUnit,Integer>(values);
		this.format = Format.of(values.keySet());
	}

	public int get(TimeUnit unit) {
		return values.get(unit);
	}

	public Format getFormat() {
		return format;
	}

	public DateTimeData asDate() {
		Map<TimeUnit,Integer> newValues = new HashMap<TimeUnit, Integer>();
		for (TimeUnit timeUnit : TimeUnit.values()) {
			if (timeUnit.isDateUnit() && values.containsKey(timeUnit)) {
				newValues.put(timeUnit,values.get(timeUnit));
			}
		}
		return new DateTimeData(newValues);
	}

	public DateTimeData asTime() {
		Map<TimeUnit,Integer> newValues = new HashMap<TimeUnit, Integer>();
		for (TimeUnit timeUnit : TimeUnit.values()) {
			if (timeUnit.isTimeUnit() && values.containsKey(timeUnit)) {
				newValues.put(timeUnit,values.get(timeUnit));
			}
		}
		return new DateTimeData(newValues);
	}
	
	public DateTimeData apply(final TimeOperation operation, final DurationData duration) {
		return format.accept(new Format.ArithmeticableVisitor<DateTimeData>() {

			@Override
			public DateTimeData visitDateTime() {
				LocalDateTime dateTime = LocalDateTime.of(
						get(TimeUnit.YEAR), get(TimeUnit.MONTH), get(TimeUnit.DAY),
						get(TimeUnit.HOUR), get(TimeUnit.MINUTE), get(TimeUnit.SECOND));
				return calc(dateTime,duration,operation);
			}
			
			@Override
			public DateTimeData visitDateHourMinute() {
				LocalDateTime dateTime = LocalDateTime.of(
						get(TimeUnit.YEAR), get(TimeUnit.MONTH), get(TimeUnit.DAY),
						get(TimeUnit.HOUR), get(TimeUnit.MINUTE));
				return calc(dateTime,duration,operation);
			}			

			@Override
			public DateTimeData visitDateHour() {
				LocalDateTime dateTime = LocalDateTime.of(
						get(TimeUnit.YEAR), get(TimeUnit.MONTH), get(TimeUnit.DAY),
						get(TimeUnit.HOUR), 0);
				return calc(dateTime,duration,operation);
			}
			
			@Override
			public DateTimeData visitDate() {
				LocalDate date = LocalDate.of(get(TimeUnit.YEAR), get(TimeUnit.MONTH), get(TimeUnit.DAY));
				return calc(date,duration,operation);
			}

			@Override
			public DateTimeData visitTime() {
				LocalTime time = LocalTime.of(get(TimeUnit.HOUR), get(TimeUnit.MINUTE), get(TimeUnit.SECOND));
				return calc(time,duration,operation);
			}

			@Override
			public DateTimeData visitYearMonth() {
				YearMonth yearMonth = YearMonth.of(get(TimeUnit.YEAR), get(TimeUnit.MONTH));
				return calc(yearMonth,duration,operation);
			}

			@Override
			public DateTimeData visitYear() {
				Year year = Year.of(get(TimeUnit.YEAR));
				return calc(year,duration,operation);
			}

		});
	}

	private DateTimeData calc(Temporal temporal, DurationData durationData, TimeOperation operation) {
		int quantity = durationData.getQuantity();
		TemporalUnit unit = durationData.getFormat().accept(temporalUnitGetter);
		Temporal newTemporal = operation==TimeOperation.PLUS ? temporal.plus(quantity,unit) : temporal.minus(quantity,unit);
		Set<TimeUnit> timeUnits = format.getTimeUnits();
		Map<TimeUnit,Integer> newValues = new HashMap<TimeUnit, Integer>();
		for (TimeUnit timeUnit : timeUnits) {
			newValues.put(timeUnit,newTemporal.get(timeUnit.accept(temporalFieldGetter)));
		}
		return new DateTimeData(newValues); 
	}
	
	private static Format.DurationVisitor<TemporalUnit> temporalUnitGetter = new Format.DurationVisitor<TemporalUnit>() {
		@Override	public TemporalUnit visitYears() 		{return ChronoUnit.YEARS;}
		@Override public TemporalUnit visitMonths() 	{return ChronoUnit.MONTHS;}
		@Override public TemporalUnit visitDays() 		{return ChronoUnit.DAYS;}
		@Override public TemporalUnit visitWeeks() 		{return ChronoUnit.WEEKS;}
		@Override public TemporalUnit visitHours() 		{return ChronoUnit.HOURS;}
		@Override public TemporalUnit visitMinutes() 	{return ChronoUnit.MINUTES;}
		@Override public TemporalUnit visitSeconds() 	{return ChronoUnit.SECONDS;}
	};

	private static TimeUnit.Visitor<TemporalField> temporalFieldGetter = new TimeUnit.Visitor<TemporalField>() {
		@Override	public TemporalField visitYear() 		{return ChronoField.YEAR;}
		@Override public TemporalField visitMonth() 	{return ChronoField.MONTH_OF_YEAR;}
		@Override public TemporalField visitDay() 		{return ChronoField.DAY_OF_MONTH;}
		@Override public TemporalField visitHour() 		{return ChronoField.HOUR_OF_DAY;}
		@Override public TemporalField visitMinute() 	{return ChronoField.MINUTE_OF_HOUR;}
		@Override public TemporalField visitSecond() 	{return ChronoField.SECOND_OF_MINUTE;}
	};
	

	public DateTimeData merge(DateTimeData dateTimeData) {
		// find the resolution of the given data
		int resolution = TimeUnit.values().length-1;
		while (!dateTimeData.values.containsKey(TimeUnit.values()[resolution])) {
			resolution--;
		}

		// create a new data that is filled up to the resolution
		// give precedence to the given data over the the current
		Map<TimeUnit,Integer> newValues = new HashMap<TimeUnit, Integer>();
		for (int i=0 ; i<=resolution ; i++) {
			TimeUnit unit = TimeUnit.values()[i];
			Integer value = dateTimeData.values.get(unit);
			if (value==null) {
				value = values.get(unit);
			}
			newValues.put(unit,value);
		}
		return new DateTimeData(newValues);
	}

	private static class TimeUnitFormat {
		private final String leadingSymbol;
		private final int digits;
		
		public TimeUnitFormat(String leadingSymbol, int digits) {
			super();
			this.leadingSymbol = leadingSymbol;
			this.digits = digits;
		}

		public String getLeadingSymbol() {
			return leadingSymbol;
		}
		
		public int getDigits() {
			return digits;
		}
		
	}
	
	private static Map<TimeUnit,TimeUnitFormat> timeUnitsFormat;
	
	static {
		timeUnitsFormat = new HashMap<TimeUnit, DateTimeData.TimeUnitFormat>();
		timeUnitsFormat.put(TimeUnit.YEAR, new TimeUnitFormat("",4));
		timeUnitsFormat.put(TimeUnit.MONTH, new TimeUnitFormat("-",2));
		timeUnitsFormat.put(TimeUnit.DAY, new TimeUnitFormat("-",2));
		timeUnitsFormat.put(TimeUnit.HOUR, new TimeUnitFormat("T",2));
		timeUnitsFormat.put(TimeUnit.MINUTE, new TimeUnitFormat(":",2));
		timeUnitsFormat.put(TimeUnit.SECOND, new TimeUnitFormat(":",2));
	}
	
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		boolean stop=false;
		for (int i=0, length=TimeUnit.values().length ; i<length && !stop; i++) {
			TimeUnit unit = TimeUnit.values()[i];
			Integer value = values.get(unit);
			if (value!=null) {
				TimeUnitFormat format = timeUnitsFormat.get(unit);
				buffer.append(format.getLeadingSymbol());
				buffer.append(String.format("%0"+format.getDigits()+"d", values.get(unit)));
			} else {
				stop = buffer.length()>0;
			}
		}
		return buffer.toString();
	}

	public DateTimeData extract(TimeUnit maxUnit) {
		Map<TimeUnit,Integer> newValues = new HashMap<TimeUnit, Integer>();
		int length = maxUnit.ordinal()+1;
		for (int i=0 ; i<length ; i++) {
			TimeUnit unit = TimeUnit.values()[i];
			newValues.put(unit,values.get(unit));
		}
		return new DateTimeData(newValues);
	}

	public Timex.Type getTimexType() {
		return format.hasDate() ? Timex.Type.DATE : Timex.Type.TIME; 
	}
}
