package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {
    private static final String CRAN_DOC_DIRECTORY = "cran.all.1400";
    private static final String CRAN_QRY_DIRECTORY = "cran.qry";
    private static final String INDEX_DIRECTORY = "./index";
    private static final String RESULT_DIRECTORY = "search_results.txt";

    public static void main(String[] args) throws IOException, ParseException {
        Analyzer analyzer = createAnalyzer();
        createIndex(analyzer);
        searchQueries(analyzer, new BM25Similarity());
    }

    private static Analyzer createAnalyzer() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("wikipedia")
                .addTokenFilter("lowercase")
                .addTokenFilter("stop")
                .addTokenFilter("porterstem")
                .build();
    }

    private static void createIndex(Analyzer analyzer) throws IOException {
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        try (IndexWriter indexWriter = new IndexWriter(directory, config)) {
            String corpus = new String(Files.readAllBytes(Paths.get(CRAN_DOC_DIRECTORY)));
            ArrayList<Document> documents = getDocuments(corpus);
            indexWriter.addDocuments(documents);
        }
    }

    private static ArrayList<Document> getDocuments(String corpus) {
        ArrayList<Document> processedDocuments = new ArrayList<>();
        String[] documents = corpus.split("(?=\\.I \\d+)");

        for (String document : documents) {
            try {
                String[] fields = document.split("\\.[ITABW]");
                if (fields.length < 6) continue;

                String id = fields[1].trim();
                String title = fields[2].trim();
                String author = fields[3].trim();
                String bibliography = fields[4].trim();
                String content = fields[5].trim();

                Document luceneDocument = new Document();
                luceneDocument.add(new StringField("id", id, Field.Store.YES));
                luceneDocument.add(new TextField("title", title, Field.Store.YES));
                luceneDocument.add(new TextField("author", author, Field.Store.YES));
                luceneDocument.add(new TextField("bibliography", bibliography, Field.Store.YES));
                luceneDocument.add(new TextField("content", content, Field.Store.YES));

                processedDocuments.add(luceneDocument);
            } catch (Exception e) {
                System.err.println("Error processing document: " + e.getMessage());
            }
        }
        return processedDocuments;
    }

    private static void searchQueries(Analyzer analyzer, Similarity similarity) throws IOException, ParseException {
        try (Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
             DirectoryReader reader = DirectoryReader.open(directory);
             PrintWriter writer = new PrintWriter(new FileWriter(RESULT_DIRECTORY))) {

            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(similarity);

            String[] fields = {"title", "author", "bibliography", "content"};
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);

            String queryDoc = new String(Files.readAllBytes(Paths.get(CRAN_QRY_DIRECTORY)));
            String[] queries = queryDoc.split("(?=\\.I \\d+)");

            int idx = 0;
            for (String q : queries) {
                try {
                    idx++;
                    String[] queryParts = q.split("\\.[IW]");
                    if (queryParts.length < 3) continue;

                    int queryId = Integer.parseInt(queryParts[1].trim());
                    String queryString = queryParts[2].trim().replace("?","");

                    Query parsedQuery = parser.parse(queryString);
                    TopDocs results = searcher.search(parsedQuery, 15);

                    writeResults(writer, idx, results, searcher, similarity);
                } catch (ParseException | NumberFormatException e) {
                    System.err.println("Error processing query: " + e.getMessage());
                }
            }
        }
    }

    private static void writeResults(PrintWriter writer, int queryId, TopDocs results,
                                     IndexSearcher searcher, Similarity similarity) throws IOException {
        ScoreDoc[] hits = results.scoreDocs;
        for (int i = 0; i < hits.length; i++) {
            Document doc = searcher.doc(hits[i].doc);
            writer.printf("%d Q0 %s %d %.6f %s%n",
                    queryId, doc.get("id"), i + 1, hits[i].score,
                    similarity instanceof ClassicSimilarity ? "STANDARD" : "BM25");
        }
    }
}