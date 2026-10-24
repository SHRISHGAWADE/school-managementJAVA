package sms.web;

import java.math.BigDecimal;
import java.util.*;

/**
 * Lightweight, zero-dependency JSON parser and serializer.
 * Supports strings, numbers, booleans, null, arrays, and objects.
 */
public final class JsonUtil {

  private JsonUtil() {}

  // ==========================================
  // SERIALIZATION (Java Object -> JSON String)
  // ==========================================

  public static String toJson(Object obj) {
    StringBuilder sb = new StringBuilder();
    serialize(obj, sb);
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private static void serialize(Object obj, StringBuilder sb) {
    if (obj == null) {
      sb.append("null");
    } else if (obj instanceof String s) {
      sb.append('"').append(escape(s)).append('"');
    } else if (obj instanceof Character c) {
      sb.append('"').append(escape(c.toString())).append('"');
    } else if (obj instanceof Boolean || obj instanceof Number) {
      if (obj instanceof BigDecimal bd) {
        sb.append(bd.toPlainString());
      } else {
        sb.append(obj);
      }
    } else if (obj instanceof Map<?, ?> map) {
      sb.append('{');
      boolean first = true;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (!first) sb.append(',');
        first = false;
        sb.append('"').append(escape(String.valueOf(entry.getKey()))).append("\":");
        serialize(entry.getValue(), sb);
      }
      sb.append('}');
    } else if (obj instanceof Iterable<?> iter) {
      sb.append('[');
      boolean first = true;
      for (Object item : iter) {
        if (!first) sb.append(',');
        first = false;
        serialize(item, sb);
      }
      sb.append(']');
    } else if (obj.getClass().isArray()) {
      sb.append('[');
      int len = java.lang.reflect.Array.getLength(obj);
      for (int i = 0; i < len; i++) {
        if (i > 0) sb.append(',');
        serialize(java.lang.reflect.Array.get(obj, i), sb);
      }
      sb.append(']');
    } else {
      // Default to quoted string representation for other types (e.g. enums, dates)
      sb.append('"').append(escape(obj.toString())).append('"');
    }
  }

  public static String escape(String s) {
    if (s == null) return "";
    StringBuilder sb = new StringBuilder(s.length() + 16);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.toString();
  }

  // ==========================================
  // DESERIALIZATION (JSON String -> Java Object)
  // ==========================================

  public static Object parse(String json) {
    if (json == null || json.isBlank()) {
      throw new IllegalArgumentException("JSON text cannot be empty or null.");
    }
    Parser parser = new Parser(json);
    Object result = parser.parseValue();
    parser.skipWhitespace();
    if (!parser.isAtEnd()) {
      throw new IllegalArgumentException(
          "Unexpected trailing token at position " + parser.pos + ": '" + parser.peek() + "'");
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> parseObject(String json) {
    Object val = parse(json);
    if (val instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    throw new IllegalArgumentException("Expected JSON object but got " + (val == null ? "null" : val.getClass().getSimpleName()));
  }

  @SuppressWarnings("unchecked")
  public static List<Object> parseArray(String json) {
    Object val = parse(json);
    if (val instanceof List<?> list) {
      return (List<Object>) list;
    }
    throw new IllegalArgumentException("Expected JSON array but got " + (val == null ? "null" : val.getClass().getSimpleName()));
  }

  private static final class Parser {
    private final String src;
    private final int len;
    private int pos = 0;

    Parser(String src) {
      this.src = src;
      this.len = src.length();
    }

    boolean isAtEnd() {
      return pos >= len;
    }

    char peek() {
      if (isAtEnd()) return '\0';
      return src.charAt(pos);
    }

    char advance() {
      if (isAtEnd()) return '\0';
      return src.charAt(pos++);
    }

    void skipWhitespace() {
      while (!isAtEnd()) {
        char c = peek();
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
          advance();
        } else {
          break;
        }
      }
    }

    Object parseValue() {
      skipWhitespace();
      if (isAtEnd()) {
        throw new IllegalArgumentException("Unexpected end of JSON input.");
      }
      char c = peek();
      return switch (c) {
        case '{' -> parseObject();
        case '[' -> parseArray();
        case '"' -> parseString();
        case 't', 'f' -> parseBoolean();
        case 'n' -> parseNull();
        default -> {
          if (c == '-' || (c >= '0' && c <= '9')) {
            yield parseNumber();
          }
          throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + pos);
        }
      };
    }

    Map<String, Object> parseObject() {
      consume('{');
      Map<String, Object> map = new LinkedHashMap<>();
      skipWhitespace();
      if (peek() == '}') {
        advance();
        return map;
      }
      while (true) {
        skipWhitespace();
        if (peek() != '"') {
          throw new IllegalArgumentException("Expected string key in object at position " + pos);
        }
        String key = parseString();
        skipWhitespace();
        consume(':');
        Object value = parseValue();
        map.put(key, value);
        skipWhitespace();
        char next = peek();
        if (next == '}') {
          advance();
          break;
        } else if (next == ',') {
          advance();
          skipWhitespace();
          if (peek() == '}') {
            throw new IllegalArgumentException("Trailing comma in object at position " + pos);
          }
        } else {
          throw new IllegalArgumentException("Expected ',' or '}' in object at position " + pos + ", got '" + next + "'");
        }
      }
      return map;
    }

    List<Object> parseArray() {
      consume('[');
      List<Object> list = new ArrayList<>();
      skipWhitespace();
      if (peek() == ']') {
        advance();
        return list;
      }
      while (true) {
        Object value = parseValue();
        list.add(value);
        skipWhitespace();
        char next = peek();
        if (next == ']') {
          advance();
          break;
        } else if (next == ',') {
          advance();
          skipWhitespace();
          if (peek() == ']') {
            throw new IllegalArgumentException("Trailing comma in array at position " + pos);
          }
        } else {
          throw new IllegalArgumentException("Expected ',' or ']' in array at position " + pos + ", got '" + next + "'");
        }
      }
      return list;
    }

    String parseString() {
      consume('"');
      StringBuilder sb = new StringBuilder();
      while (!isAtEnd()) {
        char c = advance();
        if (c == '"') {
          return sb.toString();
        }
        if (c == '\\') {
          if (isAtEnd()) {
            throw new IllegalArgumentException("Unterminated escape sequence in string.");
          }
          char esc = advance();
          switch (esc) {
            case '"' -> sb.append('"');
            case '\\' -> sb.append('\\');
            case '/' -> sb.append('/');
            case 'b' -> sb.append('\b');
            case 'f' -> sb.append('\f');
            case 'n' -> sb.append('\n');
            case 'r' -> sb.append('\r');
            case 't' -> sb.append('\t');
            case 'u' -> {
              if (pos + 4 > len) {
                throw new IllegalArgumentException("Incomplete unicode escape sequence.");
              }
              String hex = src.substring(pos, pos + 4);
              pos += 4;
              try {
                int codePoint = Integer.parseInt(hex, 16);
                sb.append((char) codePoint);
              } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid unicode escape: \\u" + hex);
              }
            }
            default -> throw new IllegalArgumentException("Invalid escape character: \\" + esc);
          }
        } else if (c < 0x20) {
          throw new IllegalArgumentException("Unescaped control character (0x" + Integer.toHexString(c) + ") in string.");
        } else {
          sb.append(c);
        }
      }
      throw new IllegalArgumentException("Unterminated string literal.");
    }

    Number parseNumber() {
      int start = pos;
      if (peek() == '-') advance();
      if (!isAtEnd() && peek() == '0') {
        advance();
      } else {
        if (isAtEnd() || peek() < '0' || peek() > '9') {
          throw new IllegalArgumentException("Invalid number format at position " + start);
        }
        while (!isAtEnd() && peek() >= '0' && peek() <= '9') {
          advance();
        }
      }

      boolean isFloating = false;
      if (!isAtEnd() && peek() == '.') {
        isFloating = true;
        advance();
        if (isAtEnd() || peek() < '0' || peek() > '9') {
          throw new IllegalArgumentException("Invalid fractional number format at position " + pos);
        }
        while (!isAtEnd() && peek() >= '0' && peek() <= '9') {
          advance();
        }
      }

      if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
        isFloating = true;
        advance();
        if (!isAtEnd() && (peek() == '+' || peek() == '-')) {
          advance();
        }
        if (isAtEnd() || peek() < '0' || peek() > '9') {
          throw new IllegalArgumentException("Invalid exponent in number at position " + pos);
        }
        while (!isAtEnd() && peek() >= '0' && peek() <= '9') {
          advance();
        }
      }

      String numStr = src.substring(start, pos);
      try {
        if (isFloating) {
          return Double.parseDouble(numStr);
        }
        long val = Long.parseLong(numStr);
        if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
          return (int) val;
        }
        return val;
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Number overflow at position " + start + ": " + numStr);
      }
    }

    Boolean parseBoolean() {
      if (matchWord("true")) return Boolean.TRUE;
      if (matchWord("false")) return Boolean.FALSE;
      throw new IllegalArgumentException("Invalid boolean literal at position " + pos);
    }

    Object parseNull() {
      if (matchWord("null")) return null;
      throw new IllegalArgumentException("Invalid literal at position " + pos);
    }

    boolean matchWord(String word) {
      if (pos + word.length() <= len && src.regionMatches(pos, word, 0, word.length())) {
        pos += word.length();
        return true;
      }
      return false;
    }

    void consume(char expected) {
      skipWhitespace();
      if (isAtEnd()) {
        throw new IllegalArgumentException("Expected '" + expected + "' but reached end of input.");
      }
      char c = advance();
      if (c != expected) {
        throw new IllegalArgumentException("Expected '" + expected + "' at position " + (pos - 1) + ", got '" + c + "'");
      }
    }
  }
}

