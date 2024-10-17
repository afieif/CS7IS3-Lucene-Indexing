package org.example;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.TextField;
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



public class Main
{

    public static void main(String[] args) throws IOException, ParseException {
        createIndex();
        TestIndex(1);
    }
    
    public static void createIndex() throws IOException {
        //  Tokenizers:[pattern, simplePatternSplit, korean, wikipedia, japanese, keyword, thai, standard, pathHierarchy, openNlp, whitespace, letter, hmmChinese, icu, uax29UrlEmail, edgeNGram, classic, nGram, simplePattern]

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
        IndexWriter indexWriter = new IndexWriter(directory, config);
        indexWriter.addDocuments(processedDocuments);
        indexWriter.close();
        directory.close();
    }

    private static ArrayList<Document> getDocuments(String corpus) {
        ArrayList<Document> processedDocuments = new ArrayList<Document>();

        String[] documents = corpus.split("(?=\\.I \\d+)");
        for(String document: documents){
            String[] fields = document.split("\\.[ITABW]");
            StringField id = new StringField("id",fields[1],Field.Store.YES);
            TextField title = new TextField("title", fields[2], Field.Store.NO);
            TextField content = new TextField("content", fields[5], Field.Store.NO);
            Document luceneDocument = new Document();
            luceneDocument.add(title);
            luceneDocument.add(content);
            luceneDocument.add(id);
            processedDocuments.add(luceneDocument);
        }
        return processedDocuments;
    }

    public static void TestIndex(Integer similarity) throws IOException, ParseException {

        String queryDoc = new String(Files.readAllBytes(Paths.get("cran.qry")));
        String[] queries = queryDoc.split("(?=\\.I \\d+)");

        Directory directory = FSDirectory.open(Paths.get("./index"));
        DirectoryReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);

        if(similarity == 0) {
            searcher.setSimilarity(new ClassicSimilarity());
        } else {
            searcher.setSimilarity(new BM25Similarity());
        }

        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer("classic")
                .addTokenFilter("trim")
                .addTokenFilter("lowercase")
                .addTokenFilter("stop")
                .addTokenFilter("porterstem")
                .build();

        String[] fields = {"title", "content"};
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);

        // Open the writer once outside the loop
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("search_results.txt", false))) {
            for (String q : queries) {
                String[] query = q.split("\\.[IW]");
                Query queryString = parser.parse(query[2].replaceAll("\\?", "").trim());
                TopDocs results = searcher.search(queryString, 50);
                StoredFields storedFields = searcher.storedFields();

                int index = 0;
                for (ScoreDoc scoreDoc : results.scoreDocs) {
                    index++;
                    Document doc = storedFields.document(scoreDoc.doc);
                    String line;
                    if (similarity == 0) {
                        line = Integer.parseInt(query[1].trim()) + " constant " + doc.get("id").trim() + " " + index + " " + scoreDoc.score + " STANDARD";
                    } else {
                        line = Integer.parseInt(query[1].trim()) + " constant " + doc.get("id").trim() + " " + index + " " + scoreDoc.score + " BM25";
                    }
                    writer.write(line);
                    writer.newLine();
                }
            }
        }

        reader.close();
        directory.close();

    }
}