package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.*;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import static java.lang.System.nanoTime;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;


public class DocumentStoreImpl implements DocumentStore {
    private StackImpl<Undoable> commandStack;
    private TrieImpl<URI> trie;
    private MinHeapImpl<URI> minHeap;
    private BTreeImpl<URI, Document> BTree;
    private int maxDocumentCount;
    private int currentDocumentCount;
    private int maxDocumentBytes;
    private int currentDocumentBytes;
    private Boolean maxDocBytesActivated = false;
    private Boolean maxDocCountActivated = false;


    public DocumentStoreImpl() {
        this.BTree = new BTreeImpl<>();
        this.commandStack = new StackImpl<>();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>(this.BTree);
        this.BTree.setPersistenceManager(new DocumentPersistenceManager(null));
    }

    public DocumentStoreImpl(File file) {
        this.BTree = new BTreeImpl<>();
        this.commandStack = new StackImpl<>();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>(this.BTree);
        this.BTree.setPersistenceManager(new DocumentPersistenceManager(file));
    }

    /**
     * @param input the document being put
     * @param uri unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException if there is an issue reading input
     * @throws IllegalArgumentException if uri or format are null
     */
    @Override
    public int put(InputStream input, URI uri, DocumentFormat format) throws IOException {
        //does this delete the entry when input is null?
        if(uri == null || format == null){
            throw new IllegalArgumentException("Can't input null URI or format");
        }
        if (format == DocumentFormat.BINARY) {
            Document doc = new DocumentImpl(uri, input.readAllBytes());
            Document old = BTree.put(uri, doc);
            if (old != null) {
                if (old.getDocumentTxt()!= null) {
                    deleteWordsFromTrie(old);
                }
                removeDocFromMinHeap(old);
            }
            insertDocToHeap(doc);
            Function<URI, Boolean> undo = (URI u) -> {
                removeDocFromMinHeap(doc);
                BTree.put(u, old);
                if (old != null) {
                    insertDocToHeap(old);
                    if (old.getDocumentTxt() != null) {
                        addWordsToTrie(old);
                    }
                }
                return true;};
            commandStack.push(new GenericCommand(uri, undo));
            if (old == null) {
                return 0;
            } else {
                return old.hashCode();
            }
        }
        if (format == DocumentFormat.TXT) {
            Scanner s = new Scanner(input).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";
            Document doc = new DocumentImpl(uri, result, null);
            Document old = BTree.put(uri, doc);
            Function<URI, Boolean> undo = (URI u) -> {
                removeDocFromMinHeap(doc);
                BTree.put(u, old);
                if (old != null) {
                    if (old.getDocumentTxt() != null) {
                        addWordsToTrie(old);
                    }
                    insertDocToHeap(old);
                }
                deleteWordsFromTrie(doc);
                return true;};
            commandStack.push(new GenericCommand(uri, undo));
            if (old == null) {
                addWordsToTrie(doc);
                insertDocToHeap(doc);
                return 0;
            } else {
                if (old.getDocumentTxt() != null) {
                    deleteWordsFromTrie(old);
                }
                removeDocFromMinHeap(old);
                insertDocToHeap(doc);
                addWordsToTrie(doc);
                return old.hashCode();
            }
        }
        return -1;
    }

    private void removeDocFromMinHeap (Document doc) {
        if (doc == null) {
            throw new IllegalArgumentException("Can't remove null doc from heap");
        }
        doc.setLastUseTime(Long.MIN_VALUE);
        this.minHeap.reHeapify(doc.getKey());
        URI removed = this.minHeap.remove();
        if (doc.getDocumentTxt() != null) {
            this.currentDocumentBytes -= this.BTree.get(removed).getDocumentTxt().getBytes().length;
        } else {
            this.currentDocumentBytes -= this.BTree.get(removed).getDocumentBinaryData().length;
        }
        this.currentDocumentCount--;
    }

    private void deleteAllCommandReferences (URI uri) {
        StackImpl<Undoable> inverted = new StackImpl<>();
        while (commandStack.size() > 0) {
            if (commandStack.peek() instanceof GenericCommand<?>) {
                GenericCommand<URI> top = (GenericCommand) commandStack.peek();
                if (!uri.equals(top.getTarget())) {
                    inverted.push(commandStack.pop());
                } else {
                    commandStack.pop();
                    break;
                }
            } else {
                CommandSet<URI> top = (CommandSet<URI>)commandStack.peek();
                if (!top.containsTarget(uri)) {
                    inverted.push(commandStack.pop());
                } else {
                    commandStack.pop();
                    CommandSet<URI> modified = new CommandSet<>();
                    for (GenericCommand command : top) {
                        if (!command.getTarget().equals(uri)) {
                            modified.add(command);
                        }
                    }
                    if (modified.size() > 0) {
                        commandStack.push(modified);
                    }
                    break;
                }
            }
        }
        while (inverted.size() > 0) {
            commandStack.push(inverted.pop());
        }
    }
    private void insertDocToHeap (Document doc) {
        if (doc == null) {
            throw new IllegalArgumentException("can't insert null doc to heap");
        }
        if (maxDocCountActivated || maxDocBytesActivated) {
            if (doc.getDocumentBinaryData() != null) {
                if (maxDocBytesActivated && doc.getDocumentBinaryData().length > this.maxDocumentBytes) {
                    throw new IllegalArgumentException("Can't insert a doc with more bytes than the max Bytes allowed");
                }
                if (maxDocCountActivated) {
                    while ((currentDocumentCount > 0) && currentDocumentCount >= maxDocumentCount) {
                        try {
                            removeFromMinHeap();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (maxDocBytesActivated) {
                    while (currentDocumentBytes + doc.getDocumentBinaryData().length > maxDocumentBytes) {
                        try {
                            removeFromMinHeap();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            } else {
                if (maxDocBytesActivated) {
                    if (doc.getDocumentTxt().getBytes().length > this.maxDocumentBytes) {
                        throw new IllegalArgumentException("Can't insert a doc with more bytes than the max Bytes allowed");
                    }
                }
                if (maxDocCountActivated) {
                    while ((currentDocumentCount > 0) && currentDocumentCount >= maxDocumentCount) {
                        try {
                            removeFromMinHeap();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (maxDocBytesActivated) {
                    while (currentDocumentBytes + doc.getDocumentTxt().getBytes().length > maxDocumentBytes) {
                        try {
                            removeFromMinHeap();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        doc.setLastUseTime(nanoTime());
        this.minHeap.insert(doc.getKey());
        this.currentDocumentCount++;
        if (doc.getDocumentTxt() != null) {
            this.currentDocumentBytes += doc.getDocumentTxt().getBytes().length;
        } else {
            this.currentDocumentBytes += doc.getDocumentBinaryData().length;
        }
    }

    private void removeFromMinHeap () throws Exception {
        URI removed = this.minHeap.remove();
        deleteAllCommandReferences(removed);
        if (this.BTree.get(removed).getDocumentTxt() != null) {
            deleteWordsFromTrie(this.BTree.get(removed));
            this.currentDocumentBytes -= this.BTree.get(removed).getDocumentTxt().getBytes().length;
        } else {
            this.currentDocumentBytes -= this.BTree.get(removed).getDocumentBinaryData().length;
        }
        this.currentDocumentCount--;
        this.BTree.moveToDisk(removed);
    }
    private void addWordsToTrie(Document doc) {
        for(String word : doc.getWords()) {
            this.trie.put(word, doc.getKey());
        }
    }

    private void deleteWordsFromTrie(Document doc) {
        for(String word : doc.getWords()) {
            trie.delete(word, doc.getKey());
        }
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document
     */
    @Override
    public Document get(URI uri) {
        Document doc = this.BTree.get(uri);
        if (doc != null) {
            doc.setLastUseTime(nanoTime());
        }
        return doc;
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    @Override
    public boolean delete(URI uri) {
        Document old = this.BTree.get(uri);
        if (old == null) {
            return false;
        }
        deleteWordsFromTrie(old);
        Function<URI, Boolean> undo = (URI u) -> {
            BTree.put(u, old);
            if (old.getDocumentTxt() != null) {
                addWordsToTrie(old);
            }
            insertDocToHeap(old);
            return true;
        };
        commandStack.push(new GenericCommand(uri, undo));
        removeDocFromMinHeap(old);
        this.BTree.put(uri, null);
        return true;
    }

    /**
     * undo the last put or delete command
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    @Override
    public void undo() throws IllegalStateException {
        if (commandStack.peek() == null) {
            throw new IllegalStateException();
        } else {
            commandStack.pop().undo();
        }
    }

    /**
     * undo the last put or delete that was done with the given URI as its key
     * @param uri
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
    @Override
    public void undo(URI uri) throws IllegalStateException {
        StackImpl<Undoable> inverted = new StackImpl<>();
        boolean match = false;
        while(commandStack.size() > 0) {
            if (commandStack.peek() instanceof GenericCommand<?>) {
                GenericCommand<URI> top = (GenericCommand) commandStack.peek();
                if (!uri.equals(top.getTarget())) {
                    inverted.push(commandStack.pop());
                } else {
                    match = true;
                    commandStack.pop().undo();
                    break;
                }
            } else {
                CommandSet<URI> top = (CommandSet<URI>)commandStack.peek();
                if (!top.containsTarget(uri)) {
                    inverted.push(commandStack.pop());
                } else {
                    match = true;
                    top.undo(uri);
                    if (top.size() == 0) {
                        commandStack.pop();
                    }
                    break;
                }
            }
        }
        if (!match) {
            throw new IllegalStateException();
        }
        while (inverted.size() > 0) {
            commandStack.push(inverted.pop());
        }
    }

    /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<Document> search(String keyword) {
        List<URI> docs = this.trie.getAllSorted(keyword, ((URI o1, URI o2) -> {
            if (this.BTree.get(o1).wordCount(keyword) < this.BTree.get(o2).wordCount(keyword)) {
                return 1;
            } else if (this.BTree.get(o1).wordCount(keyword) > this.BTree.get(o2).wordCount(keyword)) {
                return -1;
            } else {
                return 0;
            }
        }));
        List<Document> returnDocs = new ArrayList<>();
        for (URI uri : docs) {
            this.BTree.get(uri).setLastUseTime(nanoTime());
            this.minHeap.reHeapify(uri);
            returnDocs.add(this.BTree.get(uri));
        }
        return returnDocs;
    }

    /**
     * Retrieve all documents whose text starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<Document> searchByPrefix(String keywordPrefix) {
        List<URI> docs = this.trie.getAllWithPrefixSorted(keywordPrefix, ((URI o1, URI o2) -> {
            if (this.BTree.get(o1).wordCount(keywordPrefix) < this.BTree.get(o2).wordCount(keywordPrefix)) {
                return 1;
            } else if (this.BTree.get(o1).wordCount(keywordPrefix) > this.BTree.get(o2).wordCount(keywordPrefix)) {
                return -1;
            } else {
                return 0;
            }
        }));
        List<Document> returnDocs = new ArrayList<>();
        for (URI uri : docs) {
            this.BTree.get(uri).setLastUseTime(nanoTime());
            this.minHeap.reHeapify(uri);
            returnDocs.add(this.BTree.get(uri));
        }
        return returnDocs;
    }

    /**
     * Completely remove any trace of any document which contains the given keyword
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAll(String keyword) {
        Set<URI> deletedDocs = this.trie.deleteAll(keyword);
        CommandSet<URI> commands = new CommandSet<>();
        Set<URI> returnSet = new HashSet<>();
        for (URI uri : deletedDocs) {
            Document doc = this.BTree.get(uri);
            deleteWordsFromTrieExceptKeyword(doc, keyword);
            removeDocFromMinHeap(doc);
            Function<URI, Boolean> undo = (URI u) -> {
                BTree.put(uri, doc);
                    addWordsToTrie(doc);
                    insertDocToHeap(doc);
                return true;
            };
            commands.addCommand(new GenericCommand<URI>(doc.getKey(), undo));
            returnSet.add(doc.getKey());
            this.BTree.put(uri, null);
        }
        this.commandStack.push(commands);
        return returnSet;
    }

    private void deleteWordsFromTrieExceptKeyword(Document doc, String keyword) {
        for(String word : doc.getWords()) {
            if (!word.equals(keyword)) {
                trie.delete(word, doc.getKey());
            }
        }
    }

    /**
     * Completely remove any trace of any document which contains a word that has the given prefix
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<URI> deletedDocs = this.trie.deleteAllWithPrefix(keywordPrefix);
        CommandSet<URI> commands = new CommandSet<>();
        Set<URI> returnSet = new HashSet<>();
        for (URI uri : deletedDocs) {
            Document doc = BTree.get(uri);
            deleteWordsFromTrie(doc);
            removeDocFromMinHeap(doc);
            Function<URI, Boolean> undo = (URI u) -> {
                BTree.put(doc.getKey(), doc);
                addWordsToTrie(doc);
                insertDocToHeap(doc);
                return true;
            };
            commands.addCommand(new GenericCommand<URI>(doc.getKey(), undo));
            returnSet.add(doc.getKey());
            this.BTree.put(doc.getKey(), null);
        }
        this.commandStack.push(commands);
        return returnSet;
    }

    @Override
    public void setMaxDocumentCount(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Can't set to negative");
        }
        this.maxDocumentCount = limit;
        while (currentDocumentCount > maxDocumentCount) {
            try {
                removeFromMinHeap();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        maxDocCountActivated = true;
    }

    @Override
    public void setMaxDocumentBytes(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Can't set to negative");
        }
        this.maxDocumentBytes = limit;
        maxDocBytesActivated = true;
    }
}
