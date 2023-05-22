package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;
import java.util.Comparator;

public class TrieImpl<Value> implements Trie<Value> {

    private static final int alphabetSize = 128; // extended ASCII
    private Node root; // root of trie

    public static class Node<Value>
    {
        protected Set<Value> docURIs = new HashSet<>();
        protected Node[] links = new Node[TrieImpl.alphabetSize];
    }
    public TrieImpl() {
        this.root = new Node();
    }
    @Override
    public void put(String key, Value val) {
        if (key == null) {
            throw new IllegalArgumentException("Can't pass in a null key");
        }
        //deleteAll the value from this key
        if (val == null)
        {
            return;
        }
        else
        {
            this.root = put(this.root, key, val, 0);
        }
    }

    private Node put(Node x, String key, Value val, int d)
    {
        if (x == null)
        {
            x = new Node();
        }
        if (d == key.length())
        {
            x.docURIs.add(val);
            return x;
        }
        char c = key.charAt(d);
        x.links[c] = this.put(x.links[c], key, val, d + 1);
        return x;
    }

    @Override
    public List getAllSorted(String key, Comparator<Value> comparator) {
        if (key == null) {
            throw new IllegalArgumentException("Can't pass in null key");
        }
        if (comparator == null) {
            throw new IllegalArgumentException("Can't pass in null comparator");
        }
        Node x = get(this.root, key, 0);
        if (x == null) {
            return new ArrayList<>();
        }
        if (x.docURIs == null) {
            return new ArrayList();
        }
        List<Value> matches = new ArrayList<Value>(x.docURIs);
        matches.sort(comparator);
        return matches;
    }

    @Override
    public List getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if (prefix == null) {
            throw new IllegalArgumentException("Can't pass in null prefix");
        }
        if (comparator == null) {
            throw new IllegalArgumentException("Can't pass in null comparator");
        }
        Set<Value> wordMatches = new HashSet<>();
        collect(get(this.root, prefix, 0), prefix, wordMatches);
        List<Value> sorted = new ArrayList<>(wordMatches);
        sorted.sort(comparator);
        return sorted;
    }

    private void collect(Node x, String pre, Set<Value> s)
    {
        if (x == null) return;
        if (!x.docURIs.isEmpty()) {
            s.addAll(x.docURIs);
        }
        for (char c = 0; c < alphabetSize; c++)
        collect(x.links[c], pre + c, s);
    }


    private Node get(Node x, String key, int d)
    {
        //link was null - return null, indicating a miss
        if (x == null)
        {
            return null;
        }
        //we've reached the last node in the key,
        //return the node
        if (d == key.length())
        {
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        return this.get(x.links[c], key, d + 1);
    }

    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Can't pass in null key");
        }
        if (prefix == "") {
            return new HashSet<>();
        }
        Set<Value> deleted = new HashSet<>();
        Node preNode = get(this.root, prefix, 0);
        if (preNode == null) {
            return new HashSet<>();
        }
        collectDelete(preNode, prefix, deleted);
        preNode.links = new Node[alphabetSize];
        this.root = deleteAll(this.root, prefix, 0);
        if (this.root == null) {
            this.root = new Node();
        }
        return deleted;
    }


    private void collectDelete(Node x, String pre, Set<Value> s)
    {
        if (x == null) {
            return;
        }
        if (!x.docURIs.isEmpty()) {
            s.addAll(x.docURIs);
            x.docURIs.clear();
        }
        for (char c = 0; c < alphabetSize; c++)
            collectDelete(x.links[c], pre + c, s);
    }


    @Override
    public Set<Value> deleteAll(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Can't pass in null key");
        }
        Set<Value> deleted = new HashSet<>();

        Node x = get(root, key, 0);
        if (x == null) {
            return deleted;
        }
        deleted.addAll(x.docURIs);
        this.root = deleteAll(this.root, key, 0);
        if (this.root == null) {
            this.root = new Node();
        }
        return deleted;
    }

    private Node deleteAll(Node x, String key, int d)
    {
        if (x == null)
        {
            return null;
        }
        //we're at the node to del - set the val to null
        if (d == key.length())
        {
            x.docURIs.clear();
        }
        //continue down the trie to the target node
        else
        {
            char c = key.charAt(d);
            x.links[c] = this.deleteAll(x.links[c], key, d + 1);
        }
        //this node has a val – do nothing, return the node
        if (!x.docURIs.isEmpty())
        {
            return x;
        }
        //remove subtrie rooted at x if it is completely empty
        for (int c = 0; c < alphabetSize; c++)
        {
            if (x.links[c] != null)
            {
                return x; //not empty
            }
        }
        //empty - set this link to null in the parent
        return null;
    }

    /**
     * Remove the given value from the node of the given key (do not remove the value from other nodes in the Trie)
     * @param key
     * @param val
     * @return the value which was deleted. If the key did not contain the given value, return null.
     */
    @Override
    public Value delete(String key, Value val) {
        if (key == null) {
            throw new IllegalArgumentException("can't pass in null key");
        }
        if (val == null) {
            throw new IllegalArgumentException("can't pass in null val");
        }
        Node node = get(this.root, key, 0);
        if (node != null && node.docURIs != null) {
            for (Object doc : node.docURIs) {
                if (val.equals(doc)) {
                    Value deletedDoc = (Value)doc;
                    node.docURIs.remove(doc);
                    if (node.docURIs.isEmpty()) {
                        deleteAll(key);
                    }
                    return deletedDoc;
                }
            }
        }
        return null;
    }
}
