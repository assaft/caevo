package caevo.time;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

public enum Format {
	YEARS() {			// P2Y	= 2 years
		public <A> A accept(Visitor<A> visitor) {return visitor.visitYears();}	
	},
	
	MONTHS() {			// P4M	= 4 months
		public <A> A accept(Visitor<A> visitor) {return visitor.visitMonths();}	
	},

	WEEKS() {			// P2W	= 2 weeks
		public <A> A accept(Visitor<A> visitor) {return visitor.visitWeeks();}	
	},
	
	DAYS() {				// P3D  = 3 days
		public <A> A accept(Visitor<A> visitor) {return visitor.visitDays();}	
	},
	
	HOURS() {			// PT7H = 7 hours
		public <A> A accept(Visitor<A> visitor) {return visitor.visitHours();}	
	},
	
	MINUTES() {		// PT4M = 4 minutes
		public <A> A accept(Visitor<A> visitor) {return visitor.visitMinutes();}	
	},
	
	SECONDS() {		// PT8S	= 8 seconds
		public <A> A accept(Visitor<A> visitor) {return visitor.visitSeconds();}	
	},
	
	DATETIME() {	// 2002-07-10T13:20:11
		public <A> A accept(Visitor<A> visitor) {return visitor.visitDateTime();}	
	},
	
	
	DATEHOURMINUTE() {		// 2002-07-10T11:23
		public <A> A accept(Visitor<A> visitor) {return visitor.visitDateHourMinute();}
	},
	

	DATEHOUR() {		// 2002-07-10T11
		public <A> A accept(Visitor<A> visitor) {return visitor.visitDateHour();}
	},

	DATE() {				// 2002-07-10
		public <A> A accept(Visitor<A> visitor) {return visitor.visitDate();}	
	},
	
	
	TIME() {				// T13:20:11
		public <A> A accept(Visitor<A> visitor) {return visitor.visitTime();}	
	},
	
	YEARMONTH() {	// 2002-07
		public <A> A accept(Visitor<A> visitor) {return visitor.visitYearMonth();}	
	},
	
	MONTHDAY() { // XXXX-07-12 (SUTime creates this for: On 12 July,...)
		public <A> A accept(Visitor<A> visitor) {return visitor.visitMonthDay();}	
	},
	
	YEAR() {				// 2002
		public <A> A accept(Visitor<A> visitor) {return visitor.visitYear();}	
	},

	MONTH() {				// XXXX-07 (SUTime creates this for: On July,...)
		public <A> A accept(Visitor<A> visitor) {return visitor.visitMonth();}	
	},
	
	HOURMINUTE() {	// T13:20
		public <A> A accept(Visitor<A> visitor) {return visitor.visitHourMinute();}	
	},
	
	HOUR() {				// T13
		public <A> A accept(Visitor<A> visitor) {return visitor.visitHour();}
	},

	UNSUPPORTED() {  // can be anything, days (XXXX-WXX-1), weird times like 'the afternoon' (2016-11-07TAF) 
		public <A> A accept(Visitor<A> visitor) {return visitor.visitUnsupported();}
	};

	public abstract <A> A accept(Visitor<A> visitor);
	
	public interface Visitor<A> {
		A visitYears();
		A visitWeeks();
		A visitMonths();
		A visitDays();
		A visitHours();
		A visitMinutes();
		A visitSeconds();
		A visitDateTime();
		A visitDateHour();
		A visitDateHourMinute();
		A visitDate();
		A visitTime();
		A visitYearMonth();
		A visitMonthDay();
		A visitYear();
		A visitMonth();
		A visitHourMinute();
		A visitHour();
		A visitUnsupported();
	}

	private static final Set<Format> duratives;
	private static final Set<Format> timePoints;
	private static final Set<Format> arithmeticable;
	private static final Set<Format> times;
	
	private static final Map<Format,Set<TimeUnit>> timePointUnits;
	private static final Map<Set<TimeUnit>,Format> timeUnitsPoint;
	
	static {
		
		duratives = new TreeSet<Format>();
		for (Format format : values()) {
			Boolean isDurative = format.accept(new DurationVisitor<Boolean>() {
				@Override public Boolean visitYears() 		{return new Boolean(true);}
				@Override public Boolean visitMonths() 		{return new Boolean(true);}
				@Override public Boolean visitWeeks() 		{return new Boolean(true);}
				@Override public Boolean visitDays() 			{return new Boolean(true);}
				@Override public Boolean visitHours() 		{return new Boolean(true);}
				@Override public Boolean visitMinutes() 	{return new Boolean(true);}
				@Override public Boolean visitSeconds() 	{return new Boolean(true);}
			});
			if (isDurative!=null && isDurative.booleanValue()) {
				duratives.add(format);
			}
		}
		
		timePoints = new TreeSet<Format>();
		for (Format format : values()) {
			Boolean isTimePoint = format.accept(new TimePointVisitor<Boolean>() {
				@Override public Boolean visitDateTime()			{return new Boolean(true);}
				@Override public Boolean visitDateHourMinute(){return new Boolean(true);}
				@Override public Boolean visitDateHour() 			{return new Boolean(true);}
				@Override public Boolean visitDate() 					{return new Boolean(true);}
				@Override public Boolean visitTime() 					{return new Boolean(true);}
				@Override public Boolean visitYearMonth() 		{return new Boolean(true);}
				@Override public Boolean visitMonthDay()  		{return new Boolean(true);}
				@Override public Boolean visitYear() 					{return new Boolean(true);}
				@Override public Boolean visitMonth()					{return new Boolean(true);}
				@Override public Boolean visitHourMinute()		{return new Boolean(true);}
				@Override public Boolean visitHour() 					{return new Boolean(true);}
			});
			if (isTimePoint!=null && isTimePoint.booleanValue()) {
				timePoints.add(format);
			}
		}
		
		arithmeticable = new TreeSet<Format>();
		for (Format format : values()) {
			Boolean isArithmeticable = format.accept(new ArithmeticableVisitor<Boolean>() {
				@Override public Boolean visitDateTime() 			{return new Boolean(true);}
				@Override public Boolean visitDateHourMinute(){return new Boolean(true);}
				@Override public Boolean visitDateHour() 			{return new Boolean(true);}
				@Override public Boolean visitDate() 					{return new Boolean(true);}
				@Override public Boolean visitYearMonth() 		{return new Boolean(true);}
				@Override public Boolean visitYear() 					{return new Boolean(true);}
				@Override public Boolean visitTime() 					{return new Boolean(true);}
			});
			if (isArithmeticable!=null && isArithmeticable.booleanValue()) {
				arithmeticable.add(format);
			}
		}

		times = new TreeSet<Format>();
		for (Format format : values()) {
			Boolean isTime = format.accept(new TimeVisitor<Boolean>() {
				@Override public Boolean visitTime() 				{return new Boolean(true);}
				@Override public Boolean visitHourMinute() 	{return new Boolean(true);}
				@Override public Boolean visitHour() 				{return new Boolean(true);}
			});
			if (isTime!=null && isTime.booleanValue()) {
				times.add(format);
			}
		}
		
		timePointUnits = new HashMap<Format, Set<TimeUnit>>();
		for (final Format format : values()) {
			Set<TimeUnit> timeUnits = new TreeSet<TimeUnit>();
			timePointUnits.put(format, timeUnits);
			for (final TimeUnit unit : TimeUnit.values()) {
				Boolean formatHasUnit = unit.accept(new TimeUnit.Visitor<Boolean>() {
					@Override public Boolean visitYear() {
						return format.accept(new TimePointVisitor<Boolean>() {
							@Override public Boolean visitDateTime()			{return true;}
							@Override public Boolean visitDateHourMinute(){return true;}
							@Override public Boolean visitDateHour() 			{return true;}
							@Override public Boolean visitDate() 					{return true;}
							@Override public Boolean visitYear() 					{return true;}
							@Override public Boolean visitYearMonth() 		{return true;}
							@Override public Boolean visitMonthDay() 			{return false;}
							@Override public Boolean visitMonth()					{return false;}
							@Override public Boolean visitTime() 					{return false;}
							@Override public Boolean visitHour() 					{return false;}
							@Override public Boolean visitHourMinute()		{return false;}
						});}
					@Override public Boolean visitMonth() {
						return format.accept(new TimePointVisitor<Boolean>() {
							@Override public Boolean visitDateTime()			{return true;}
							@Override public Boolean visitDateHourMinute(){return true;}
							@Override public Boolean visitDateHour() 			{return true;}
							@Override public Boolean visitDate() 					{return true;}
							@Override public Boolean visitYear() 					{return false;}
							@Override public Boolean visitYearMonth() 		{return true;}
							@Override public Boolean visitMonthDay() 			{return true;}
							@Override public Boolean visitMonth() 				{return true;}
							@Override public Boolean visitTime() 					{return false;}
							@Override public Boolean visitHour() 					{return false;}
							@Override public Boolean visitHourMinute()		{return false;}
						});}
					@Override public Boolean visitDay() {
						return format.accept(new TimePointVisitor<Boolean>() {
							@Override public Boolean visitDateTime()			{return true;}
							@Override public Boolean visitDateHourMinute(){return true;}
							@Override public Boolean visitDateHour() 			{return true;}
							@Override public Boolean visitDate() 					{return true;}
							@Override public Boolean visitYear() 					{return false;}
							@Override public Boolean visitYearMonth() 		{return false;}
							@Override public Boolean visitMonthDay() 			{return true;}
							@Override public Boolean visitMonth() 				{return false;}
							@Override public Boolean visitTime() 					{return false;}
							@Override public Boolean visitHour() 					{return false;}
							@Override public Boolean visitHourMinute()		{return false;}
						});}
					@Override public Boolean visitHour() {
						return format.accept(new TimePointVisitor<Boolean>() {
							@Override public Boolean visitDateTime()			{return true;}
							@Override public Boolean visitDateHourMinute(){return true;}
							@Override public Boolean visitDateHour() 			{return true;}
							@Override public Boolean visitDate() 					{return false;}
							@Override public Boolean visitYear() 					{return false;}
							@Override public Boolean visitYearMonth() 		{return false;}
							@Override public Boolean visitMonthDay() 			{return false;}
							@Override public Boolean visitMonth() 				{return false;}
							@Override public Boolean visitTime() 					{return true;}
							@Override public Boolean visitHour() 					{return true;}
							@Override public Boolean visitHourMinute()		{return true;}
						});}
					@Override public Boolean visitMinute() {
						return format.accept(new TimePointVisitor<Boolean>() {
							@Override public Boolean visitDateTime()			{return true;}
							@Override public Boolean visitDateHourMinute(){return true;}
							@Override public Boolean visitDateHour() 			{return false;}
							@Override public Boolean visitDate() 					{return false;}
							@Override public Boolean visitYear() 					{return false;}
							@Override public Boolean visitYearMonth() 		{return false;}
							@Override public Boolean visitMonthDay() 			{return false;}
							@Override public Boolean visitMonth() 				{return false;}
							@Override public Boolean visitTime() 					{return true;}
							@Override public Boolean visitHour() 					{return false;}
							@Override public Boolean visitHourMinute()		{return true;}
						});}
					@Override public Boolean visitSecond() {
						return format.accept(new TimePointVisitor<Boolean>() {
							@Override public Boolean visitDateTime()			{return true;}
							@Override public Boolean visitDateHourMinute(){return false;}
							@Override public Boolean visitDateHour() 			{return false;}
							@Override public Boolean visitDate() 					{return false;}
							@Override public Boolean visitYear() 					{return false;}
							@Override public Boolean visitYearMonth() 		{return false;}
							@Override public Boolean visitMonthDay() 			{return false;}
							@Override public Boolean visitMonth() 				{return false;}
							@Override public Boolean visitTime() 					{return true;}
							@Override public Boolean visitHour() 					{return false;}
							@Override public Boolean visitHourMinute		(){return false;}
						});}
				});
				if (formatHasUnit!=null && formatHasUnit) {
					timeUnits.add(unit);					
				}
			}			
		}		

		timeUnitsPoint = new HashMap<Set<TimeUnit>,Format>();
		for (Entry<Format, Set<TimeUnit>> entry : timePointUnits.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				timeUnitsPoint.put(entry.getValue(), entry.getKey());
			}
		}
		
	}
	
	public boolean isDurative() {
		return duratives.contains(this);
	}

	public boolean isTimePoint() {
		return timePoints.contains(this);
	}

	public boolean isArithmeticable() {
		return arithmeticable.contains(this);
	}
	
	public Set<TimeUnit> getTimeUnits() {
		return timePointUnits.get(this);
	}
	
	public boolean has(TimeUnit unit) {
		return timePointUnits.get(this).contains(unit);
	}
	
	public boolean isMergableTo(Format format) {
		BitSet bitset = new BitSet(TimeUnit.values().length);
		
		Set<TimeUnit> targetTimeUnits = timePointUnits.get(format);
		for (TimeUnit unit : targetTimeUnits) {
			bitset.set(unit.ordinal());
		}

		Set<TimeUnit> currentTimeUnits = timePointUnits.get(this);
		for (TimeUnit unit : currentTimeUnits) {
			bitset.set(unit.ordinal());
		}
		
		int firstSetBit = bitset.nextSetBit(0);
		int nextClearBit = bitset.nextClearBit(firstSetBit);
		int length = nextClearBit-firstSetBit+1;
		boolean singleSection = length==bitset.cardinality(); 

		boolean result = false;
		if (singleSection) {
			Set<TimeUnit> timeUnits = new TreeSet<TimeUnit>();
			for (int i=0 ; i<length ; i++) {
				timeUnits.add(TimeUnit.values()[i+firstSetBit]);
			}
			result = exists(timeUnits);
		}
		return result;
	}	

	public boolean hasDate() {
		return !times.contains(this);
	}
	
	public static boolean exists(Set<TimeUnit> timeUnits) {
		return timeUnitsPoint.containsKey(timeUnits);
	}
	
	public static Format of(Set<TimeUnit> timeUnits) {
		return timeUnitsPoint.get(timeUnits);
	}
	
	public static TimeUnit toTimeUnit(Format format) {
		return format.accept(new DurationVisitor<TimeUnit>() {
			@Override	public TimeUnit visitYears() 		{return TimeUnit.YEAR;}
			@Override public TimeUnit visitMonths() 	{return TimeUnit.MONTH;}
			@Override public TimeUnit visitWeeks() 		{return TimeUnit.DAY;}
			@Override public TimeUnit visitDays() 		{return TimeUnit.DAY;}
			@Override public TimeUnit visitHours() 		{return TimeUnit.HOUR;}
			@Override public TimeUnit visitMinutes() 	{return TimeUnit.MINUTE;}
			@Override public TimeUnit visitSeconds() 	{return TimeUnit.SECOND;}
		});
	}

	public static Integer toTimeUnitFactor(Format format) {
		return format.accept(new DurationVisitor<Integer>() {
			@Override	public Integer visitYears() 	{return 1;}
			@Override public Integer visitMonths() 	{return 1;}
			@Override public Integer visitWeeks() 	{return 7;}
			@Override public Integer visitDays() 		{return 1;}
			@Override public Integer visitHours() 	{return 1;}
			@Override public Integer visitMinutes() {return 1;}
			@Override public Integer visitSeconds() {return 1;}
		});
	}
	
	public static Format fromTimeUnit(TimeUnit unit) {
		return unit.accept(new TimeUnit.Visitor<Format>() {
			@Override	public Format visitYear() 		{return Format.YEARS;}
			@Override public Format visitMonth() 		{return Format.MONTHS;}
			@Override public Format visitDay() 			{return Format.DAYS;}
			@Override public Format visitHour() 		{return Format.HOURS;}
			@Override public Format visitMinute() 	{return Format.MINUTES;}
			@Override public Format visitSecond() 	{return Format.SECONDS;}
		});
	}
	
	public abstract static class ArithmeticableVisitor<A> implements Visitor<A> {
		@Override public A visitYears() 			{return null;}
		@Override public A visitMonths() 			{return null;}
		@Override public A visitWeeks() 			{return null;}
		@Override public A visitDays() 				{return null;}
		@Override public A visitHours() 			{return null;}
		@Override public A visitMinutes() 		{return null;}
		@Override public A visitSeconds() 		{return null;}
		@Override public A visitMonthDay()  	{return null;}
		@Override public A visitMonth()				{return null;}
		@Override public A visitHourMinute()	{return null;}
		@Override public A visitHour() 				{return null;}
		@Override public A visitUnsupported() {return null;}
	}
	
	public abstract static class TimePointVisitor<A> implements Visitor<A> {
		@Override public A visitYears() 			{return null;}
		@Override public A visitMonths() 			{return null;}
		@Override public A visitWeeks() 			{return null;}
		@Override public A visitDays() 				{return null;}
		@Override public A visitHours() 			{return null;}
		@Override public A visitMinutes() 		{return null;}
		@Override public A visitSeconds() 		{return null;}
		@Override public A visitUnsupported()	{return null;}
	}
	
	public abstract static class DurationVisitor<A> implements Visitor<A> {
		@Override public A visitDateTime()			{return null;}
		@Override public A visitDateHourMinute(){return null;}
		@Override public A visitDateHour() 			{return null;}
		@Override public A visitDate() 					{return null;}
		@Override public A visitTime() 					{return null;}
		@Override public A visitYearMonth() 		{return null;}
		@Override public A visitMonthDay()  		{return null;}
		@Override public A visitYear() 					{return null;}
		@Override public A visitMonth()					{return null;}
		@Override public A visitHourMinute()		{return null;}
		@Override public A visitHour() 					{return null;}
		@Override public A visitUnsupported() 	{return null;}
	}

	public abstract static class TimeVisitor<A> extends TimePointVisitor<A> {
		@Override public A visitDateTime() 			{return null;}
		@Override public A visitDateHourMinute(){return null;}
		@Override public A visitDateHour() 			{return null;}
		@Override public A visitDate() 					{return null;}
		@Override public A visitYearMonth() 		{return null;}
		@Override public A visitYear() 					{return null;}
		@Override public A visitMonthDay() 			{return null;}
		@Override public A visitMonth() 				{return null;}
	}
}

