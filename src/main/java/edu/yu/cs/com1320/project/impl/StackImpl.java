package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl <T> implements Stack <T> {

        private Object [] stack = new Object [5];
    private int top = -1;
    public StackImpl() {};
    /**
     * @param element object to add to the Stack
     */
    @Override
    public void push(T element) {
        if (top < stack.length - 1) {
            stack[top + 1] = element;
        } else {
            doubleStack(element);
        }
        top++;
    }

    private void doubleStack(T o) {
        Object [] temp = new Object [stack.length * 2];
        for (int i = 0; i <= top; i++) {
            temp[i] = stack[i];
        }
        temp[top + 1] = o;
        stack = temp;
    }
    /**
     * removes and returns element at the top of the stack
     *
     * @return element at the top of the stack, null if the stack is empty
     */
    @Override
    public T pop() {
        if (top == -1) {
            return null;
        } else {
            Object [] temp = new Object [stack.length];
            for (int i = 0; i < top; i++) {
                temp[i] = stack[i];
            }
            Object oldTop = stack[top];
            stack = temp;
            top--;
            return (T)oldTop;
        }
    }
    /**
     *
     * @return the element at the top of the stack without removing it
     */
    @Override
    public T peek() {
        if (top == -1) {
            return null;
        } else {
            return (T)stack[top];
        }
    }
    /**
     *
     * @return how many elements are currently in the stack
     */
    @Override
    public int size() {
        if (top == -1) {
            return 0;
        } else {
            return (top+1);
        }
    }
}
