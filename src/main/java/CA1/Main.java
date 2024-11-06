package CA1;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;

class Main{
    public static void main(String[] args) throws IOException, ParseException{
        Analyzer analyzer = CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter("stop")
                .addTokenFilter("porterstem")
                .build();
        Indexer.createIndex(analyzer);
        Searcher.searchQueries(analyzer);
    }
}