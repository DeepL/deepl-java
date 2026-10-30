// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.deepl.api.utils.*;
import java.util.*;
import java.util.function.BiFunction;
import org.jetbrains.annotations.*;

/** Stores the entries of a glossary. */
public class GlossaryEntries implements Map<String, String> {
  private final Map<String, String> entries = new HashMap<>();

  /** Construct an empty GlossaryEntries. */
  public GlossaryEntries() {}

  /** Initializes a new GlossaryEntries with the entry pairs in the given map. */
  public GlossaryEntries(Map<String, String> entryPairs) {
    this.putAll(entryPairs);
  }

  /**
   * Converts the given tab-separated-value (TSV) string of glossary entries into a new
   * GlossaryEntries object. Whitespace is trimmed from the start and end of each term.
   */
  public static GlossaryEntries fromTsv(String tsv) {
    GlossaryEntries result = new GlossaryEntries();
    String[] lines = tsv.split("(\\r\\n|\\n|\\r)");
    int lineNumber = 0;
    for (String line : lines) {
      ++lineNumber;
      String lineTrimmed = trimWhitespace(line);
      if (lineTrimmed.isEmpty()) {
        continue;
      }
      String[] splitLine = lineTrimmed.split("\\t");
      if (splitLine.length < 2) {
        throw new IllegalArgumentException(
            String.format(
                "Entry on line %d does not contain a term separator: %s", lineNumber, lineTrimmed));
      } else if (splitLine.length > 2) {
        throw new IllegalArgumentException(
            String.format(
                "Entry on line %d contains more than one term separator: %s", lineNumber, line));
      } else {
        String sourceTerm = trimWhitespace(splitLine[0]);
        String targetTerm = trimWhitespace(splitLine[1]);
        if (result.containsKey(sourceTerm)) {
          throw new IllegalArgumentException(
              String.format(
                  "Entry on line %d duplicates source term '%s'", lineNumber, sourceTerm));
        }
        result.put(sourceTerm, targetTerm);
      }
    }

    if (result.entries.isEmpty()) {
      throw new IllegalArgumentException("TSV string contains no valid entries");
    }

    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    GlossaryEntries glossaryEntries = (GlossaryEntries) o;
    return glossaryEntries.entries.equals(entries);
  }

  @Override
  public int size() {
    return entries.size();
  }

  @Override
  public boolean isEmpty() {
    return entries.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return entries.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return entries.containsValue(value);
  }

  @Override
  public String get(Object key) {
    return entries.get(key);
  }

  /**
   * Adds the given source term and target term to the glossary entries. Both terms are trimmed of
   * leading and trailing whitespace, then checked with {@link #validateGlossaryTerm}; the trimmed
   * terms are stored.
   *
   * @param sourceTerm key with which the specified value is to be associated
   * @param targetTerm value to be associated with the specified key
   * @return The previous target term associated with this source term, or null if this source term
   *     was not present.
   */
  public String put(String sourceTerm, String targetTerm) throws IllegalArgumentException {
    String source = checkedTerm(sourceTerm);
    return entries.put(source, checkedTerm(targetTerm));
  }

  /**
   * Replaces each target term with the result of the given function. Every new term is trimmed and
   * checked like in {@link #put}; if any is invalid, no entry is changed.
   */
  @Override
  public void replaceAll(BiFunction<? super String, ? super String, ? extends String> function) {
    Map<String, String> replaced = new HashMap<>();
    for (Map.Entry<String, String> entryPair : entries.entrySet()) {
      replaced.put(
          entryPair.getKey(),
          checkedTerm(function.apply(entryPair.getKey(), entryPair.getValue())));
    }
    entries.putAll(replaced);
  }

  @Override
  public String remove(Object key) {
    return entries.remove(key);
  }

  @Override
  public void putAll(@NotNull Map<? extends String, ? extends String> m) {
    for (Map.Entry<? extends String, ? extends String> entryPair : m.entrySet()) {
      put(entryPair.getKey(), entryPair.getValue());
    }
  }

  @Override
  public void clear() {
    entries.clear();
  }

  @NotNull
  @Override
  public Set<String> keySet() {
    return entries.keySet();
  }

  @NotNull
  @Override
  public Collection<String> values() {
    return entries.values();
  }

  /**
   * Returns a view of the entries. {@link Map.Entry#setValue} trims and checks the new target term
   * like {@link #put}.
   */
  @NotNull
  @Override
  public Set<Entry<String, String>> entrySet() {
    Set<Entry<String, String>> backing = entries.entrySet();
    return new AbstractSet<Entry<String, String>>() {
      @Override
      public @NotNull Iterator<Entry<String, String>> iterator() {
        Iterator<Entry<String, String>> iterator = backing.iterator();
        return new Iterator<Entry<String, String>>() {
          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public Entry<String, String> next() {
            return new CheckedEntry(iterator.next());
          }

          @Override
          public void remove() {
            iterator.remove();
          }
        };
      }

      @Override
      public int size() {
        return backing.size();
      }

      @Override
      public boolean contains(Object o) {
        return backing.contains(o);
      }

      @Override
      public boolean remove(Object o) {
        return backing.remove(o);
      }

      @Override
      public void clear() {
        backing.clear();
      }
    };
  }

  /** An entry of the backing map whose setValue checks the new target term. */
  private static final class CheckedEntry implements Map.Entry<String, String> {
    private final Map.Entry<String, String> entry;

    CheckedEntry(Map.Entry<String, String> entry) {
      this.entry = entry;
    }

    @Override
    public String getKey() {
      return entry.getKey();
    }

    @Override
    public String getValue() {
      return entry.getValue();
    }

    @Override
    public String setValue(String value) {
      return entry.setValue(checkedTerm(value));
    }

    @Override
    public boolean equals(Object o) {
      return entry.equals(o);
    }

    @Override
    public int hashCode() {
      return entry.hashCode();
    }

    @Override
    public String toString() {
      return entry.toString();
    }
  }

  /** Validates the given term and returns it trimmed. */
  private static String checkedTerm(String term) {
    validateGlossaryTerm(term);
    return trimWhitespace(term);
  }

  /**
   * Checks the validity of the given glossary term, for example that it contains no invalid
   * characters. Whitespace at the start and end of the term is ignored. Terms are considered valid
   * if they comprise at least one non-whitespace character, and contain no invalid characters: C0
   * and C1 control characters, and Unicode newlines.
   *
   * @param term String containing term to check.
   */
  public static void validateGlossaryTerm(String term) throws IllegalArgumentException {
    String termTrimmed = trimWhitespace(term);
    if (termTrimmed.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Term '%s' contains no non-whitespace characters", term));
    }
    for (int i = 0; i < termTrimmed.length(); ++i) {
      char ch = termTrimmed.charAt(i);
      if ((ch <= 31) || (128 <= ch && ch <= 159) || ch == '\u2028' || ch == '\u2029') {
        throw new IllegalArgumentException(
            String.format(
                "Term '%s' contains invalid character: '%c' (U+%04X)", term, ch, (int) ch));
      }
    }
  }

  /**
   * Converts the glossary entries to a string containing the entries in tab-separated-value (TSV)
   * format.
   *
   * @return String containing the entries in TSV format.
   */
  public String toTsv() {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, String> entryPair : entries.entrySet()) {
      if (builder.length() > 0) {
        builder.append("\n");
      }
      builder.append(entryPair.getKey()).append("\t").append(entryPair.getValue());
    }
    return builder.toString();
  }

  /**
   * Strips whitespace as defined by {@link Character#isWhitespace} from the beginning and end of
   * the given string, like spoken terms. Implemented here because String.strip() is not in Java 8.
   *
   * @param input String to have whitespace trimmed.
   * @return Input string with whitespace removed from ends.
   */
  private static String trimWhitespace(String input) {
    int start = 0;
    int end = input.length();
    while (start < end && Character.isWhitespace(input.charAt(start))) {
      start++;
    }
    while (end > start && Character.isWhitespace(input.charAt(end - 1))) {
      end--;
    }
    return input.substring(start, end);
  }
}
