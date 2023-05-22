package edu.yu.cs.com1320.project.stage5.impl;

import com.sun.jdi.Value;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import edu.yu.cs.com1320.project.stage5.Document;

import java.net.URISyntaxException;
import java.util.*;

import com.google.gson.*;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import jakarta.xml.bind.DatatypeConverter;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;


import static java.lang.System.nanoTime;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;


/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private File baseDir;
    private static final String pathSeparatorChar = File.pathSeparator;
    private Map<URI, Boolean> textDocument = new HashMap<>();

    public DocumentPersistenceManager(File baseDir){
        if (baseDir == null) {
            this.baseDir = new File(System.getProperty("user.dir"));
        } else {
            this.baseDir = baseDir;
        }
    }



    @Override
    public void serialize(URI uri, Document val) throws IOException {
        GsonBuilder gsonBuildr = new GsonBuilder();
        gsonBuildr.registerTypeAdapter(Document.class, new DocumentSerializer());
        String jsonString = gsonBuildr.create().toJson(val);



      //  Gson gson = new Gson();
        String filepath = uri.toString().replace("http://", "");
        filepath.replace("/", pathSeparatorChar);
        filepath += (".json");
        File dir = new File(baseDir, filepath);
        dir.getParentFile().mkdirs();
        Writer writer = new FileWriter(dir);
       // writer.write(gson.toJson(val).toString());
        writer.write(jsonString);
        writer.close();
    }

    private class DocumentSerializer implements JsonSerializer<Document> {
        public JsonElement serialize(Document src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jObject = new JsonObject();
            String uri = "uri";
            String text = "text";
            String wordCountMap = "wordCountMap";
            String binaryData = "binaryData";
            jObject.addProperty(uri, src.getKey().toString());
            if (src.getDocumentBinaryData() != null ) {
                String base64Encoded = DatatypeConverter.printBase64Binary(src.getDocumentBinaryData());
                jObject.addProperty(binaryData, base64Encoded);
            } else {
                jObject.addProperty(text, src.getDocumentTxt());
                String wordMapasJsonString = JSONValue.toJSONString(src.getWordMap());
                jObject.addProperty(wordCountMap, wordMapasJsonString);
            }
            return jObject;
        }
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        Gson gson = new Gson();
        String filepath = uri.toString().replace("http://", "");
        filepath.replace("/", pathSeparatorChar);
        filepath += (".json");
        File dir = new File(baseDir, filepath);
        FileReader fileReader = new FileReader(dir);
        BufferedReader encoded = new BufferedReader(fileReader);
        Document doc = gson.fromJson(encoded, DocumentImpl.class);
        delete(uri);
        doc.setLastUseTime(nanoTime());
        return doc;
    }

    private class DocumentDeseralizer implements JsonDeserializer<Document>{
        @Override
        public Document deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jsonFile = jsonElement.getAsJsonObject();
            Document doc = null;
            if (jsonFile.has("text")) {
                try {
                    HashMap<String, Integer> wordCount = new Gson().fromJson(jsonFile.getAsJsonObject("wordCountMap"), HashMap.class);
                    doc = new DocumentImpl(new URI(jsonFile.get("uri").getAsString()), jsonFile.get("text").getAsString(), wordCount);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    doc = new DocumentImpl(new URI(jsonFile.get("uri").getAsString()), DatatypeConverter.parseBase64Binary(jsonFile.get("binaryData").getAsString()));
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            return doc;
        }
    }


    @Override
    public boolean delete(URI uri) throws IOException {
        boolean docDeleted;
        String filepath = uri.toString().replace("http://", "");
        filepath.replace("/", pathSeparatorChar);
        filepath += (".json");
        File dir = new File(baseDir, filepath);
        docDeleted = dir.delete();
        return docDeleted;
    }
}