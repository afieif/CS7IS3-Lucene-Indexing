package CS7IS3;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Indexer {
    public static void createIndex(Analyzer analyzer) throws IOException{
        String INDEX_DIRECTORY = "./index";
        String CRANFIELD_DIRECTORY = "cran.all.1400";
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        IndexWriter indexWriter = new IndexWriter(directory,config);
        String corpus = new String(Files.readAllBytes(Paths.get(CRANFIELD_DIRECTORY)));
        ArrayList<Document> documents = DocumentParser.parse(corpus);
        indexWriter.addDocuments(documents);

    }
}
