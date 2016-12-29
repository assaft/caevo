package caevo.time;

import java.util.Set;
import java.util.TreeSet;

public enum TimeUnit {
	YEAR() 	{public <A> A accept(Visitor<A> visitor) {return visitor.visitYear();}},
	MONTH()	{public <A> A accept(Visitor<A> visitor) {return visitor.visitMonth();}},
	DAY()		{public <A> A accept(Visitor<A> visitor) {return visitor.visitDay();}},
	HOUR()	{public <A> A accept(Visitor<A> visitor) {return visitor.visitHour();}},
	MINUTE(){public <A> A accept(Visitor<A> visitor) {return visitor.visitMinute();}},
	SECOND(){public <A> A accept(Visitor<A> visitor) {return visitor.visitSecond();}};
	
	public abstract <A> A accept(Visitor<A> visitor);
	
	public interface Visitor<A> {
		A visitYear();
		A visitMonth();
		A visitDay();
		A visitHour();
		A visitMinute();
		A visitSecond();
	}

	private static final Set<TimeUnit> timeUnits;
	private static final Set<TimeUnit> dateUnits;

	private static final DateVisitor<Boolean> dateChecker;
	private static final TimeVisitor<Boolean> timeChecker;
	
	static {
		dateChecker = new DateVisitor<Boolean>() {
			@Override	public Boolean visitYear() 	{return true;}
			@Override public Boolean visitMonth() {return true;}
			@Override public Boolean visitDay() 	{return true;}
		};
		
		timeChecker = new TimeVisitor<Boolean>() {
			@Override	public Boolean visitHour() 	 {return true;}
			@Override public Boolean visitMinute() {return true;}
			@Override public Boolean visitSecond() {return true;}
		};
		
		timeUnits = new TreeSet<TimeUnit>();
		for (TimeUnit timeUnit : values()) {
			Boolean isTimeUnit = timeUnit.accept(timeChecker);
			if (isTimeUnit!=null && isTimeUnit) {
				timeUnits.add(timeUnit);
			}
		}
	
		dateUnits = new TreeSet<TimeUnit>();
		for (TimeUnit timeUnit : values()) {
			Boolean isDateUnit = timeUnit.accept(dateChecker);
			if (isDateUnit!=null && isDateUnit) {
				dateUnits.add(timeUnit);
			}
		}
		
	}
	
	public boolean isTimeUnit() {
		return timeUnits.contains(this);
	}

	public boolean isDateUnit() {
		return dateUnits.contains(this);
	}
	
	public abstract static class DateVisitor<A> implements Visitor<A>{
		@Override	public A visitHour() {return null;}
		@Override public A visitMinute() {return null;}
		@Override public A visitSecond() {return null;}
	}

	public abstract static class TimeVisitor<A> implements Visitor<A>{
		@Override	public A visitYear() {return null;}
		@Override public A visitMonth() {return null;}
		@Override public A visitDay() {return null;}
	}

	
}
