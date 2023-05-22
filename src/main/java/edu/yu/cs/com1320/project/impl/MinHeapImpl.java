package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;
import edu.yu.cs.com1320.project.stage5.Document;

import java.net.URI;

import java.util.NoSuchElementException;

public class MinHeapImpl<E extends Comparable<E>> extends MinHeap<E> {
    private BTreeImpl<URI, Document> bTree;
    public MinHeapImpl() {
        elements = (E[]) new Comparable [5];
    }
    public MinHeapImpl (BTreeImpl bTree) {
        elements = (E[]) new Comparable [5];
        this.bTree = bTree;
    }
    @Override
    public void reHeapify(E element) {
        MinHeapImpl<E> restructured = new MinHeapImpl<>(this.bTree);
        for (int i = 1; i <= this.count; i++) {
            restructured.insert(this.elements[i]);
        }
        for(int j = 1; j <= this.count; j++) {
            this.elements[j] = restructured.remove();
        }
    }

    @Override
    protected int getArrayIndex(E element) {
        if (element == null) {
            throw new NoSuchElementException("Can't pass in null element");
        }
        for (int i =1; i <= this.count; i++) {
            if (this.elements[i].equals(element)) {
                return i;
            }
        }
        throw new NoSuchElementException("Heap doesn't contain the given element");
    }

    @Override
    protected void doubleArraySize() {
        E[] doubled = (E[]) new Comparable [elements.length * 2];
        for (int i = 0; i <= this.count; i++) {
            doubled [i] = this.elements [i];
        }
        this.elements = doubled;
    }

    @Override
    protected boolean isGreater(int i, int j) {
        if (this.bTree != null) {
            return this.bTree.get((URI) elements[i]).compareTo(this.bTree.get((URI) elements[j])) > 0;
        } else {
            return elements[i].compareTo(elements[j]) > 0;
        }
    }
}
