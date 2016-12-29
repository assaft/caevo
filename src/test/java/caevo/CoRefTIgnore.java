package caevo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import caevo.util.CaevoProperties;

@RunWith(Parameterized.class)
public class CoRefTIgnore {

	private static Pattern dctPattern = Pattern.compile("t0\\,(\\d{4}(\\-\\d{2})?(\\-\\d{2})?)");
	
	private final String directory;
	private final String inName;
	private final String inputText;
	private final String expectedOutput;
	private final String dct;
	private final Main main;
		
	public CoRefTIgnore(Main main, String id, String directory, String inName, String inputText, String expectedOutput) {
		this.main = main;
		this.directory = directory;
		this.inName = inName;
		this.inputText = inputText;
		this.expectedOutput = expectedOutput;

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
			String receivedOutput = main.markupRawText(inputText,false,dct).writeToString();
			CaevoResult recResult = new CaevoResult(receivedOutput, false);
			CaevoResult expResult = new CaevoResult(expectedOutput, true);
			Files.write(Paths.get(directory + "/" + inName + "_received.xml"), receivedOutput.getBytes("UTF-8"));
			boolean equals = recResult.timexes.equals(expResult.timexes);
			if (!equals) {
				System.err.println("Result is not as expected for " + inName + ":");
				System.err.println("Input text: " + inputText);
				System.err.println("Received:\n" + recResult.timexes.toString());
				System.err.println("Expected:\n" + expResult.timexes.toString());
			}
			Assert.assertTrue("result is not as expected",equals);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
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
		ClassLoader classLoader = CoRefTIgnore.class.getClassLoader();
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
			testCases.add(new Object[]{
					main,
					id,
					getPath(classLoader,directoryName),
					fileName.substring(0,fileName.indexOf('.')),
					readFile(getPath(classLoader, inputFile)),
					readFile(getPath(classLoader, expectedOutput))});
		}
		
		return testCases;
	}

}
