package solrproject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashSet;

public class FormPageRankGraph {

	private static final String FILEPATH = "/home/mukund/ir/assignment4/Reuters/URLtoHTML_reuters_news.csv";

	private static final String DIRPATH = "/home/mukund/ir/assignment4/Reuters/reutersnews";

	private static final String EDGELIST = "/home/mukund/ir/assignment4/edgeList.txt";

	public static void main(String[] args) throws Exception {

		HashMap<String, String> fileUrlMap = new HashMap<String, String>();
		HashMap<String, String> urlFileMap = new HashMap<String, String>();
		ArrayList<String> urls;

		BufferedReader br = null;
		FileReader fr = null;

		try {

			fr = new FileReader(FILEPATH);
			br = new BufferedReader(fr);

			String curr;

			br = new BufferedReader(new FileReader(FILEPATH));

			while ((curr = br.readLine()) != null) {

				urls = new ArrayList<String>(Arrays.asList(curr.split(",")));

				String url0 = urls.get(0).trim();

				String url1 = urls.get(1).trim();

				fileUrlMap.put(url0, url1);

				urlFileMap.put(url1, url0);

			}

		} catch (IOException e) {

			e.printStackTrace();

		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if (fr != null) {
					fr.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		File dir = new File(DIRPATH);

		Set<String> edges = new HashSet<String>();

		for (File file : dir.listFiles()) {

			Document doc = Jsoup.parse(file, "UTF-8", fileUrlMap.get(file.getName()));

			Elements links = doc.select("a[href]");

			for (Element link : links) {

				String url = link.attr("abs:href").trim();

				if (urlFileMap.containsKey(url)) {

					edges.add(file.getName() + " " + urlFileMap.get(url));

				}
			}
		}

		BufferedWriter bw = null;

		FileWriter fw = null;

		try {

			fw = new FileWriter(EDGELIST);

			bw = new BufferedWriter(fw);

			for (String s : edges) {

				bw.write(s);

				bw.write("\n");

			}

		}

		catch (IOException e) {

			e.printStackTrace();

		}

		finally {

			try {

				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();

			} catch (IOException e) {

				e.printStackTrace();

			}
		}
	}
}

