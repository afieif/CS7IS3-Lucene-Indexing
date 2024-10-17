package org.example;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;

public class Main {

    public static void main(String[] args) throws IOException, ParseException {
        createIndex();
        TestIndex(1);
    }

    public static void createIndex() throws IOException {
        try {
            String corpus = new String(Files.readAllBytes(Paths.get("cran.all.1400")));
            ArrayList<Document> processedDocuments = getDocuments(corpus);

            Analyzer analyzer = CustomAnalyzer.builder()
                    .withTokenizer("classic")
                    .addTokenFilter("trim")
                    .addTokenFilter("lowercase")
                    .addTokenFilter("stop")
                    .addTokenFilter("porterstem")
                    .build();

            Directory directory = FSDirectory.open(Paths.get("./index"));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            try (IndexWriter indexWriter = new IndexWriter(directory, config)) {
                indexWriter.addDocuments(processedDocuments);
            }
        } catch (IOException e) {
            System.err.println("Error creating index: " + e.getMessage());
            throw e;
        }
    }

    private static ArrayList<Document> getDocuments(String corpus) {
        ArrayList<Document> processedDocuments = new ArrayList<>();

        String[] documents = corpus.split("(?=\\.I \\d+)");
        for (String document : documents) {
            try {
                String[] fields = document.split("\\.[ITABW]");
                if (fields.length < 6) {
                    continue; // Skip if not enough fields are found
                }

                String id = fields[1].trim();
                String title = fields[2].trim();
                String content = fields[5].trim();

                Document luceneDocument = new Document();
                luceneDocument.add(new StringField("id", id, Field.Store.YES));
                luceneDocument.add(new TextField("title", title, Field.Store.NO));
                luceneDocument.add(new TextField("content", content, Field.Store.NO));
                processedDocuments.add(luceneDocument);
            } catch (Exception e) {
                System.err.println("Error processing document: " + e.getMessage());
            }
        }
        return processedDocuments;
    }

    public static void TestIndex(Integer similarity) throws IOException, ParseException {
        String queryDoc = new String(Files.readAllBytes(Paths.get("cran.qry")));
        String[] queries = queryDoc.split("(?=\\.I \\d+)");

        try (Directory directory = FSDirectory.open(Paths.get("./index"));
             DirectoryReader reader = DirectoryReader.open(directory);
             BufferedWriter writer = new BufferedWriter(new FileWriter("search_results.txt", false))) {

            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(similarity == 0 ? new ClassicSimilarity() : new BM25Similarity());

            Analyzer analyzer = CustomAnalyzer.builder()
                    .withTokenizer("classic")
                    .addTokenFilter("trim")
                    .addTokenFilter("lowercase")
                    .addTokenFilter("stop")
                    .addTokenFilter("porterstem")
                    .build();

            String[] fields = {"title", "content"};
            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);

            for (String q : queries) {
                try {
                    String[] queryParts = q.split("\\.[IW]");
                    if (queryParts.length < 3) continue;

                    int queryId = Integer.parseInt(queryParts[1].trim());
                    String queryString = queryParts[2].replaceAll("\\?", "").trim();

                    Query parsedQuery = parser.parse(queryString);
                    TopDocs results = searcher.search(parsedQuery, 50);

                    writeResults(writer, queryId, results, searcher, similarity);
                } catch (ParseException | NumberFormatException e) {
                    System.err.println("Error processing query: " + e.getMessage());
                }
            }
        }
    }

    private static void writeResults(BufferedWriter writer, int queryId, TopDocs results,
                                     IndexSearcher searcher, int similarity) throws IOException {
        StoredFields storedFields = searcher.storedFields();
        for (int i = 0; i < results.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = results.scoreDocs[i];
            Document doc = storedFields.document(scoreDoc.doc);
            String line = String.format("%d constant %s %d %.6f %s",
                    queryId, doc.get("id").trim(), i + 1, scoreDoc.score,
                    similarity == 0 ? "STANDARD" : "BM25");
            writer.write(line);
            writer.newLine();
        }
    }
}