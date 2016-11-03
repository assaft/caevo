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

}
