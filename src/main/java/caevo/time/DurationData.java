package caevo.time;

import caevo.Timex;
import caevo.Timex.Mod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value @AllArgsConstructor(access=AccessLevel.PRIVATE)
public class DurationData implements TimeData {
	int quantity;
	Timex.Mod mod;
	String modText;
	Format format;
	TimeUnit unit;

	public DurationData(int quantity, Format format) {
		this(quantity,null,null,format,Format.toTimeUnit(format));
	}
	
	public DurationData(int quantity, Timex.Mod mod, String modText, TimeUnit unit) {
		this(quantity,mod,modText,Format.fromTimeUnit(unit),unit);
	}
	
	public String toString() {
		return "P" + (unit.isTimeUnit() ? "T" : "") + quantity + unit.toString().substring(0,1);
	}

	public String toText(boolean incMod) {
		return (incMod ? modText + " " : "") + quantity + " " + unit.toString().toLowerCase() + (quantity!=1 ? "s" : "");
	}

	public DurationData with(int quantity, Mod mod, String modText) {
		return new DurationData(quantity,mod,modText,unit);
	}
	
}
