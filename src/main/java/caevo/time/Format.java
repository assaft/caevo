package caevo.time;

import java.util.Arrays;
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
	
	DATE() {				// 2002-07-10
		public <A> A accept(Visitor<A> visitor) {return visitor.visitDate();}	
	},
	
	TIME() {				// T13:20:11
		public <A> A accept(Visitor<A> visitor) {return visitor.visitTime();}	
	},
	
	YEARMONTH() {	// 2002-07
		public <A> A accept(Visitor<A> visitor) {return visitor.visitYearMonth();}	
	},
	
	YEAR() {				// 2002
		public <A> A accept(Visitor<A> visitor) {return visitor.visitYear();}	
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
		A visitYear();
		A visitHourMinute();
		A visitHour();
	}

	
	private static Set<Format> duratives = new TreeSet<Format>(Arrays.asList(
			YEARS,MONTHS,DAYS,HOURS,MINUTES,SECONDS));

	private static Set<Format> timePoints = new TreeSet<Format>(Arrays.asList(
			DATETIME,DATE,TIME,YEARMONTH,YEAR,HOURMINUTE,HOUR));
	
	public boolean isDurative() {
		return duratives.contains(this);
	}

	public boolean isAbsolute() {
		return timePoints.contains(this);
	}
	
	public boolean has(TimeUnit unit) {
		return unit.accept(new TimeUnit.Visitor<Boolean>() {
			@Override public Boolean visitYear() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return true;}
					@Override public Boolean visitTime() 			{return false;}
					@Override public Boolean visitYearMonth() {return true;}
					@Override public Boolean visitYear() 			{return true;}
					@Override public Boolean visitHourMinute(){return false;}
					@Override public Boolean visitHour() 			{return false;}
				});}
			@Override public Boolean visitMonth() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return true;}
					@Override public Boolean visitTime() 			{return false;}
					@Override public Boolean visitYearMonth() {return true;}
					@Override public Boolean visitYear() 			{return false;}
					@Override public Boolean visitHourMinute(){return false;}
					@Override public Boolean visitHour() 			{return false;}
				});}
			@Override public Boolean visitDay() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return true;}
					@Override public Boolean visitTime() 			{return false;}
					@Override public Boolean visitYearMonth() {return false;}
					@Override public Boolean visitYear() 			{return false;}
					@Override public Boolean visitHourMinute(){return false;}
					@Override public Boolean visitHour() 			{return false;}
				});}
			@Override public Boolean visitHour() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return false;}
					@Override public Boolean visitTime() 			{return false;}
					@Override public Boolean visitYearMonth() {return false;}
					@Override public Boolean visitYear() 			{return false;}
					@Override public Boolean visitHourMinute(){return true;}
					@Override public Boolean visitHour() 			{return true;}
				});}
			@Override public Boolean visitMinute() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return false;}
					@Override public Boolean visitTime() 			{return false;}
					@Override public Boolean visitYearMonth() {return false;}
					@Override public Boolean visitYear() 			{return false;}
					@Override public Boolean visitHourMinute(){return true;}
					@Override public Boolean visitHour() 			{return false;}
				});}
			@Override public Boolean visitSecond() {
				return accept(new TimePointVisitor<Boolean>() {
					@Override public Boolean visitDateTime()	{return true;}
					@Override public Boolean visitDate() 			{return false;}
					@Override public Boolean visitTime() 			{return false;}
					@Override public Boolean visitYearMonth() {return false;}
					@Override public Boolean visitYear() 			{return false;}
					@Override public Boolean visitHourMinute(){return false;}
					@Override public Boolean visitHour() 			{return false;}
				});}
		});
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
		@Override public A visitYear() 			{return null;}
		@Override public A visitHourMinute(){return null;}
		@Override public A visitHour() 			{return null;}
	}
	
}
