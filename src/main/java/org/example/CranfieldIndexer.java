package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CranfieldIndexer {
    public static Directory main(String[] args) throws IOException {
        Directory indexDirectory = new ByteBuffersDirectory();
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(indexDirectory, config);

        try (BufferedReader br = new BufferedReader(new FileReader("cran.all.1400"))) {
            String line;
            Document doc = null;
            StringBuilder contentBuilder = new StringBuilder();
            String currentId = "";
            while ((line = br.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (doc != null) {
                        doc.add(new TextField("content", contentBuilder.toString(), Field.Store.YES));
                        indexWriter.addDocument(doc);
                        contentBuilder.setLength(0);
                    }
                    doc = new Document();
                    currentId = line.substring(3).trim();
                    doc.add(new StringField("id", currentId, Field.Store.YES));
                } else if (!line.startsWith(".")) {
                    contentBuilder.append(line).append(" ");
                }
            }
            // Add the last document
            if (doc != null) {
                doc.add(new TextField("content", contentBuilder.toString(), Field.Store.YES));
                indexWriter.addDocument(doc);
            }
        }
        indexWriter.close();
        return indexDirectory;
    }
}