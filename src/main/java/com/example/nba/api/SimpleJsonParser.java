package com.example.nba.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser with no external dependencies. It supports the subset of JSON
 * required by the balldontlie API responses (objects, arrays, strings, numbers,
 * booleans, and null). The parser is intentionally small and forgiving enough for
 * lightweight ingestion tasks.
 */
public final class SimpleJsonParser {
    private final String text;
    private int idx;

    public SimpleJsonParser(String text) {
        this.text = text;
        this.idx = 0;
    }

    public Object parse() {
        skipWhitespace();
        Object value = parseValue();
        skipWhitespace();
        if (idx != text.length()) throw new IllegalArgumentException("Trailing characters in JSON");
        return value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (idx >= text.length()) throw new IllegalArgumentException("Unexpected end of input");
        char c = text.charAt(idx);
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't' -> parseTrue();
            case 'f' -> parseFalse();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() {
        expect('{');
        Map<String, Object> obj = new HashMap<>();
        skipWhitespace();
        if (peek('}')) {
            idx++; // consume
            return obj;
        }
        while (true) {
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            obj.put(key, value);
            skipWhitespace();
            if (peek('}')) { idx++; break; }
            expect(',');
        }
        return obj;
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> arr = new ArrayList<>();
        skipWhitespace();
        if (peek(']')) { idx++; return arr; }
        while (true) {
            arr.add(parseValue());
            skipWhitespace();
            if (peek(']')) { idx++; break; }
            expect(',');
        }
        return arr;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (idx < text.length()) {
            char c = text.charAt(idx++);
            if (c == '"') break;
            if (c == '\\') { // escape sequence
                if (idx >= text.length()) throw new IllegalArgumentException("Unterminated escape");
                char e = text.charAt(idx++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (idx + 4 > text.length()) throw new IllegalArgumentException("Bad unicode escape");
                        String hex = text.substring(idx, idx + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        idx += 4;
                    }
                    default -> throw new IllegalArgumentException("Unknown escape: " + e);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Boolean parseTrue() {
        expect('t'); expect('r'); expect('u'); expect('e');
        return Boolean.TRUE;
    }

    private Boolean parseFalse() {
        expect('f'); expect('a'); expect('l'); expect('s'); expect('e');
        return Boolean.FALSE;
    }

    private Object parseNull() {
        expect('n'); expect('u'); expect('l'); expect('l');
        return null;
    }

    private Number parseNumber() {
        int start = idx;
        if (peek('-')) idx++;
        while (idx < text.length() && Character.isDigit(text.charAt(idx))) idx++;
        if (peek('.')) {
            idx++;
            while (idx < text.length() && Character.isDigit(text.charAt(idx))) idx++;
        }
        if (peek('e') || peek('E')) {
            idx++;
            if (peek('+') || peek('-')) idx++;
            while (idx < text.length() && Character.isDigit(text.charAt(idx))) idx++;
        }
        String num = text.substring(start, idx);
        if (num.isEmpty()) throw new IllegalArgumentException("Invalid number at position " + start);
        try {
            if (num.contains(".") || num.contains("e") || num.contains("E")) return Double.parseDouble(num);
            long l = Long.parseLong(num);
            if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) return (int) l;
            return l;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid number: " + num, nfe);
        }
    }

    private void skipWhitespace() {
        while (idx < text.length()) {
            char c = text.charAt(idx);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') idx++;
            else break;
        }
    }

    private boolean peek(char c) {
        return idx < text.length() && text.charAt(idx) == c;
    }

    private void expect(char c) {
        if (idx >= text.length() || text.charAt(idx) != c) {
            throw new IllegalArgumentException("Expected '" + c + "' at position " + idx);
        }
        idx++;
    }
}
