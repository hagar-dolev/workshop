

import java.io.File;
import java.io.IOException;


import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.document.Document;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.PointValues;


//import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryparser.classic.ParseException;

//import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;


import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.util.Version;


public class Indexing {

    private IndexWriter writer;

    public Indexing(){

    }



}

