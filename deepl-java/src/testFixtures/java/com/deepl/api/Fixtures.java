// Copyright 2026 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Test inputs shared by the offline and real-API test suites. */
public final class Fixtures {
  public static final String EXAMPLE_TEXT = "proton beam";

  public static final String EXAMPLE_TMX =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<tmx version=\"1.4\"><header creationtool=\"deepl-java-tests\""
          + " creationtoolversion=\"1\" segtype=\"sentence\" o-tmf=\"plain\""
          + " adminlang=\"en\" srclang=\"en\" datatype=\"plaintext\"/>"
          + "<body><tu><tuv xml:lang=\"en\"><seg>Segment number 1</seg></tuv>"
          + "<tuv xml:lang=\"de\"><seg>Segment Nummer 1</seg></tuv></tu></body></tmx>\n";

  private Fixtures() {}

  public static File writeFile(File file, String content) throws IOException {
    Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    return file;
  }

  public static String readFile(File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
  }
}
