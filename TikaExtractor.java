import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class TikaExtractor {

	public static List<String> parseHtmlFile(File myFile)
			throws FileNotFoundException, IOException, SAXException, TikaException {
		// create a file input stream
		FileInputStream inputstream = new FileInputStream(myFile);

		BodyContentHandler handler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
		ParseContext pcontext = new ParseContext();

		HtmlParser htmlparser = new HtmlParser();
		htmlparser.parse(inputstream, handler, metadata, pcontext);

		String htmlContent = handler.toString();

		List<String> words = Arrays.asList(htmlContent.split("\\W+"));
		return words;
	}

	public static void main(String args[]) throws FileNotFoundException, IOException, SAXException, TikaException {
		String directory = "/home/mukund/ir/assignment4/Reuters/reutersnews";
		File dir = new File(directory);
		File[] fileList = dir.listFiles();
		LinkedList<String> allWords = new LinkedList<String>();
		for (File file : fileList) {
			List<String> words = parseHtmlFile(file);
			allWords.addAll(words);

		}
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/mukund/ir/assignment4/big.txt"));
		try {
			for (String words : allWords) {
				writer.write(words + "\n");
			}
		} finally {
			writer.close();
		}
	}
}