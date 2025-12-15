package com.example.nba;

import com.example.nba.collections.DoublyLinkedList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DoublyLinkedListTest {

    @Test
    void addRemoveMaintainsOrder() {
        DoublyLinkedList<Integer> list = new DoublyLinkedList<>();
        list.addLast(1);
        list.addLast(2);
        list.addFirst(0);

        assertEquals(3, list.size());
        assertEquals(0, list.removeFirst());
        assertEquals(2, list.removeLast());
        assertEquals(1, list.removeFirst());
        assertTrue(list.isEmpty());
    }

    @Test
    void removeFirstOccurrenceWorks() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        list.addLast("a");
        list.addLast("b");
        list.addLast("c");
        assertTrue(list.removeFirstOccurrence("b"));
        assertFalse(list.contains("b"));
        assertEquals(2, list.size());
    }
}
