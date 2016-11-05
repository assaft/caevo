package caevo.time;

public class DurationData implements TimeData {
	private final int quantity;
	private final Format format;
	private final TimeUnit unit;

	private DurationData(int quantity, Format format, TimeUnit unit) {
		super();
		this.quantity = quantity;
		this.format = format;
		this.unit = unit;
	}

	public DurationData(int quantity, Format format) {
		this(quantity,format,Format.toTimeUnit(format));
	}
	
	public DurationData(int quantity, TimeUnit unit) {
		this(quantity,Format.fromTimeUnit(unit),unit);
	}
	
	public int getQuantity() {
		return quantity;
	}

	public TimeUnit getTimeUnit() {
		return unit;
	}
	
	@Override
	public Format getFormat() {
		return format;
	}
	
	public String toString() {
		return quantity + " " + format.toString();
	}
}
