package caevo;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import caevo.util.Util;
import junit.framework.TestCase;

public class FileVsBufferTest extends TestCase {
	
	Main main = new Main();
	
	public void testFileVsBuffer(String text) throws IOException {

		// Create a temporary file.
		String tempfile = "input.txt";
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(tempfile)));
		writer.write(text);
		writer.close();

		// Run the full markup pipeline on the temp file.
		SieveDocuments docs1 = main.markupRawText(tempfile);
		assertNotNull(docs1);
		docs1.writeToXML("output.xml");
		List<String> lines = Util.readLinesFromFile("output.xml");
		StringBuilder fileOutput = new StringBuilder();
		String separator = System.getProperty("line.separator");
		for (String line : lines) {
			fileOutput.append(line);
			fileOutput.append(separator);
		}

		// Run the full markup pipeline on the buffer.
		SieveDocuments docs2 = main.markupRawText(text, false);
		assertNotNull(docs2);
		String bufferOutput = docs2.writeToString();
		
		// compare results
		assertEquals("Generated XML.",fileOutput.toString(),bufferOutput);
	}
	
	
	public void testRawToTimex() throws Exception {
		
		List<String> texts = Arrays.asList(
				"Libya brought the case in 2003 to Britain because of 11/25/1980 and complained.",
				"Libya, which brought the case to the United Nations' highest judicial body in its dispute with the United States and Britain, hailed the ruling and said it would press anew for a trial in a third neutral country. Britain will complain because they always complain.");

		for (String text : texts) {
			testFileVsBuffer(text);
		}
		
	}

}
