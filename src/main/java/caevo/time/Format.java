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
	
	/*
	DATEHOUR() {		// 2002-07-10T11
		public <A> A accept(Visitor<A> visitor) {return visitor.visitDateHour();}
	},

	DATEHOURMINUTE() {		// 2002-07-10T11:23
		public <A> A accept(Visitor<A> visitor) {return visitor.visitDateHourMinute();}
	},*/
	
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
	};


	public abstract <A> A accept(Visitor<A> visitor);
	
	public interface Visitor<A> {
		A visitYears();
		A visitMonths();
		A visitDays();
		A visitHours();
		A visitMinutes();
		A visitSeconds();
		A visitDateTime();
		A visitDate();
		A visitTime();
		A visitYearMonth();
		A visitMonthDay();
		A visitYear();
		A visitMonth();
		A visitHourMinute();
		A visitHour();
	}

	
	private static Set<Format> duratives = new TreeSet<Format>(Arrays.asList(
			YEARS,MONTHS,DAYS,HOURS,MINUTES,SECONDS));

	private static Set<Format> timePoints = new TreeSet<Format>(Arrays.asList(
			DATETIME,DATE,TIME,YEARMONTH,MONTHDAY,YEAR,MONTH,HOURMINUTE,HOUR));

	private static Set<Format> arithmeticable = new TreeSet<Format>(Arrays.asList(
			DATETIME,DATE,TIME,YEARMONTH,YEAR));

	private static Set<Format> times = new TreeSet<Format>(Arrays.asList(
			TIME,HOURMINUTE,HOUR));
	
	private static Map<Format,Set<TimeUnit>> timePointUnits;
	private static Map<Set<TimeUnit>,Format> timeUnitsPoint;
	
	static {
		timePointUnits = new HashMap<Format, Set<TimeUnit>>();
		timePointUnits.put(DATETIME, new TreeSet<TimeUnit>(Arrays.asList(
				TimeUnit.YEAR,TimeUnit.MONTH,TimeUnit.DAY,TimeUnit.HOUR,TimeUnit.MINUTE,TimeUnit.SECOND)));	
		timePointUnits.put(DATE, new TreeSet<TimeUnit>(Arrays.asList(TimeUnit.YEAR,TimeUnit.MONTH,TimeUnit.DAY)));	
		timePointUnits.put(YEARMONTH, new TreeSet<TimeUnit>(Arrays.asList(TimeUnit.YEAR,TimeUnit.MONTH)));
		timePointUnits.put(YEAR, new TreeSet<TimeUnit>(Arrays.asList(TimeUnit.YEAR)));
		timePointUnits.put(MONTHDAY, new TreeSet<TimeUnit>(Arrays.asList(TimeUnit.MONTH,TimeUnit.DAY)));
		timePointUnits.put(MONTH, new TreeSet<TimeUnit>(Arrays.asList(TimeUnit.MONTH)));
		timePointUnits.put(TIME, new TreeSet<TimeUnit>(Arrays.asList(TimeUnit.HOUR,TimeUnit.MINUTE,TimeUnit.SECOND)));
		timePointUnits.put(HOURMINUTE, new TreeSet<TimeUnit>(Arrays.asList(TimeUnit.HOUR,TimeUnit.MINUTE)));
		timePointUnits.put(HOUR, new TreeSet<TimeUnit>(Arrays.asList(TimeUnit.HOUR)));

		timeUnitsPoint = new HashMap<Set<TimeUnit>,Format>();
		for (Entry<Format, Set<TimeUnit>> entry : timePointUnits.entrySet()) {
			timeUnitsPoint.put(entry.getValue(), entry.getKey());
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
			@Override public TimeUnit visitDays() 		{return TimeUnit.DAY;}
			@Override public TimeUnit visitHours() 		{return TimeUnit.HOUR;}
			@Override public TimeUnit visitMinutes() 	{return TimeUnit.MINUTE;}
			@Override public TimeUnit visitSeconds() 	{return TimeUnit.SECOND;}
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
	
	public boolean has(TimeUnit unit) {
		return unit.accept(new TimeUnit.Visitor<Boolean>() {
			@Override public Boolean visitYear() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return true;}
					@Override public Boolean visitYear() 			{return true;}
					@Override public Boolean visitYearMonth() {return true;}
					@Override public Boolean visitMonthDay() 	{return false;}
					@Override public Boolean visitMonth()			{return false;}
					@Override public Boolean visitTime() 			{return false;}
					@Override public Boolean visitHour() 			{return false;}
					@Override public Boolean visitHourMinute(){return false;}
				});}
			@Override public Boolean visitMonth() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return true;}
					@Override public Boolean visitYear() 			{return false;}
					@Override public Boolean visitYearMonth() {return true;}
					@Override public Boolean visitMonthDay() 	{return true;}
					@Override public Boolean visitMonth() 		{return true;}
					@Override public Boolean visitTime() 			{return false;}
					@Override public Boolean visitHour() 			{return false;}
					@Override public Boolean visitHourMinute(){return false;}
				});}
			@Override public Boolean visitDay() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return true;}
					@Override public Boolean visitYear() 			{return false;}
					@Override public Boolean visitYearMonth() {return false;}
					@Override public Boolean visitMonthDay() 	{return true;}
					@Override public Boolean visitMonth() 		{return false;}
					@Override public Boolean visitTime() 			{return false;}
					@Override public Boolean visitHour() 			{return false;}
					@Override public Boolean visitHourMinute(){return false;}
				});}
			@Override public Boolean visitHour() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return false;}
					@Override public Boolean visitYear() 			{return false;}
					@Override public Boolean visitYearMonth() {return false;}
					@Override public Boolean visitMonthDay() 	{return false;}
					@Override public Boolean visitMonth() 		{return false;}
					@Override public Boolean visitTime() 			{return true;}
					@Override public Boolean visitHour() 			{return true;}
					@Override public Boolean visitHourMinute(){return true;}
				});}
			@Override public Boolean visitMinute() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return false;}
					@Override public Boolean visitYear() 			{return false;}
					@Override public Boolean visitYearMonth() {return false;}
					@Override public Boolean visitMonthDay() 	{return false;}
					@Override public Boolean visitMonth() 		{return false;}
					@Override public Boolean visitTime() 			{return true;}
					@Override public Boolean visitHour() 			{return false;}
					@Override public Boolean visitHourMinute(){return true;}
				});}
			@Override public Boolean visitSecond() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return false;}
					@Override public Boolean visitYear() 			{return false;}
					@Override public Boolean visitYearMonth() {return false;}
					@Override public Boolean visitMonthDay() 	{return false;}
					@Override public Boolean visitMonth() 		{return false;}
					@Override public Boolean visitTime() 			{return true;}
					@Override public Boolean visitHour() 			{return false;}
					@Override public Boolean visitHourMinute(){return false;}
				});}
		});
	}

	public abstract static class ArithmeticableVisitor<A> implements Visitor<A> {
		@Override public A visitYears() 		{return null;}
		@Override public A visitMonths() 		{return null;}
		@Override public A visitDays() 			{return null;}
		@Override public A visitHours() 		{return null;}
		@Override public A visitMinutes() 	{return null;}
		@Override public A visitSeconds() 	{return null;}
		@Override public A visitMonthDay()  {return null;}
		@Override public A visitMonth()			{return null;}
		@Override public A visitHourMinute(){return null;}
		@Override public A visitHour() 			{return null;}
	}
	
	public abstract static class TimePointVisitor<A> implements Visitor<A> {
		@Override public A visitYears() 		{return null;}
		@Override public A visitMonths() 		{return null;}
		@Override public A visitDays() 			{return null;}
		@Override public A visitHours() 		{return null;}
		@Override public A visitMinutes() 	{return null;}
		@Override public A visitSeconds() 	{return null;}
	}
	
	public abstract static class DurationVisitor<A> implements Visitor<A> {
		@Override public A visitDateTime()	{return null;}
		@Override public A visitDate() 			{return null;}
		@Override public A visitTime() 			{return null;}
		@Override public A visitYearMonth() {return null;}
		@Override public A visitMonthDay()  {return null;}
		@Override public A visitYear() 			{return null;}
		@Override public A visitMonth()			{return null;}
		@Override public A visitHourMinute(){return null;}
		@Override public A visitHour() 			{return null;}
	}

}

/*
  		final Format thisFormat = this;
		return format.accept(new TimePointVisitor<Boolean>() {

			@Override public Boolean visitDateTime() {return true;}

			@Override public Boolean visitDate() {
				return thisFormat.accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime() {return true;}
					@Override public Boolean visitDate() {return true;}
					@Override public Boolean visitTime() {return true;}
					@Override public Boolean visitYearMonth() {return true;}
					@Override public Boolean visitMonthDay() {return true;}
					@Override public Boolean visitYear() {return true;}
					@Override public Boolean visitMonth() { return true;}
					@Override public Boolean visitHourMinute() {return true;}
					@Override public Boolean visitHour() {return true;}
				});
			}

			@Override public Boolean visitTime() {
				return thisFormat.accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime() {return true;}
					@Override public Boolean visitDate() {return true;}
					@Override public Boolean visitTime() {return true;}
					@Override public Boolean visitHourMinute() {return true;}
					@Override public Boolean visitHour() {return true;}
					@Override public Boolean visitYearMonth() {return false;}
					@Override public Boolean visitMonthDay() {return false;}
					@Override public Boolean visitYear() {return false;}
					@Override public Boolean visitMonth() { return false;}
				});
			}

			@Override
			public Boolean visitYearMonth() {
				return thisFormat.accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime() {return true;}
					@Override public Boolean visitDate() {return true;}
					@Override public Boolean visitTime() {return false;}
					@Override public Boolean visitHourMinute() {return false;}
					@Override public Boolean visitHour() {return false;}
					@Override public Boolean visitYearMonth() {return true;}
					@Override public Boolean visitMonthDay() {return true;}
					@Override public Boolean visitYear() {return true;}
					@Override public Boolean visitMonth() { return true;}
				});
			}

			@Override
			public Boolean visitMonthDay() {
				return null;
			}

			@Override
			public Boolean visitYear() {
				return null;
			}

			@Override
			public Boolean visitMonth() {
				return null;
			}

			@Override
			public Boolean visitHourMinute() {
				return null;
			}

			@Override
			public Boolean visitHour() {
				return null;
			}
		});

*/ 
