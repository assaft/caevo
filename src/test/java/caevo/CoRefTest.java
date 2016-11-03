package caevo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import caevo.CaevoResult.ElementPair;
import caevo.CaevoResult.EntityType;
import caevo.CaevoResult.MeasureType;
import caevo.CaevoResult.Measures;
import caevo.CaevoResult.TimelineMeasures;
import caevo.util.CaevoProperties;

@RunWith(Parameterized.class)
public class CoRefTest {

	private static Pattern dctPattern = Pattern.compile("t0\\,(\\d{4}(\\-\\d{2})?(\\-\\d{2})?)");
	
	private final String id;
	private final String directory;
	private final String inName;
	private final String inputText;
	private final String expectedOutput;
	private final String gold;
	private final Map<String,String> alignment; 
	private final String dct;
	private final Main main;
		
	public CoRefTest(Main main, String id, String directory, String inName, String inputText, String expectedOutput, String gold, String alignment) {
		this.main = main;
		this.id = id;
		this.directory = directory;
		this.inName = inName;
		this.inputText = inputText;
		this.expectedOutput = expectedOutput;
		this.gold = gold;

		/*
		this.alignment = new HashMap<String, String>();
		for (String line : alignment.split("\n")) {
			line = line.trim();
			if (!line.isEmpty()) {
				String[] lineParts = line.split("=");
				this.alignment.put(lineParts[0], lineParts[1]);
			}
		}*/
		this.alignment = null;

		Matcher matcher = dctPattern.matcher(expectedOutput);
		if (!matcher.find()) {
			System.err.println("Expected:\n" + expectedOutput.toString());
			throw new RuntimeException("Timex t0 no found in expected result");
		}
		this.dct = matcher.group(1);
	}
	
	@Test
	public void run() {
		try {
			/*
			CaevoResult goldResult = new CaevoResult(gold, true);
			Set<ElementPair> densityResult = goldResult.checkDensity();
			if (!densityResult.isEmpty()) {
				throw new Exception("Gold verification failed for test t" + id + "; missing links: " + densityResult); 
			}*/
			
			String receivedOutput = main.markupRawText(inputText,false,dct).writeToString();
			CaevoResult recResult = new CaevoResult(receivedOutput, false);
			CaevoResult expResult = new CaevoResult(expectedOutput, true);
			Files.write(Paths.get(directory + "/" + inName + "_received.xml"), receivedOutput.getBytes("UTF-8"));
			boolean equals = recResult.equals(expResult);
			if (!equals) {
				System.err.println("Result is not as expected for " + inName + ":");
				System.err.println("Input text: " + inputText);
				System.err.println("Received:\n" + recResult.toString());
				System.err.println("Expected:\n" + expResult.toString());
				
				System.err.println(recResult.toSTF());
			}
			/*
			TimelineMeasures measures = CaevoResult.calcMeasures(goldResult, expResult, alignment);
			Files.write(Paths.get(directory + "/" + inName + "_scores.csv"), measuresToCSV(measures).getBytes("UTF-8"));
			*/
			Assert.assertTrue("result is not as expected",equals);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
	
	private String measuresToCSV(TimelineMeasures timelineMeasures) {
		
		// create header line
		StringBuilder buffer = new StringBuilder(",");
		for (MeasureType measureType : MeasureType.values()) {
			buffer.append(measureType.toString().substring(0, 2) + ",");
		}
		buffer.deleteCharAt(buffer.length()-1);
		buffer.append("\n");
		
		// counters for calculating the average
		Map<MeasureType,Double> sums = new HashMap<CaevoResult.MeasureType, Double>();
		for (MeasureType measureType : MeasureType.values()) {
			sums.put(measureType,0.0);
		}
		
		// create lines of values
		for (EntityType entityType : EntityType.values()) {
			Measures measures = timelineMeasures.getMeasures(entityType);
			buffer.append(entityType.toString() + "s" + ",");
			for (MeasureType measureType : MeasureType.values()) {
				double value = measures.getMeasure(measureType);
				buffer.append(String.format("%.2f", value) + ",");
				sums.put(measureType,sums.get(measureType) + value);
			}
			buffer.deleteCharAt(buffer.length()-1);
			buffer.append("\n");
		}
		
		// create average line
		buffer.append("Average,");
		for (MeasureType measureType : MeasureType.values()) {
			double value = sums.get(measureType) / EntityType.values().length; 
			buffer.append(String.format("%.2f", value) + ",");
		}
		buffer.deleteCharAt(buffer.length()-1);
		buffer.append("\n");
		
		return buffer.toString();
	}

	public static Pattern inputFileRegEx = Pattern.compile("^t(\\d\\d)\\.txt$");
	
	public static Map<String,String> listInputFiles(final File folder) {
		Map<String,String> files = new TreeMap<String,String>();
		for (final File fileEntry : folder.listFiles()) {
      Matcher matcher = inputFileRegEx.matcher(fileEntry.getName());
			if (!fileEntry.isDirectory() && matcher.find()) {
        files.put(matcher.group(1),fileEntry.getName());  
      }
    }
		return files;
	}
	
	public static String readFile(String path) throws IOException {
		return new String (Files.readAllBytes(Paths.get(path)),Charset.forName("UTF-8")); 		
	}
	
	
	
	protected static String getPath(ClassLoader classLoader, String fileName) {
		URL resource = classLoader.getResource(fileName);
		if (resource==null) {
			throw new RuntimeException("File not found: " + fileName);
		}
		return new File(resource.getFile()).getPath();
	}
	
	@Parameterized.Parameters(name= "{1}: {3}")
	public static Collection<?> initParams() throws Exception {
		
		// Constants
		String directoryName = "coref";
		String testProperties = "test.properties";

		// Read test properties file
		ClassLoader classLoader = CoRefTest.class.getClassLoader();
		CaevoProperties.load(getPath(classLoader,directoryName + "/" + testProperties));
		
		//Get the files from resources folder
		File directory = new File(classLoader.getResource(directoryName).getFile());
		Map<String,String> files = listInputFiles(directory);

		// Create Caevo's instance
		Main main = new Main();
		
		// Create the test cases
		List<Object[]> testCases = new ArrayList<Object[]>();
		for (String id : files.keySet()) {
			String fileName = files.get(id);
			String inputFile = directoryName + "/" + fileName;
			String expectedOutput = directoryName + "/" + fileName.substring(0,fileName.indexOf('.')) + "_expected.stf";
			//String goldResult = 		directoryName + "/" + fileName.substring(0,fileName.indexOf('.')) + "_gold.stf";
			//String alignmentFile = 	directoryName + "/" + fileName.substring(0,fileName.indexOf('.')) + "_alignment.txt";
			testCases.add(new Object[]{
					main,
					id,
					getPath(classLoader,directoryName),
					fileName.substring(0,fileName.indexOf('.')),
					readFile(getPath(classLoader, inputFile)),
					readFile(getPath(classLoader, expectedOutput)),
					"",//readFile(getPath(classLoader, goldResult)),
					""//readFile(getPath(classLoader, alignmentFile))
					});
		}
		
		return testCases;
	}

}
