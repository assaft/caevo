package caevo;

public enum CorefWord {
	SAME() {
	   public <A> A accept(Visitor<A> visitor) {
	    return visitor.visitSame();
	  }
	},
	
	NEXT() {
	  public <A> A accept(Visitor<A> visitor) {
	    return visitor.visitNext();
	  }
	},
	
	PREVIOUS() {
		public <A> A accept(Visitor<A> visitor) {
	    return visitor.visitPrevious();
	  }
	},
	
	LATER() {
		public <A> A accept(Visitor<A> visitor) {
	    return visitor.visitLater();
	  }
	},
	
	BEFORE() {
		public <A> A accept(Visitor<A> visitor) {
	    return visitor.visitBefore();
	  }
	};
	
	public abstract <A> A accept(Visitor<A> visitor);
	
	public interface Visitor<A> {
		A visitSame();
		A visitNext();
		A visitPrevious();
		A visitLater();
		A visitBefore();
	}
	
	public static boolean isCorefWord(String candidate) {
		boolean ret = true;
		try {
			valueOf(candidate);
		} catch (Exception e) {
			ret = false;
		}
		return ret;
	}
}
