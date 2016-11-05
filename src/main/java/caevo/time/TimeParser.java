package caevo.time;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {

	private static final String DATETIME_REGEX_STR = "^(?:(\\d{4}|XXXX)(?:\\-?(\\d{2}))?(?:\\-?(\\d{2}))?|(?:(T?\\d{2})(?:\\:?(\\d{2}))?(?:\\:?(\\d{2}))?))+$";
	private static final Pattern DATETIME_REGEX = Pattern.compile(DATETIME_REGEX_STR);

	private static final Pattern periodPattern = Pattern.compile("P(\\d+)([YMD])");
	private static final Pattern durationPattern = Pattern.compile("PT(\\d+)([HMS])");

	public TimeData parse(String s) {

		TimeData ret = null;
		Format format = null;

		Matcher matcher = DATETIME_REGEX.matcher(s);
		if (matcher.find()) {
			Map<TimeUnit,Integer> values = new HashMap<TimeUnit, Integer>();
			for (int i=0, length = TimeUnit.values().length ; i<length ; i++) {
				int gId = i+1;
				if (matcher.group(gId)!=null) {
					if (!matcher.group(gId).equals("XXXX")) {
						TimeUnit timeUnit = TimeUnit.values()[i]; 
						String value = matcher.group(gId);
						if (timeUnit==TimeUnit.HOUR) {
							value = value.substring(1);
						}
						values.put(timeUnit,Integer.parseInt(value));
					}
				}
			}
			if (Format.exists(values.keySet())) {
				ret = new DateTimeData(values);
			}
		} else {
			int quantity;
			matcher = periodPattern.matcher(s);
			if (matcher.matches()) {
				if (matcher.group(2).equals("Y")) {
					format = Format.YEARS;
				} else if (matcher.group(2).equals("M")) {
					format = Format.MONTHS;
				} else if (matcher.group(2).equals("D")) {
					format = Format.DAYS;						
				}
				quantity = Integer.parseInt(matcher.group(1));
			} else {
				matcher = durationPattern.matcher(s);
				if (matcher.matches()) {
					if (matcher.group(2).equals("H")) {
						format = Format.HOURS;
					} else if (matcher.group(2).equals("M")) {
						format = Format.MINUTES;
					} else if (matcher.group(2).equals("S")) {
						format = Format.SECONDS;
					}
					quantity = Integer.parseInt(matcher.group(1));
				} else {
					throw new RuntimeException("Failed to parse: " + s);
				}
			}

			if (format!=null) {
				ret = new DurationData(quantity, format);
			}
		}
		
		return ret;
	}
	
}


