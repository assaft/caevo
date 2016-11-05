package caevo.time;

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
