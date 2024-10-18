package CS7IS3;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.util.ArrayList;

public class DocumentParser {
    public static ArrayList<Document> parse(String corpus) {
        ArrayList<Document> parsedDocuments = new ArrayList<>();
        String[] documents = corpus.split("(?=\\.I \\d+)");

        for(String document:documents){
            String[] fields = document.split("\\.[ITABW]");

            String id = fields[1].trim();
            String title = fields[2].trim();
            String content = fields[5].trim();

            Document luceneDocument = new Document();
            luceneDocument.add(new StringField("id",id, Field.Store.YES));
            luceneDocument.add(new TextField("Title",title,Field.Store.YES));
            luceneDocument.add(new TextField("Content", content,Field.Store.YES));

            parsedDocuments.add(luceneDocument);
        }
        return parsedDocuments;
    }
}
