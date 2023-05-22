package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DocumentImpl implements Document {
    URI uri;
    String text;
    byte[] binaryData;
    long lastTimeUsed;
    private Map<String, Integer> wordCountMap = new HashMap<String, Integer>();
    public DocumentImpl(URI uri, String txt, Map<String, Integer> wordCountMap) {
        if (txt == null) {
            throw new IllegalArgumentException("txt is null");
        }
        if (txt.length() == 0) {
            throw new IllegalArgumentException("no txt in the string");
        }
        this.uri = uri;
        this.text = txt;
        if (wordCountMap == null) {
            String[] docWords = txt.split(" ");
            for(int i =0; i< docWords.length; i++) {
                String word = new String("");
                for(int j=0; j<docWords[i].length(); j++) {
                    int k = (docWords[i].charAt(j));
                    if ((k > 47 && k < 58) || (k > 64 && k < 91) || (k>96 && k<123)) {
                        word += docWords[i].charAt(j);
                    }
                }
                if (!word.isEmpty()) {
                    this.wordCountMap.merge(word, 1, Integer::sum);
                }
            }
        } else {
            this.wordCountMap = wordCountMap;
        }
    }

    public DocumentImpl(URI uri, byte[] binaryData) {
        if (binaryData == null) {
            throw new IllegalArgumentException("binaryData is null");
        }
        if (binaryData.length == 0) {
            throw new IllegalArgumentException("no data in the array");
        }
        this.uri = uri;
        this.binaryData = binaryData;
    }
    @Override
    public String getDocumentTxt() {
        return this.text;
    }

    @Override
    public byte[] getDocumentBinaryData() {
        return this.binaryData;
    }

    @Override
    public URI getKey() {
        return this.uri;
    }

    @Override
    public int wordCount(String word) {
        if (binaryData != null) {
            return 0;
        }
        if (this.wordCountMap.containsKey(word)) {
            return this.wordCountMap.get(word);
        } else {
            return 0;
        }
    }

    @Override
    public Set<String> getWords() {
        return this.wordCountMap.keySet();
    }

    /**
     * return the last time this document was used, via put/get or via a search result
     * (for stage 4 of project)
     */
    @Override
    public long getLastUseTime() {
        return lastTimeUsed;
    }

    @Override
    public void setLastUseTime(long timeInNanoseconds) {
        this.lastTimeUsed = timeInNanoseconds;
    }

    @Override
    public Map<String, Integer> getWordMap() {
        return this.wordCountMap;
    }

    @Override
    public void setWordMap(Map<String, Integer> wordMap) {
        this.wordCountMap = wordMap;
    }

    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(binaryData);
        return Math.abs(result);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DocumentImpl)) {
            return false;
        }
        DocumentImpl other = (DocumentImpl) obj;
        if (this.hashCode() == other.hashCode()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo (Document o) {
        if (o == null) {
            throw new IllegalArgumentException("Can't compare to null object");
        }
        if (this.lastTimeUsed > o.getLastUseTime()) {
            return 1;
        }
        if (this.lastTimeUsed < o.getLastUseTime()) {
            return -1;
        }
        return 0;
    }
}
