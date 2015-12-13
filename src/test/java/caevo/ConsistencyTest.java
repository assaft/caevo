package caevo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import caevo.util.Util;
import junit.framework.TestCase;

public class ConsistencyTest extends TestCase {
	
	Main main = new Main();
	
	public void testConsistency(String text) throws IOException {

		// Create a temporary file.
		String tempfile = "testing-consistency.txt";
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(tempfile)));
		writer.write(text);
		writer.close();

		// Run the full markup pipeline.
		SieveDocuments docs1 = main.markupRawText(tempfile);
		assertNotNull(docs1);
		docs1.writeToXML("testing-consisteny_output1.xml");
		
		// Run the full markup pipeline again.
		SieveDocuments docs2 = main.markupRawText(tempfile);
		assertNotNull(docs2);
		docs2.writeToXML("testing-consisteny_output2.xml");
		
		assertEquals("Generated XML.",
				Util.readLinesFromFile("testing-consisteny_output1.xml").toString(),
				Util.readLinesFromFile("testing-consisteny_output2.xml").toString());
	}
	
	
	public void testRawToTimex() throws Exception {
		
		List<String> texts = Arrays.asList(
				"Libya brought the case in 2003 to Britain because of 11/25/1980 and complained.",
				"Libya, which brought the case to the United Nations' highest judicial body in its dispute with the United States and Britain, hailed the ruling and said it would press anew for a trial in a third neutral country. Britain will complain because they always complain.");

		for (String text : texts) {
			testConsistency(text);
		}
		
	}

}
