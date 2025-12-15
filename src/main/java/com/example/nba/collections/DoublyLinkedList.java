package com.example.nba.collections;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Sentinel-based doubly linked list (custom, no java.util collections inside).
 * Supports O(1) addLast, removeFirst/Last, unlink by node reference.
 */
public final class DoublyLinkedList<E> implements Iterable<E> {

    private static final class Node<E> {
        E item;
        Node<E> prev;
        Node<E> next;
        Node(E item, Node<E> prev, Node<E> next) {
            this.item = item;
            this.prev = prev;
            this.next = next;
        }
    }

    private final Node<E> head; // sentinel
    private final Node<E> tail; // sentinel
    private int size;
    private int modCount;

    public DoublyLinkedList() {
        head = new Node<>(null, null, null);
        tail = new Node<>(null, head, null);
        head.next = tail;
        size = 0;
        modCount = 0;
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void addLast(E e) {
        Objects.requireNonNull(e, "element");
        linkBefore(e, tail);
    }

    public void addFirst(E e) {
        Objects.requireNonNull(e, "element");
        linkBefore(e, head.next);
    }

    public E peekFirst() {
        return isEmpty() ? null : head.next.item;
    }

    public E peekLast() {
        return isEmpty() ? null : tail.prev.item;
    }

    public E removeFirst() {
        if (isEmpty()) throw new NoSuchElementException("empty");
        return unlink(head.next);
    }

    public E removeLast() {
        if (isEmpty()) throw new NoSuchElementException("empty");
        return unlink(tail.prev);
    }

    public boolean removeFirstOccurrence(E e) {
        Objects.requireNonNull(e, "element");
        for (Node<E> n = head.next; n != tail; n = n.next) {
            if (e.equals(n.item)) {
                unlink(n);
                return true;
            }
        }
        return false;
    }

    public boolean contains(E e) {
        Objects.requireNonNull(e, "element");
        for (E x : this) if (e.equals(x)) return true;
        return false;
    }

    private void linkBefore(E e, Node<E> succ) {
        Node<E> pred = succ.prev;
        Node<E> newNode = new Node<>(e, pred, succ);
        pred.next = newNode;
        succ.prev = newNode;
        size++;
        modCount++;
    }

    private E unlink(Node<E> n) {
        Node<E> pred = n.prev;
        Node<E> succ = n.next;
        pred.next = succ;
        succ.prev = pred;
        E item = n.item;
        n.item = null;
        n.prev = null;
        n.next = null;
        size--;
        modCount++;
        return item;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private Node<E> cursor = head.next;
            private Node<E> lastReturned = null;
            private final int expected = modCount;

            private void check() {
                if (expected != modCount) throw new ConcurrentModificationException();
            }

            @Override
            public boolean hasNext() {
                check();
                return cursor != tail;
            }

            @Override
            public E next() {
                check();
                if (cursor == tail) throw new NoSuchElementException();
                lastReturned = cursor;
                cursor = cursor.next;
                return lastReturned.item;
            }

            @Override
            public void remove() {
                check();
                if (lastReturned == null) throw new IllegalStateException();
                DoublyLinkedList.this.unlink(lastReturned);
                lastReturned = null;
            }
        };
    }
}
