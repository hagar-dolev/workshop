
import java.io.Console;
import java.io.FileReader;
import java.io.FileWriter;


import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.util.stream.Collectors;

import java.util.stream.Stream;
import java.util.Arrays;


import java.nio.file.Files;

import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.*;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;





import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import static org.apache.lucene.document.DoublePoint.newRangeQuery;


public class MultiDimManager {
    private static double DISTANCE = 20;
    private static int HOWMANY = 1;

    static double [] convertStringNumbersToDouble(String [] numbers){
        double [] dNum = {};
        if (numbers.length == 0) return dNum;
        else {
            dNum = new double[numbers.length];
            int i = 0;
            for (String num : numbers) {
                dNum[i] = Double.parseDouble(num);

            }
            return dNum;
        }
    }

    private static ThreeDModel getModelFromTxtFile(Path modelPath){

        ThreeDModel model = new ThreeDModel();
        try  {
            List<String> stringList = Files.readAllLines(modelPath);
            String[] stringArray = stringList.toArray(new String[]{});
            if (stringArray.length == 1) {
                String[] imgPointString = stringArray[0].split(",");
                double [] imgPoints = convertStringNumbersToDouble(imgPointString);
                model = new ThreeDModel(imgPoints);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return model;

    }

    private static double [] getDoubleListFromJsonArr(JSONArray arr){

        double [] doubleArr = new double [arr.size()];
        for(int i=0;i<arr.size();i++){
            doubleArr[i] = (double) arr.get(i); // iterate the JSONArray and extract the keys
        }

        return doubleArr;
    }

    private static ThreeDModel getModel(Path modelPath){

        ThreeDModel model = new ThreeDModel();
        try  {

            // parsing file "JSONExample.json"
            Object obj = new JSONParser().parse(new FileReader(modelPath.toString()));

            // typecasting obj to JSONObject
            JSONObject jo = (JSONObject) obj;
//            new JSONArray(jsonArrayString);

//            System.out.println(jo.get("silhouette_x"));
//            System.out.println(jo.get("silhouette_x").length);
            double [] fourier_x = getDoubleListFromJsonArr((JSONArray) jo.get("fourier_x"));
            double [] fourier_y = getDoubleListFromJsonArr((JSONArray) jo.get("fourier_y"));
            double [] fourier_z = getDoubleListFromJsonArr((JSONArray) jo.get("fourier_z"));
            String SrcPath = (String) jo.get("src_path");
            String name = (String) jo.get("name");

            model = new ThreeDModel(fourier_x, fourier_y, fourier_z, SrcPath, name);
            // double [] sill_x, double [] sill_y, double [] sill_z, String relSrcPath, String name
            //        model.silhouette_x, model.silhouette_x, model.silhouette_z,
            //                model.relSrcPath, model.name



        } catch (Exception e) {
            e.printStackTrace();
        }

        return model;

    }

    private static void addModelBasicImgPoint(IndexWriter writer,Path modelPath) throws RuntimeException {
        // double [] sill_x, double [] sill_y, double [] sill_z, String relSrcPath, String name

        ThreeDModel model = getModel(modelPath);
        Document document = new Document();
        document.add(new DoublePoint("imgPoint", model.imgPoint));

        //        model.silhouette_x, model.silhouette_y, model.silhouette_z,
        //                model.relSrcPath, model.name
//            document.add(new DoublePoint("silhouette_x", model.silhouette_x));
//            document.add(new DoublePoint("silhouette_y", model.silhouette_y));
//            document.add(new DoublePoint("silhouette_z", model.silhouette_z));


        try {
            writer.addDocument(document); // This is really IOexception.

        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private static void addModel(IndexWriter writer,Path modelPath) throws RuntimeException {
        ThreeDModel model = getModel(modelPath);
        Document document = new Document();

//        if (model == null){return;}

        // X plane
        for (int i = 0; i < 8; i++){
            String fieldName = "fourier_x_" + Integer.toString(i);
            double [] curr_part = Arrays.copyOfRange(model.fourier_x, i * 8, (i+1) *8);
            document.add(new DoublePoint(fieldName, curr_part ));
        }
        // Y plane
        for (int i = 0; i < 8; i++){
            String fieldName = "fourier_y_" + Integer.toString(i);
            double [] curr_part = Arrays.copyOfRange(model.fourier_y, i * 8, (i+1) *8);
            document.add(new DoublePoint(fieldName, curr_part ));
        }
        // Z plane
        for (int i = 0; i < 8; i++){
            String fieldName = "fourier_z_" + Integer.toString(i);
            double [] curr_part = Arrays.copyOfRange(model.fourier_z, i * 8, (i+1) *8);
            document.add(new DoublePoint(fieldName, curr_part));
        }
        document.add(new TextField("SrcPath", model.SrcPath, Field.Store.YES));
        document.add(new TextField("name", model.name, Field.Store.YES));

        try {
            writer.addDocument(document); // This is really IOexception.

        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void buildIndexFromDirectory(String []args) throws IOException {

        Path indexPath = Paths.get(args[0]);
        Path modelsPath = Paths.get(args[1]);

        Directory indexDirectory = FSDirectory.open(indexPath);  //new MMapDirectory(path);
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(indexDirectory, indexWriterConfig);

        try (Stream<Path> paths = Files.walk(modelsPath)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(filePath -> addModel(writer, filePath));

        } catch (Exception e) {
            e.printStackTrace();
        }

        writer.close();
    }

//    public List<Document> searchFiles(String inField, String queryString) {
//        Query query = new QueryParser(inField, analyzer).parse(queryString);
//        Directory indexDirectory = FSDirectory.open(Paths.get(indexPath));
//        IndexReader indexReader = DirectoryReader.open(indexDirectory);
//        IndexSearcher searcher = new IndexSearcher(indexReader);
//        TopDocs topDocs = searcher.search(query, 10);
//
//        return topDocs.scoreDocs.stream()
//                .map(scoreDoc -> searcher.doc(scoreDoc.doc))
//                .collect(Collectors.toList());
//    }

    private static Document getDoc(IndexSearcher searcher, ScoreDoc sd){
        try {
            return searcher.doc(sd.doc);
        } catch (Exception e){

            e.printStackTrace();
        }

        return null;
    }

    private static void docsToJsonFile(String resultsPath, List<Document> results){
        JSONArray docsList = new JSONArray();
        for (Document result : results) {
            JSONObject currDoc = new JSONObject();
            currDoc.put("src", result.get("SrcPath"));
            docsList.add(currDoc);
        }

        try (FileWriter fileW = new FileWriter(resultsPath)) {
            String toWrite = docsList.toJSONString();
            fileW.write(toWrite);
            fileW.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BooleanQuery createQuery(ThreeDModel model){
        BooleanQuery.Builder booleanQueryBuild = new BooleanQuery.Builder();

        for (int i = 0; i < HOWMANY; i ++){

            double [] first_part = Arrays.copyOfRange(model.fourier_x, i*8, (i+1)*8);
            double [] upperRange = Arrays.copyOf(first_part, first_part.length);

            for (int j =0; j < 8; j++){
                upperRange[j] += MultiDimManager.DISTANCE;
            }
            double [] lowerRange = Arrays.copyOf(first_part, first_part.length);
            for (int j =0; j < 8; j++){
                lowerRange[j] -= MultiDimManager.DISTANCE;
            }

            Query query = newRangeQuery("fourier_x_" + Integer.toString(i), lowerRange, upperRange);


            booleanQueryBuild.add(query, BooleanClause.Occur.MUST);
        }


        BooleanQuery bQ = booleanQueryBuild.build();
        return bQ;
    }

    private static void searchByModelBoolean(String []args) throws IOException {
        Path modelPath = Paths.get(args[0]);
        String resultsPath = args[1];
        Path indexPath = Paths.get(args[2]);
        ThreeDModel model = getModel(modelPath);

        BooleanQuery bQ = createQuery(model);

        Directory indexDirectory = FSDirectory.open(indexPath);
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(bQ, 10);

        List<Document> results = Arrays.stream(topDocs.scoreDocs)
                .map(scoreDoc -> getDoc(searcher,scoreDoc))
                .collect(Collectors.toList());

        docsToJsonFile(resultsPath, results);

    }


    private static void searchByModel(String []args) throws IOException {
        Path modelPath = Paths.get(args[0]);
        String resultsPath = args[1];
        Path indexPath = Paths.get(args[2]);
        ThreeDModel model = getModel(modelPath);

        double [] first_part = Arrays.copyOfRange(model.fourier_x, 0, 8);
        double [] upperRange = Arrays.copyOf(first_part, first_part.length);

        for (int i =0; i < 8; i++){
            upperRange[i] += MultiDimManager.DISTANCE;
        }
        double [] lowerRange = Arrays.copyOf(first_part, first_part.length);
        for (int i =0; i < 8; i++){
            lowerRange[i] -= MultiDimManager.DISTANCE;
        }

        Query query = newRangeQuery("fourier_x_0", lowerRange, upperRange);

        Directory indexDirectory = FSDirectory.open(indexPath);
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher searcher = new IndexSearcher(indexReader);
        TopDocs topDocs = searcher.search(query, 10);

        List<Document> results = Arrays.stream(topDocs.scoreDocs)
                .map(scoreDoc -> getDoc(searcher,scoreDoc))
                .collect(Collectors.toList());

        docsToJsonFile(resultsPath, results);

    }

    /**
     * This is the main method managing the program.
     * There are 2 options, building an index and searching a model in one.
     * @param args:
     *            buildIndex indexPath modelsPath
     *            searchModel modelPath resultsPath indexPath
     */
    public static void main(String []args) {

        if (args.length < 3) return;
        String [] input = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (args[0]){
                case "buildIndex":
                    buildIndexFromDirectory(input);
                    break;
                case "searchModel":
                    searchByModelBoolean(input);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


class ThreeDModel{
    double [] imgPoint, fourier_y, fourier_x, fourier_z;
//    String [] tags;
    String name, SrcPath;
    String orgPath;
    String dataPath;

    public ThreeDModel(){ }

//    'silhouette_y', 'silhouette_x', 'silhouette_z', "rel_src_path", "name"

    public ThreeDModel(double [] fourier_x, double [] fourier_y, double [] fourier_z, String SrcPath, String name){
        this.fourier_x = fourier_x;
        this.fourier_y = fourier_y;
        this.fourier_z = fourier_z;
        this.name = name;
        this.SrcPath = SrcPath;
    }

    public ThreeDModel(double [] imgPoint){
        this.imgPoint = imgPoint;
    }

    public ThreeDModel(double [] imgPoint, String orgPath, String dataPath){
        this.imgPoint = imgPoint;
        this.orgPath = orgPath;
        this.dataPath = dataPath;
    }
}


//searchModel
//        /Users/hagardolev/Documents/Computer-Science/Third_year/sara_guidance/m81.json
//        /Users/hagardolev/Documents/Computer-Science/Third_year/sara_guidance/results_1.json
//        /Users/hagardolev/Documents/Computer-Science/Third_year/sara_guidance/lucene_index


//buildIndex
//        /Users/hagardolev/Documents/Computer-Science/Third_year/sara_guidance/lucene_index
//        /Users/hagardolev/Documents/Computer-Science/Third_year/sara_guidance/models_jsons