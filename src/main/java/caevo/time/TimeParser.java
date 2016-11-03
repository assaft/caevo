package caevo.time;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {

	private static final String DATETIME_REGEX_STR = "^(?:(\\d{4})(?:\\-?(\\d{2}))?(?:\\-?(\\d{2}))?|(?:(T?\\d{2})(?:\\:?(\\d{2}))?(?:\\:?(\\d{2}))?))+$";
	private static final Pattern DATETIME_REGEX = Pattern.compile(DATETIME_REGEX_STR);

	private static final Pattern periodPattern = Pattern.compile("P(\\d+)([YMD])");
	private static final Pattern durationPattern = Pattern.compile("PT(\\d+)([HMS])");

	public TimeData parse(String s) {

		TimeData ret = null;
		Format format = null;

		Matcher matcher = DATETIME_REGEX.matcher(s);
		if (matcher.find()) {
			int year = -1,  month = -1, day = -1;
			int hour = -1, minute = -1, second = -1;

			if (matcher.group(1)!=null && matcher.group(2)!=null && 
					matcher.group(3)!=null) {
				year = Integer.parseInt(matcher.group(1));
				month = Integer.parseInt(matcher.group(2));
				day = Integer.parseInt(matcher.group(3));
				format = Format.DATE;
			} else if (matcher.group(1)!=null && matcher.group(2)!=null) {
				year = Integer.parseInt(matcher.group(1));
				month = Integer.parseInt(matcher.group(2));
				format = Format.YEARMONTH;
			} else if (matcher.group(1)!=null) {
				year = Integer.parseInt(matcher.group(1));
				format = Format.YEAR;
			}

			if (matcher.group(4)!=null && matcher.group(5)!=null && 
					matcher.group(6)!=null) {
				hour = Integer.parseInt(matcher.group(4).substring(1));
				minute = Integer.parseInt(matcher.group(5));
				second = Integer.parseInt(matcher.group(6));
				format = format==Format.DATE ? Format.DATETIME : Format.TIME;
			} else if (matcher.group(4)!=null && matcher.group(5)!=null) {
				hour = Integer.parseInt(matcher.group(4).substring(1));
				minute = Integer.parseInt(matcher.group(5));
				format = Format.HOURMINUTE;
			} else if (matcher.group(4)!=null) {
				hour = Integer.parseInt(matcher.group(4).substring(1));
				format = Format.YEAR;
			} 

			if (format!=null) {
				ret = new DateTimeData(year, month, day, hour, minute, second, format);
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
				}
				quantity = Integer.parseInt(matcher.group(1));
			}

			if (format!=null) {
				ret = new DurationData(quantity, format);
			}
		}
		
		return ret;
	}

}


