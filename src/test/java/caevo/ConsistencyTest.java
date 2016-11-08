package caevo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import caevo.util.CaevoProperties;
import caevo.util.Util;

@RunWith(Parameterized.class)
public class ConsistencyTest {

	private final Main main;
	private final String text;
	
	public ConsistencyTest(Main main, String text) {
		super();
		this.main = main;
		this.text = text;
	}
	
	@Test
	public void run() throws IOException {

		// Create a temporary file.
		
		File tempIn = File.createTempFile("testing-consistency_input","txt");
		File tempOut1 = File.createTempFile("testing-consisteny_output1","xml");
		File tempOut2 = File.createTempFile("testing-consisteny_output2","xml");
		
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(tempIn.getAbsolutePath())));
		writer.write(text);
		writer.close();

		// Run the full markup pipeline.
		SieveDocuments docs1 = main.markupRawText(tempIn.getAbsolutePath());
		Assert.assertNotNull(docs1);
		docs1.writeToXML(tempOut1.getAbsolutePath());
		
		// Run the full markup pipeline again.
		SieveDocuments docs2 = main.markupRawText(tempIn.getAbsolutePath());
		Assert.assertNotNull(docs2);
		docs2.writeToXML(tempOut2.getAbsolutePath());
		
		Assert.assertEquals("Generated XML.",
				Util.readLinesFromFile(tempOut1.getAbsolutePath()).toString(),
				Util.readLinesFromFile(tempOut2.getAbsolutePath()).toString());
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
