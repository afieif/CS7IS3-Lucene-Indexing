package CS7IS3;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Searcher {
    public static void searchQueries(Analyzer analyzer) throws IOException, ParseException {
        String indexDirectory = "./index";
        String resultDirectory = "search_results.txt";
        String queryDirectory = "cran.qry";

        Directory directory = FSDirectory.open(Paths.get(indexDirectory));
        DirectoryReader reader = DirectoryReader.open(directory);

        IndexSearcher indexSearcher = new IndexSearcher(reader);
        indexSearcher.setSimilarity(new BM25Similarity());

        String[] fields = {"title", "content"};
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);

        String queryDoc = new String(Files.readAllBytes(Paths.get(queryDirectory)));
        String[] queries = queryDoc.split("(?=\\.I \\d+)");

        try (PrintWriter writer = new PrintWriter(new FileWriter(resultDirectory))) {
            int idx = 0;
            for (String q : queries) {
                try {
                    idx++;
                    String[] queryParts = q.split("\\.[IW]");

                    String queryString = queryParts[2].trim().replace("?", "");
                    Query parsedQuery = parser.parse(queryString);

                    TopDocs results = indexSearcher.search(parsedQuery, 120);

                    ScoreDoc[] hits = results.scoreDocs;
                    for (int i = 0; i < hits.length; i++) {
                        int docId = hits[i].doc;
                        Document document = indexSearcher.storedFields().document(docId);
                        writer.printf("%d Q0 %s %d %.6f %s%n",
                                idx, document.get("id"), i + 1, hits[i].score, "BM25");
                    }
                } catch (ParseException | NumberFormatException e) {
                    System.err.println("Error processing query: " + e.getMessage());
                }
            }
        }
    }
}
