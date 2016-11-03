package caevo.time;

public class DurationData implements TimeData {
	private final int quantity;
	private final Format format;

	public DurationData(int quantity, Format format) {
		super();
		this.quantity = quantity;
		this.format = format;
	}

	public int getQuantity() {
		return quantity;
	}

	@Override
	public Format getFormat() {
		return format;
	}
	
}
