package caevo.time;

import java.util.HashMap;
import java.util.Map;

public class DateTimeData implements TimeData {

	private final Map<TimeUnit,Integer> values;
	private final Format format;
	
	public DateTimeData(int year, int month, int day, int hour,
			int minute, int second, Format format) {
		super();
		values = new HashMap<TimeUnit, Integer>();
		values.put(TimeUnit.YEAR, year);
		values.put(TimeUnit.MONTH, month);
		values.put(TimeUnit.DAY, day);
		values.put(TimeUnit.HOUR, hour);
		values.put(TimeUnit.MINUTE, minute);
		values.put(TimeUnit.SECOND, second);
		this.format = format;
	}
	
	public int get(TimeUnit unit) {
		return values.get(unit);
	}
	
	public Format getFormat() {
		return format;
	}
	
}
