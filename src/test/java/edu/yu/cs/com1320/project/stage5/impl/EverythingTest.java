package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.DocumentStore;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;
import edu.yu.cs.com1320.project.stage5.impl.DocumentStoreImpl;
import org.junit.Test;
import org.junit.*;
import org.junit.Assert.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;

//import static org.junit.jupiter.api.Assertions.*;

public class EverythingTest  {
    public EverythingTest() throws URISyntaxException {}

    String string1 = "Lakers are the best!";
    String string2 = "Clippers are the worst!";
    String string3 = "The Nuggets are overrated";
    URI uri1 = new URI("www.lakers.com");
    URI uri2 = new URI("www.clippers.com");
    URI uri3 = new URI("www.nuggets.com");
    DocumentImpl doc1 = new DocumentImpl(uri1, string1,null);
    DocumentImpl doc2 = new DocumentImpl(uri2, string2, null);
    DocumentImpl doc3 = new DocumentImpl(uri3, string3, null);
    byte[] b1 = string1.getBytes();
    byte[] b2 = string2.getBytes();
    byte[] b3 = string3.getBytes();
    ByteArrayInputStream stream1 = new ByteArrayInputStream(b1);
    ByteArrayInputStream stream2 = new ByteArrayInputStream(b2);
    ByteArrayInputStream stream3 = new ByteArrayInputStream(b3);
    private static final String pathSeparatorChar = File.pathSeparator;
    private File baseDir = new File(System.getProperty("user.dir"));

    @Test
    public void testEverything() throws IOException {
        DocumentStoreImpl ds = new DocumentStoreImpl();
        ds.put(stream1, uri1, DocumentStore.DocumentFormat.TXT);
        List<Document> results = ds.search("Lakers");
        assertEquals(doc1, results.get(0));
        ds.put(stream3, uri3, DocumentStore.DocumentFormat.TXT);
        results = ds.search("the");
        assertEquals(1, results.size());
        ds.put(stream2, uri2, DocumentStore.DocumentFormat.TXT);
        results = ds.search("the");
        assertEquals(2, results.size());
        ds.undo();
        results = ds.searchByPrefix("th");
        for(Document doc : results) {
            System.out.println(doc.getKey());
        }
        assertEquals(1, results.size());
    }

    @Test
    public void testEverything2() throws IOException {
        DocumentStoreImpl ds = new DocumentStoreImpl();
        ds.put(stream1, uri1, DocumentStore.DocumentFormat.TXT);
        List<Document> results;
        ds.put(stream3, uri3, DocumentStore.DocumentFormat.TXT);
        ds.put(stream2, uri2, DocumentStore.DocumentFormat.TXT);
        results = ds.search("are");
        assertEquals(3, results.size());
        ds.undo(uri3);
        results = ds.search("are");
        assertEquals(2, results.size());
        ds.deleteAllWithPrefix("th");
        results = ds.search("the");
        assertEquals(0, results.size());
    }

    @Test
    public void testHeapandDocMax() throws IOException {
        DocumentStoreImpl ds = new DocumentStoreImpl();
        ds.setMaxDocumentCount(2);;
        ds.put(stream1, uri1, DocumentStore.DocumentFormat.TXT);
        ds.put(stream2, uri2, DocumentStore.DocumentFormat.TXT);
        List<Document> results;
        results = ds.search("the");
        assertEquals(2, results.size());
        ds.put(stream3, uri3, DocumentStore.DocumentFormat.TXT);
        assertNotNull(ds.get(uri1));
        assertNotNull(ds.get(uri2));
        assertNotNull(ds.get(uri3));

    }
    @Test
    public void testHeapandByteMax() throws IOException {
        DocumentStoreImpl ds = new DocumentStoreImpl();
        ds.setMaxDocumentBytes(50);
        ds.put(stream1, uri1, DocumentStore.DocumentFormat.TXT);
        ds.put(stream2, uri2, DocumentStore.DocumentFormat.TXT);
        List<Document> results;
        results = ds.search("the");
        assertEquals(2, results.size());
        ds.put(stream3, uri3, DocumentStore.DocumentFormat.TXT);
        assertNotNull(ds.get(uri1));
        assertNotNull(ds.get(uri2));
        assertNotNull(ds.get(uri3));

    }

    @Test
    public void testUndoAndDeserialize() throws IOException {
        DocumentStoreImpl ds = new DocumentStoreImpl();
        ds.put(stream1, uri1, DocumentStore.DocumentFormat.TXT);
        ds.put(stream2, uri2, DocumentStore.DocumentFormat.TXT);
        ds.put(stream3, uri3, DocumentStore.DocumentFormat.TXT);
        ds.undo();
        assertNull(ds.get(uri3));
        assertEquals(ds.get(uri1),doc1);
        assertFalse(ds.delete(uri3));
        ds.undo(uri1);
        assertEquals(ds.get(uri2),doc2);
    }

    @Test
    public void testSerializeAndDeserializeWithBinaryDoc() throws IOException {
        DocumentStoreImpl ds = new DocumentStoreImpl();
        ds.setMaxDocumentCount(2);
        ds.put(stream1,uri1, DocumentStore.DocumentFormat.BINARY);
        ds.put(stream2, uri2, DocumentStore.DocumentFormat.TXT);
        Document temp = ds.get(uri1);
        ds.put(stream3, uri3, DocumentStore.DocumentFormat.TXT);
        assertEquals(ds.get(uri1), temp);
    }

}

