import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class InvertedIndexJob {
	private static final Logger LOG = Logger.getLogger("InvertedIndexJob");

	public static class InvertedIndexMapper extends Mapper<LongWritable, Text, Text, Text> {

		private Text word = new Text();
		private Text docID = new Text();

		@Override
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			LOG.log(Level.INFO, line);
			String[] content = line.split("\t", 2);
			docID.set(content[0]);
			String docContent = content[1];
			docContent = docContent.replaceAll("[^a-zA-Z]", " ").toLowerCase();
			StringTokenizer itr = new StringTokenizer(docContent);
			while (itr.hasMoreTokens()) {
				String nextToken = itr.nextToken();
				if (nextToken.length() > 0 && nextToken != " ") {
					word.set(nextToken);
					context.write(word, docID);
				}
			}
		}

	}

	public static class InvertedIndexReducer extends Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			LOG.log(Level.INFO, "inside reducer");
			Map<String, Integer> docidVsCounter = new HashMap<String, Integer>();
			for (Text value : values) {
				String docId = value.toString();
				docidVsCounter.put(docId, docidVsCounter.getOrDefault(docId, 0) + 1);
			}
			StringBuilder builder = new StringBuilder();
			for (Entry<String, Integer> entries : docidVsCounter.entrySet()) {
				builder.append(entries.getKey()).append(":").append(entries.getValue()).append(" ");
			}
			context.write(key, new Text(builder.toString()));
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "inverted index");
		job.setJarByClass(InvertedIndexJob.class);
		job.setMapperClass(InvertedIndexMapper.class);
		job.setReducerClass(InvertedIndexReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}