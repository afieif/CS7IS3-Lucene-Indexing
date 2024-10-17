package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CranfieldSearcher {
    public static void main(String[] args) throws Exception {
        Directory indexDirectory = CranfieldIndexer.main(null); // Get the indexed directory
        IndexReader reader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(reader);
        StandardAnalyzer analyzer = new StandardAnalyzer();

        QueryParser parser = new QueryParser("content", analyzer);
        parser.setAllowLeadingWildcard(true); // Allow leading wildcards

        try (BufferedReader br = new BufferedReader(new FileReader("cran.qry"));
             FileWriter writer = new FileWriter("results.trec")) {

            String line;
            StringBuilder queryBuilder = new StringBuilder();
            int queryID = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (queryBuilder.length() > 0) {
                        processQuery(queryID, queryBuilder.toString(), searcher, parser, writer);
                        queryBuilder.setLength(0);
                    }
                    queryID = Integer.parseInt(line.substring(3).trim());
                } else if (!line.startsWith(".")) {
                    queryBuilder.append(line).append(" ");
                }
            }
            // Process the last query
            if (queryBuilder.length() > 0) {
                processQuery(queryID, queryBuilder.toString(), searcher, parser, writer);
            }
        }

        reader.close();
    }

    private static void processQuery(int queryID, String queryText, IndexSearcher searcher, QueryParser parser, FileWriter writer) throws Exception {
        // Escape special characters in the query
        String escapedQuery = QueryParser.escape(queryText);
        Query query = parser.parse(escapedQuery);
        TopDocs results = searcher.search(query, 100); // Retrieve top 100 results

        for (int i = 0; i < results.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = results.scoreDocs[i];
            Document doc = searcher.doc(scoreDoc.doc);
            writer.write(String.format("%d Q0 %s %d %f STANDARD\n",
                    queryID, doc.get("id"), i + 1, scoreDoc.score));
        }
    }
}