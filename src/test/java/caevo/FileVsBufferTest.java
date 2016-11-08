package caevo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import caevo.util.CaevoProperties;
import caevo.util.Util;
import org.junit.Assert;

@RunWith(Parameterized.class)
public class FileVsBufferTest {
	
	private final Main main;
	private final String text;
	
	public FileVsBufferTest(Main main, String text) {
		super();
		this.main = main;
		this.text = text;
	}

	@Test
	public void run() throws IOException {

		// Create a temporary file.
		File tempIn = File.createTempFile("input","txt");
		File tempOut = File.createTempFile("output1","xml");
		
		
		String tempInfile = tempIn.getAbsolutePath();
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(tempInfile)));
		writer.write(text);
		writer.close();

		// Run the full markup pipeline on the temp file.
		String tempOutfile = tempOut.getAbsolutePath();
		SieveDocuments docs1 = main.markupRawText(tempInfile);
		Assert.assertNotNull(docs1);
		docs1.writeToXML(tempOutfile);
		List<String> lines = Util.readLinesFromFile(tempOutfile);
		StringBuilder fileOutput = new StringBuilder();
		String separator = "\r\n"; // to match Format.getPrettyXML() used by docs2.writeToString() below  
		for (String line : lines) {
			fileOutput.append(line);
			fileOutput.append(separator);
		}

		// Run the full markup pipeline on the buffer.
		SieveDocuments docs2 = main.markupRawText(text, false, main.getDefaultFixedDct(), tempIn.getName());
		Assert.assertNotNull(docs2);
		String bufferOutput = docs2.writeToString();
		
		// compare results
		if (!fileOutput.toString().equals(bufferOutput)) {
			System.out.println("Not equal!");
			Files.write(Paths.get("file1.xml"), fileOutput.toString().getBytes("UTF-8"));
			Files.write(Paths.get("file2.xml"), bufferOutput.getBytes("UTF-8"));
		}
		
		Assert.assertEquals("Generated XML.",fileOutput.toString(),bufferOutput);
	}

	private static List<String> texts = Arrays.asList(
			"Libya brought the case in 2003 to Britain because of 11/25/1980 and complained.",
			"Libya, which brought the case to the United Nations' highest judicial body in its dispute with the United States and Britain, hailed the ruling and said it would press anew for a trial in a third neutral country. Britain will complain because they always complain.");
	
	@Parameterized.Parameters(name= "{1}")
	public static Collection<?> initParams() throws Exception {

		// test properties
		String testProperties = "test.properties";
		String directoryName = "general";

		// Read test properties file
		ClassLoader classLoader = TestSuite.class.getClassLoader();
		CaevoProperties.load(TestTools.getPath(classLoader,directoryName + "/" + testProperties));
		
		// caevo
		Main main = new Main();

		// prepare the test cases
		List<Object[]> testCases = new ArrayList<Object[]>();
		for (String text : texts) {
			testCases.add(new Object[]{main,text});
		}
 
		return testCases;
	}


}
