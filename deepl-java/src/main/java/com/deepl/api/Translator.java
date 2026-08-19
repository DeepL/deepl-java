// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

import com.deepl.api.http.HttpResponse;
import com.deepl.api.http.HttpResponseStream;
import com.deepl.api.parsing.Parser;
import com.deepl.api.utils.*;
import com.google.gson.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.*;
import org.jetbrains.annotations.Nullable;

/**
 * Client for the DeepL API. To use the DeepL API, initialize an instance of this class using your
 * DeepL Authentication Key as found in your <a href="https://www.deepl.com/pro-account/">DeepL
 * account</a>.
 */
public class Translator {
  /** Base URL for DeepL API Free accounts. */
  private static final String DEEPL_SERVER_URL_FREE = "https://api-free.deepl.com";
  /** Base URL for DeepL API Pro accounts */
  private static final String DEEPL_SERVER_URL_PRO = "https://api.deepl.com";

  protected final Parser jsonParser = new Parser();
  protected final HttpClientWrapper httpClientWrapper;
  protected final DeepLApiVersion apiVersion;

  /**
   * Initializes a new Translator object using your Authentication Key.
   *
   * <p>Note: This function does not establish a connection to the DeepL API. To check connectivity,
   * use {@link Translator#getUsage()}.
   *
   * @param authKey DeepL Authentication Key as found in your <a
   *     href="https://www.deepl.com/pro-account/">DeepL account</a>.
   * @param options Additional options controlling Translator behaviour.
   * @throws IllegalArgumentException If authKey is invalid.
   * @deprecated Use {@link DeepLClient} instead.
   */
  @Deprecated
  public Translator(String authKey, TranslatorOptions options) throws IllegalArgumentException {
    if (authKey == null || authKey.isEmpty()) {
      throw new IllegalArgumentException("authKey cannot be null or empty");
    }

    String sanitizedAuthKey = authKey.trim();
    this.apiVersion = options.apiVersion;
    String serverUrl =
        (options.getServerUrl() != null)
            ? options.getServerUrl()
            : (isFreeAccountAuthKey(sanitizedAuthKey)
                ? DEEPL_SERVER_URL_FREE
                : DEEPL_SERVER_URL_PRO);

    Map<String, String> headers = new HashMap<>();
    if (options.getHeaders() != null) {
      headers.putAll(options.getHeaders());
    }
    headers.putIfAbsent("Authorization", "DeepL-Auth-Key " + sanitizedAuthKey);
    headers.putIfAbsent(
        "User-Agent",
        constructUserAgentString(options.getSendPlatformInfo(), options.getAppInfo()));

    this.httpClientWrapper =
        new HttpClientWrapper(
            serverUrl, headers, options.getTimeout(), options.getProxy(), options.getMaxRetries());
  }

  /**
   * Initializes a new Translator object using your Authentication Key.
   *
   * <p>Note: This function does not establish a connection to the DeepL API. To check connectivity,
   * use {@link Translator#getUsage()}.
   *
   * @param authKey DeepL Authentication Key as found in your <a
   *     href="https://www.deepl.com/pro-account/">DeepL account</a>.
   * @throws IllegalArgumentException If authKey is invalid.
   * @deprecated Use {@link DeepLClient} instead.
   */
  @Deprecated
  public Translator(String authKey) throws IllegalArgumentException {
    this(authKey, new TranslatorOptions());
  }

  /**
   * Builds the user-agent String which contains platform information.
   *
   * @return A string containing the client library version, java version and operating system.
   */
  private String constructUserAgentString(boolean sendPlatformInfo, AppInfo appInfo) {
    StringBuilder sb = new StringBuilder();
    sb.append("deepl-java/1.17.0");
    if (sendPlatformInfo) {
      sb.append(" (");
      Properties props = System.getProperties();
      sb.append(props.get("os.name") + "-" + props.get("os.version") + "-" + props.get("os.arch"));
      sb.append(") java/");
      sb.append(props.get("java.version"));
    }
    if (appInfo != null) {
      sb.append(" " + appInfo.getAppName() + "/" + appInfo.getAppVersion());
    }
    return sb.toString();
  }

  /**
   * Determines if the given DeepL Authentication Key belongs to an API Free account.
   *
   * @param authKey DeepL Authentication Key as found in your <a
   *     href="https://www.deepl.com/pro-account/">DeepL account</a>.
   * @return <code>true</code> if the Authentication Key belongs to an API Free account, otherwise
   *     <code>false</code>.
   */
  public static boolean isFreeAccountAuthKey(String authKey) {
    return authKey != null && authKey.endsWith(":fx");
  }

  /**
   * Functions the same as {@link Translator#translateText(String, String, String,
   * TextTranslationOptions)} but sourceLang is auto-detected.
   */
  public TextResult translateText(
      String text, String targetLang, @Nullable TextTranslationOptions options)
      throws InterruptedException, DeepLException {
    ArrayList<String> texts = new ArrayList<>();
    texts.add(text);
    return translateText(texts, null, targetLang, options).get(0);
  }

  /**
   * Translate specified text from source language into target language.
   *
   * @param text Text to translate; must not be empty.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Text translated into specified target language, and detected source language.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public TextResult translateText(
      String text,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable TextTranslationOptions options)
      throws InterruptedException, DeepLException {
    ArrayList<String> texts = new ArrayList<>();
    texts.add(text);
    return translateText(texts, sourceLang, targetLang, options).get(0);
  }

  /**
   * Functions the same as {@link Translator#translateText(String, String, String,
   * TextTranslationOptions)} but with default options.
   *
   * @see Translator#translateText(String, String, String, TextTranslationOptions)
   */
  public TextResult translateText(String text, @Nullable String sourceLang, String targetLang)
      throws DeepLException, InterruptedException {
    return translateText(text, sourceLang, targetLang, null);
  }

  /**
   * Functions the same as {@link Translator#translateText(String, String, String,
   * TextTranslationOptions)} but accepts {@link Language} objects for source and target languages,
   * and uses default options.
   *
   * @see Translator#translateText(String, String, String, TextTranslationOptions)
   */
  public TextResult translateText(String text, @Nullable Language sourceLang, Language targetLang)
      throws DeepLException, InterruptedException {
    return translateText(
        text, (sourceLang != null) ? sourceLang.getCode() : null, targetLang.getCode(), null);
  }

  /**
   * Functions the same as {@link Translator#translateText(String, String, String,
   * TextTranslationOptions)} but accepts {@link Language} objects for target language and
   * sourceLang is auto-detected.
   *
   * @see Translator#translateText(String, String, String, TextTranslationOptions)
   */
  public TextResult translateText(
      String text, Language targetLang, @Nullable TextTranslationOptions options)
      throws DeepLException, InterruptedException {
    return translateText(text, null, targetLang.getCode(), options);
  }

  /**
   * Functions the same as {@link Translator#translateText(String, String, String,
   * TextTranslationOptions)} but accepts {@link Language} objects for source and target languages.
   *
   * @see Translator#translateText(String, String, String, TextTranslationOptions)
   */
  public TextResult translateText(
      String text,
      @Nullable Language sourceLang,
      Language targetLang,
      @Nullable TextTranslationOptions options)
      throws DeepLException, InterruptedException {
    return translateText(
        text, (sourceLang != null) ? sourceLang.getCode() : null, targetLang.getCode(), options);
  }

  /**
   * Translate specified texts from source language into target language.
   *
   * @param texts List of texts to translate; each text must not be empty.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return List of texts translated into specified target language, and detected source language.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<TextResult> translateText(
      List<String> texts,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable TextTranslationOptions options)
      throws DeepLException, InterruptedException {
    Iterable<KeyValuePair<String, String>> params =
        createHttpParams(texts, sourceLang, targetLang, options);
    HttpResponse response =
        httpClientWrapper.sendRequestWithBackoff(
            String.format("/%s/translate", this.apiVersion), params);
    checkResponse(response, false, false);
    return jsonParser.parseTextResult(response.getBody());
  }

  /**
   * Functions the same as {@link Translator#translateText(List, String, String,
   * TextTranslationOptions)} but accepts {@link Language} objects for source and target languages,
   * and uses default options.
   *
   * @see Translator#translateText(List, String, String, TextTranslationOptions)
   */
  public List<TextResult> translateText(
      List<String> texts, @Nullable Language sourceLang, Language targetLang)
      throws DeepLException, InterruptedException {
    return translateText(
        texts, (sourceLang != null) ? sourceLang.getCode() : null, targetLang.getCode(), null);
  }

  /**
   * Functions the same as {@link Translator#translateText(List, String, String,
   * TextTranslationOptions)} but accepts {@link Language} objects for source and target languages.
   *
   * @see Translator#translateText(List, String, String, TextTranslationOptions)
   */
  public List<TextResult> translateText(
      List<String> texts,
      @Nullable Language sourceLang,
      Language targetLang,
      @Nullable TextTranslationOptions options)
      throws DeepLException, InterruptedException {
    return translateText(
        texts, (sourceLang != null) ? sourceLang.getCode() : null, targetLang.getCode(), options);
  }

  /**
   * Functions the same as {@link Translator#translateText(List, String, String,
   * TextTranslationOptions)} but uses default options.
   *
   * @see Translator#translateText(List, String, String, TextTranslationOptions)
   */
  public List<TextResult> translateText(
      List<String> texts, @Nullable String sourceLang, String targetLang)
      throws DeepLException, InterruptedException {
    return translateText(texts, sourceLang, targetLang, null);
  }

  /**
   * Retrieves the usage in the current billing period for this DeepL account. This function can
   * also be used to check connectivity with the DeepL API and that the account has access.
   *
   * @return {@link Usage} object containing account usage information.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public Usage getUsage() throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(String.format("/%s/usage", apiVersion));
    checkResponse(response, false, false);
    return jsonParser.parseUsage(response.getBody());
  }

  /**
   * Retrieves the list of supported translation source languages.
   *
   * @return List of {@link Language} objects representing the available translation source
   *     languages.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<Language> getSourceLanguages() throws DeepLException, InterruptedException {
    return getLanguages(LanguageType.Source);
  }

  /**
   * Retrieves the list of supported translation target languages.
   *
   * @return List of {@link Language} objects representing the available translation target
   *     languages.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<Language> getTargetLanguages() throws DeepLException, InterruptedException {
    return getLanguages(LanguageType.Target);
  }

  /**
   * Retrieves the list of supported translation source or target languages.
   *
   * @param languageType The type of languages to retrieve, source or target.
   * @return List of {@link Language} objects representing the available translation source or
   *     target languages.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<Language> getLanguages(LanguageType languageType)
      throws DeepLException, InterruptedException {
    ArrayList<KeyValuePair<String, String>> params = new ArrayList<>();
    if (languageType == LanguageType.Target) {
      params.add(new KeyValuePair<>("type", "target"));
    }
    HttpResponse response =
        httpClientWrapper.sendRequestWithBackoff(
            String.format("/%s/languages", apiVersion), params);
    checkResponse(response, false, false);
    return jsonParser.parseLanguages(response.getBody());
  }

  /**
   * Retrieves the list of supported glossary language pairs. When creating glossaries, the source
   * and target language pair must match one of the available language pairs.
   *
   * @return List of {@link GlossaryLanguagePair} objects representing the available glossary
   *     language pairs.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<GlossaryLanguagePair> getGlossaryLanguages()
      throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(
            String.format("/%s/glossary-language-pairs", apiVersion));
    checkResponse(response, false, false);
    return jsonParser.parseGlossaryLanguageList(response.getBody());
  }

  /**
   * Translate specified document content from source language to target language and store the
   * translated document content to specified stream.
   *
   * @param inputFile File to upload to be translated.
   * @param outputFile File to download translated document to.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Status when document translation completed, this allows the number of billed characters
   *     to be queried.
   * @throws IOException If the output path is occupied or the input file does not exist.
   * @throws DocumentTranslationException If any error occurs while communicating with the DeepL
   *     API, or if the thread is interrupted during execution of this function. The exception
   *     includes the document handle that may be used to retrieve the document.
   */
  public DocumentStatus translateDocument(
      File inputFile,
      File outputFile,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable DocumentTranslationOptions options)
      throws DocumentTranslationException, IOException {
    try {
      if (outputFile.exists()) {
        throw new IOException("File already exists at output path");
      }
      try (InputStream inputStream = new FileInputStream(inputFile);
          OutputStream outputStream = new FileOutputStream(outputFile)) {
        return translateDocument(
            inputStream, inputFile.getName(), outputStream, sourceLang, targetLang, options);
      }
    } catch (Exception exception) {
      outputFile.delete();
      throw exception;
    }
  }

  /**
   * Functions the same as {@link Translator#translateDocument(File, File, String, String,
   * DocumentTranslationOptions)} but uses default options.
   *
   * @see Translator#translateDocument(File, File, String, String, DocumentTranslationOptions)
   */
  public DocumentStatus translateDocument(
      File inputFile, File outputFile, @Nullable String sourceLang, String targetLang)
      throws DocumentTranslationException, IOException {
    return translateDocument(inputFile, outputFile, sourceLang, targetLang, null);
  }

  /**
   * Translate specified document content from source language to target language and store the
   * translated document content to specified stream. On return, input stream will be at end of
   * stream and neither stream will be closed.
   *
   * @param inputStream Stream containing file to upload to be translated.
   * @param fileName Name of the input file. The file extension is used to determine file type.
   * @param outputStream Stream to download translated document to.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Status when document translation completed, this allows the number of billed characters
   *     to be queried.
   * @throws DocumentTranslationException If any error occurs while communicating with the DeepL
   *     API, or if the thread is interrupted during execution of this function. The exception
   *     includes the document handle that may be used to retrieve the document.
   */
  public DocumentStatus translateDocument(
      InputStream inputStream,
      String fileName,
      OutputStream outputStream,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable DocumentTranslationOptions options)
      throws DocumentTranslationException {
    DocumentHandle handle = null;
    try {
      handle = translateDocumentUpload(inputStream, fileName, sourceLang, targetLang, options);
      DocumentStatus status = translateDocumentWaitUntilDone(handle);
      translateDocumentDownload(handle, outputStream);
      return status;
    } catch (Exception exception) {
      throw new DocumentTranslationException(
          "Error occurred during document translation: " + exception.getMessage(),
          exception,
          handle);
    }
  }

  /**
   * Functions the same as {@link Translator#translateDocument(InputStream, String, OutputStream,
   * String, String, DocumentTranslationOptions)} but uses default options.
   *
   * @see Translator#translateDocument(InputStream, String, OutputStream, String, String,
   *     DocumentTranslationOptions)
   */
  public DocumentStatus translateDocument(
      InputStream inputFile,
      String fileName,
      OutputStream outputFile,
      @Nullable String sourceLang,
      String targetLang)
      throws DocumentTranslationException {
    return translateDocument(inputFile, fileName, outputFile, sourceLang, targetLang, null);
  }

  /**
   * Upload document at specified input path for translation from source language to target
   * language. See the <a href= "https://www.deepl.com/docs-api/translating-documents/">DeepL API
   * documentation</a> for the currently supported document types.
   *
   * @param inputFile File containing document to be translated.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Handle associated with the in-progress document translation.
   * @throws IOException If the input file does not exist.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DocumentHandle translateDocumentUpload(
      File inputFile,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable DocumentTranslationOptions options)
      throws DeepLException, IOException, InterruptedException {
    Iterable<KeyValuePair<String, String>> params =
        createHttpParams(sourceLang, targetLang, options);
    try (FileInputStream inputStream = new FileInputStream(inputFile)) {
      HttpResponse response =
          httpClientWrapper.uploadWithBackoff(
              String.format("/%s/document", apiVersion), params, inputFile.getName(), inputStream);
      checkResponse(response, false, false);
      return jsonParser.parseDocumentHandle(response.getBody());
    }
  }

  /**
   * Functions the same as {@link Translator#translateDocumentUpload(File, String, String,
   * DocumentTranslationOptions)} but uses default options.
   *
   * @see Translator#translateDocumentUpload(File, String, String, DocumentTranslationOptions)
   */
  public DocumentHandle translateDocumentUpload(
      File inputFile, @Nullable String sourceLang, String targetLang)
      throws DeepLException, IOException, InterruptedException {
    return translateDocumentUpload(inputFile, sourceLang, targetLang, null);
  }

  /**
   * Upload document at specified input path for translation from source language to target
   * language. See the <a href= "https://www.deepl.com/docs-api/translating-documents/">DeepL API
   * documentation</a> for the currently supported document types.
   *
   * @param inputStream Stream containing document to be translated. On return, input stream will be
   *     at end of stream and will not be closed.
   * @param fileName Name of the input file. The file extension is used to determine file type.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Handle associated with the in-progress document translation.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DocumentHandle translateDocumentUpload(
      InputStream inputStream,
      String fileName,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable DocumentTranslationOptions options)
      throws DeepLException, InterruptedException {
    Iterable<KeyValuePair<String, String>> params =
        createHttpParams(sourceLang, targetLang, options);
    HttpResponse response =
        httpClientWrapper.uploadWithBackoff(
            String.format("/%s/document/", apiVersion), params, fileName, inputStream);
    checkResponse(response, false, false);
    return jsonParser.parseDocumentHandle(response.getBody());
  }

  /**
   * Functions the same as {@link Translator#translateDocumentUpload(InputStream, String, String,
   * String, DocumentTranslationOptions)} but uses default options.
   *
   * @see Translator#translateDocumentUpload(InputStream, String, String, String,
   *     DocumentTranslationOptions)
   */
  public DocumentHandle translateDocumentUpload(
      InputStream inputStream, String fileName, @Nullable String sourceLang, String targetLang)
      throws DeepLException, InterruptedException {
    return translateDocumentUpload(inputStream, fileName, sourceLang, targetLang, null);
  }

  /**
   * Retrieve the status of in-progress document translation associated with specified handle.
   *
   * @param handle Handle associated with document translation to check.
   * @return Status of the document translation.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DocumentStatus translateDocumentStatus(DocumentHandle handle)
      throws DeepLException, InterruptedException {
    ArrayList<KeyValuePair<String, String>> params = new ArrayList<>();
    params.add(new KeyValuePair<>("document_key", handle.getDocumentKey()));
    String relativeUrl = String.format("/%s/document/%s", apiVersion, handle.getDocumentId());
    HttpResponse response = httpClientWrapper.sendRequestWithBackoff(relativeUrl, params);
    checkResponse(response, false, false);
    return jsonParser.parseDocumentStatus(response.getBody());
  }

  /**
   * Checks document translation status and waits until document translation is complete or fails
   * due to an error.
   *
   * @param handle Handle associated with document translation to wait for.
   * @return Status when document translation completed, this allows the number of billed characters
   *     to be queried.
   * @throws InterruptedException If the thread is interrupted while waiting for the document
   *     translation to complete.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DocumentStatus translateDocumentWaitUntilDone(DocumentHandle handle)
      throws InterruptedException, DeepLException {
    DocumentStatus status = translateDocumentStatus(handle);
    while (status.ok() && !status.done()) {
      Thread.sleep(calculateDocumentWaitTimeMillis(status.getSecondsRemaining()));
      status = translateDocumentStatus(handle);
    }

    if (!status.ok()) {
      String message =
          (status.getErrorMessage() != null) ? status.getErrorMessage() : "Unknown error";
      throw new DeepLException(message);
    }
    return status;
  }

  /**
   * Downloads the resulting translated document associated with specified handle to the specified
   * output file. The document translation must be complete i.e. {@link DocumentStatus#done()} for
   * the document status must be <code>true</code>.
   *
   * @param handle Handle associated with document translation to download.
   * @param outputFile File to download translated document to.
   * @throws IOException If the output path is occupied.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void translateDocumentDownload(DocumentHandle handle, File outputFile)
      throws DeepLException, IOException, InterruptedException {
    try {
      if (outputFile.exists()) {
        throw new IOException("File already exists at output path");
      }
      try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
        translateDocumentDownload(handle, outputStream);
      }
    } catch (Exception exception) {
      outputFile.delete();
      throw exception;
    }
  }

  /**
   * Downloads the resulting translated document associated with specified handle to the specified
   * output stream. The document translation must be complete i.e. {@link DocumentStatus#done()} for
   * the document status must be <code>true</code>. The output stream is not closed.
   *
   * @param handle Handle associated with document translation to download.
   * @param outputStream Stream to download translated document to.
   * @throws IOException If an I/O error occurs.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void translateDocumentDownload(DocumentHandle handle, OutputStream outputStream)
      throws DeepLException, IOException, InterruptedException {
    ArrayList<KeyValuePair<String, String>> params = new ArrayList<>();
    params.add(new KeyValuePair<>("document_key", handle.getDocumentKey()));
    String relativeUrl =
        String.format("/%s/document/%s/result", apiVersion, handle.getDocumentId());
    try (HttpResponseStream response = httpClientWrapper.downloadWithBackoff(relativeUrl, params)) {
      checkResponse(response);
      assert response.getBody() != null;
      StreamUtil.transferTo(response.getBody(), outputStream);
    }
  }

  /**
   * Creates a glossary in your DeepL account with the specified details and returns a {@link
   * GlossaryInfo} object with details about the newly created glossary. The glossary can be used in
   * translations to override translations for specific terms (words). The glossary source and
   * target languages must match the languages of translations for which it will be used.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLang Language code of the source terms language.
   * @param targetLang Language code of the target terms language.
   * @param entries Glossary entries to add to the glossary.
   * @return {@link GlossaryInfo} object with details about the newly created glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public GlossaryInfo createGlossary(
      String name, String sourceLang, String targetLang, GlossaryEntries entries)
      throws DeepLException, InterruptedException {
    return createGlossaryInternal(name, sourceLang, targetLang, "tsv", entries.toTsv());
  }

  /**
   * Creates a glossary in your DeepL account with the specified details and returns a {@link
   * GlossaryInfo} object with details about the newly created glossary. The glossary can be used in
   * translations to override translations for specific terms (words). The glossary source and
   * target languages must match the languages of translations for which it will be used.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLang Language code of the source terms language.
   * @param targetLang Language code of the target terms language.
   * @param csvFile File containing CSV content for glossary.
   * @return {@link GlossaryInfo} object with details about the newly created glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   * @throws IOException If an I/O error occurs.
   */
  public GlossaryInfo createGlossaryFromCsv(
      String name, String sourceLang, String targetLang, File csvFile)
      throws DeepLException, InterruptedException, IOException {
    try (FileInputStream stream = new FileInputStream(csvFile)) {
      String csvContent = StreamUtil.readStream(stream);
      return createGlossaryFromCsv(name, sourceLang, targetLang, csvContent);
    }
  }

  /**
   * Creates a glossary in your DeepL account with the specified details and returns a {@link
   * GlossaryInfo} object with details about the newly created glossary. The glossary can be used in
   * translations to override translations for specific terms (words). The glossary source and
   * target languages must match the languages of translations for which it will be used.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLang Language code of the source terms language.
   * @param targetLang Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @return {@link GlossaryInfo} object with details about the newly created glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public GlossaryInfo createGlossaryFromCsv(
      String name, String sourceLang, String targetLang, String csvContent)
      throws DeepLException, InterruptedException {
    return createGlossaryInternal(name, sourceLang, targetLang, "csv", csvContent);
  }

  /**
   * Retrieves information about the glossary with the specified ID and returns a {@link
   * GlossaryInfo} object containing details. This does not retrieve the glossary entries; to
   * retrieve entries use {@link Translator#getGlossaryEntries(String)}
   *
   * @param glossaryId ID of glossary to retrieve.
   * @return {@link GlossaryInfo} object with details about the specified glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public GlossaryInfo getGlossary(String glossaryId) throws DeepLException, InterruptedException {
    String relativeUrl = String.format("/%s/glossaries/%s", apiVersion, glossaryId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, true);
    return jsonParser.parseGlossaryInfo(response.getBody());
  }

  /**
   * Retrieves information about all glossaries and returns an array of {@link GlossaryInfo} objects
   * containing details. This does not retrieve the glossary entries; to retrieve entries use {@link
   * Translator#getGlossaryEntries(String)}
   *
   * @return Array of {@link GlossaryInfo} objects with details about each glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<GlossaryInfo> listGlossaries() throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(String.format("/%s/glossaries", apiVersion));
    checkResponse(response, false, false);
    return jsonParser.parseGlossaryInfoList(response.getBody());
  }

  /**
   * Retrieves the entries containing within the glossary and returns them as a {@link
   * GlossaryEntries}.
   *
   * @param glossary {@link GlossaryInfo} object corresponding to glossary for which to retrieve
   *     entries.
   * @return {@link GlossaryEntries} containing entry pairs of the glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public GlossaryEntries getGlossaryEntries(GlossaryInfo glossary)
      throws DeepLException, InterruptedException {
    return getGlossaryEntries(glossary.getGlossaryId());
  }

  /**
   * Retrieves the entries containing within the glossary with the specified ID and returns them as
   * a {@link GlossaryEntries}.
   *
   * @param glossaryId ID of glossary for which to retrieve entries.
   * @return {@link GlossaryEntries} containing entry pairs of the glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public GlossaryEntries getGlossaryEntries(String glossaryId)
      throws DeepLException, InterruptedException {
    String relativeUrl = String.format("/%s/glossaries/%s/entries", apiVersion, glossaryId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, true);
    return GlossaryEntries.fromTsv(response.getBody());
  }

  /**
   * Deletes the specified glossary.
   *
   * @param glossary {@link GlossaryInfo} object corresponding to glossary to delete.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteGlossary(GlossaryInfo glossary) throws DeepLException, InterruptedException {
    deleteGlossary(glossary.getGlossaryId());
  }

  /**
   * Deletes the glossary with the specified ID.
   *
   * @param glossaryId ID of glossary to delete.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteGlossary(String glossaryId) throws DeepLException, InterruptedException {
    String relativeUrl = String.format("/%s/glossaries/%s", apiVersion, glossaryId);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    checkResponse(response, false, true);
  }

  /**
   * Checks the specified texts, languages and options are valid, and returns an iterable of
   * containing the parameters to include in HTTP request.
   *
   * @param texts Iterable of texts to translate.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Iterable of parameters for HTTP request.
   */
  private static ArrayList<KeyValuePair<String, String>> createHttpParams(
      List<String> texts,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable TextTranslationOptions options) {
    ArrayList<KeyValuePair<String, String>> params =
        createHttpParamsCommon(
            sourceLang,
            targetLang,
            options != null ? options.getFormality() : null,
            options != null ? options.getGlossaryId() : null,
            options != null ? options.getGlossaryIds() : null);
    texts.forEach(
        (text) -> {
          if (text.isEmpty()) throw new IllegalArgumentException("text must not be empty");
          params.add(new KeyValuePair<>("text", text));
        });
    // Always send show_billed_characters=1, remove when the API default is changed to true
    params.add(new KeyValuePair<>("show_billed_characters", "1"));

    if (options != null) {
      // Note: formality and glossaryId are added above
      if (options.getSentenceSplittingMode() != null) {
        switch (options.getSentenceSplittingMode()) {
          case Off:
            params.add(new KeyValuePair<>("split_sentences", "0"));
            break;
          case NoNewlines:
            params.add(new KeyValuePair<>("split_sentences", "nonewlines"));
            break;
          case All:
            params.add(new KeyValuePair<>("split_sentences", "1"));
            break;
          default:
            break;
        }
      }
      if (options.isPreserveFormatting()) {
        params.add(new KeyValuePair<>("preserve_formatting", "1"));
      }
      if (options.getContext() != null) {
        params.add(new KeyValuePair<>("context", options.getContext()));
      }
      if (options.getModelType() != null) {
        params.add(new KeyValuePair<>("model_type", options.getModelType()));
      }
      if (options.getTagHandling() != null) {
        params.add(new KeyValuePair<>("tag_handling", options.getTagHandling()));
      }
      if (options.getTagHandlingVersion() != null) {
        params.add(new KeyValuePair<>("tag_handling_version", options.getTagHandlingVersion()));
      }
      if (!options.isOutlineDetection()) {
        params.add(new KeyValuePair<>("outline_detection", "0"));
      }
      if (options.getSplittingTags() != null) {
        params.add(new KeyValuePair<>("splitting_tags", joinTags(options.getSplittingTags())));
      }
      if (options.getNonSplittingTags() != null) {
        params.add(
            new KeyValuePair<>("non_splitting_tags", joinTags(options.getNonSplittingTags())));
      }
      if (options.getIgnoreTags() != null) {
        params.add(new KeyValuePair<>("ignore_tags", joinTags(options.getIgnoreTags())));
      }
      if (options.getStyleId() != null) {
        params.add(new KeyValuePair<>("style_id", options.getStyleId()));
      }
      if (options.getTranslationMemoryId() != null) {
        params.add(new KeyValuePair<>("translation_memory_id", options.getTranslationMemoryId()));
      }
      if (options.getTranslationMemoryThreshold() != null) {
        if (options.getTranslationMemoryId() == null) {
          throw new IllegalArgumentException(
              "translationMemoryThreshold requires translationMemoryId");
        }
        params.add(
            new KeyValuePair<>(
                "translation_memory_threshold",
                options.getTranslationMemoryThreshold().toString()));
      }
      if (options.getCustomInstructions() != null) {
        for (String instruction : options.getCustomInstructions()) {
          params.add(new KeyValuePair<>("custom_instructions", instruction));
        }
      }
      addExtraBodyParameters(params, options.getExtraBodyParameters());
    }
    return params;
  }

  /**
   * Checks the specified languages and document translation options are valid, and returns an
   * iterable of containing the parameters to include in HTTP request.
   *
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Iterable of parameters for HTTP request.
   */
  protected static ArrayList<KeyValuePair<String, String>> createHttpParams(
      String sourceLang, String targetLang, DocumentTranslationOptions options) {
    ArrayList<KeyValuePair<String, String>> params =
        createHttpParamsCommon(
            sourceLang,
            targetLang,
            options != null ? options.getFormality() : null,
            options != null ? options.getGlossaryId() : null,
            options != null ? options.getGlossaryIds() : null);

    if (options != null) {
      if (options.getStyleId() != null) {
        params.add(new KeyValuePair<>("style_id", options.getStyleId()));
      }
      if (options.getTranslationMemoryId() != null) {
        params.add(new KeyValuePair<>("translation_memory_id", options.getTranslationMemoryId()));
      }
      if (options.getTranslationMemoryThreshold() != null) {
        if (options.getTranslationMemoryId() == null) {
          throw new IllegalArgumentException(
              "translationMemoryThreshold requires translationMemoryId");
        }
        params.add(
            new KeyValuePair<>(
                "translation_memory_threshold",
                options.getTranslationMemoryThreshold().toString()));
      }
    }

    addExtraBodyParameters(params, options != null ? options.getExtraBodyParameters() : null);

    return params;
  }

  /**
   * Checks the specified parameters common to both text and document translation are valid, and
   * returns an iterable of containing the parameters to include in HTTP request.
   *
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param formality Formality option for translation.
   * @param glossaryId ID of glossary to use for translation.
   * @param glossaryIds List of glossary IDs to use for translation.
   * @return Iterable of parameters for HTTP request.
   */
  protected static ArrayList<KeyValuePair<String, String>> createHttpParamsCommon(
      @Nullable String sourceLang,
      String targetLang,
      @Nullable Formality formality,
      @Nullable String glossaryId,
      @Nullable List<String> glossaryIds) {
    targetLang = LanguageCode.standardize(targetLang);
    sourceLang = sourceLang == null ? null : LanguageCode.standardize(sourceLang);
    checkValidLanguages(sourceLang, targetLang);

    ArrayList<KeyValuePair<String, String>> params = new ArrayList<>();
    if (sourceLang != null) {
      params.add(new KeyValuePair<>("source_lang", sourceLang));
    }
    params.add(new KeyValuePair<>("target_lang", targetLang));

    if (formality != null) {
      switch (formality) {
        case More:
          params.add(new KeyValuePair<>("formality", "more"));
          break;
        case Less:
          params.add(new KeyValuePair<>("formality", "less"));
          break;
        case PreferMore:
          params.add(new KeyValuePair<>("formality", "prefer_more"));
          break;
        case PreferLess:
          params.add(new KeyValuePair<>("formality", "prefer_less"));
          break;
        case Default:
        default:
          params.add(new KeyValuePair<>("formality", "default"));
          break;
      }
    }

    if (glossaryId != null) {
      if (sourceLang == null) {
        throw new IllegalArgumentException("sourceLang is required if using a glossary");
      }
      params.add(new KeyValuePair<>("glossary_id", glossaryId));
    }

    if (glossaryIds != null && !glossaryIds.isEmpty()) {
      if (glossaryId != null) {
        throw new IllegalArgumentException(
            "glossaryIds cannot be used together with glossaryId; use one or the other");
      }
      if (sourceLang == null) {
        throw new IllegalArgumentException("sourceLang is required if using glossaries");
      }
      if (glossaryIds.size() > 5) {
        throw new IllegalArgumentException("glossaryIds cannot contain more than 5 glossary IDs");
      }
      params.add(new KeyValuePair<>("glossary_ids", String.join(",", glossaryIds)));
    }

    return params;
  }

  /** Combine XML tags with comma-delimiter to be included in HTTP request parameters. */
  private static String joinTags(Iterable<String> tags) {
    return String.join(",", tags);
  }

  /**
   * Adds extra body parameters to the HTTP request parameters. Extra parameters can override
   * existing parameters.
   *
   * @param params List of HTTP parameters to add to.
   * @param extraBodyParameters Map of extra parameters to add (can be null).
   */
  private static void addExtraBodyParameters(
      ArrayList<KeyValuePair<String, String>> params, Map<String, String> extraBodyParameters) {
    if (extraBodyParameters != null) {
      params.removeIf(pair -> extraBodyParameters.containsKey(pair.getKey()));
      for (Map.Entry<String, String> entry : extraBodyParameters.entrySet()) {
        params.add(new KeyValuePair<>(entry.getKey(), entry.getValue()));
      }
    }
  }

  /**
   * Checks the specified source and target language are valid.
   *
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @throws IllegalArgumentException If either language code is invalid.
   */
  protected static void checkValidLanguages(@Nullable String sourceLang, String targetLang)
      throws IllegalArgumentException {
    if (sourceLang != null && sourceLang.isEmpty()) {
      throw new IllegalArgumentException("sourceLang must be null or non-empty");
    }
    if (targetLang.isEmpty()) {
      throw new IllegalArgumentException("targetLang must not be empty");
    }
    switch (targetLang) {
      case "en":
        throw new IllegalArgumentException(
            "targetLang=\"en\" is not allowed, please use \"en-GB\" or \"en-US\" instead");
      case "pt":
        throw new IllegalArgumentException(
            "targetLang=\"pt\" is not allowed, please use \"pt-PT\" or \"pt-BR\" instead");
      default:
        break;
    }
  }

  /** Creates a glossary with given details. */
  private GlossaryInfo createGlossaryInternal(
      String name, String sourceLang, String targetLang, String entriesFormat, String entries)
      throws DeepLException, InterruptedException {
    ArrayList<KeyValuePair<String, String>> params = new ArrayList<>();
    params.add(new KeyValuePair<>("name", name));
    params.add(new KeyValuePair<>("source_lang", sourceLang));
    params.add(new KeyValuePair<>("target_lang", targetLang));
    params.add(new KeyValuePair<>("entries_format", entriesFormat));
    params.add(new KeyValuePair<>("entries", entries));
    HttpResponse response =
        httpClientWrapper.sendRequestWithBackoff(
            String.format("/%s/glossaries", apiVersion), params);
    checkResponse(response, false, false);
    return jsonParser.parseGlossaryInfo(response.getBody());
  }

  /**
   * Functions the same as {@link Translator#checkResponse(HttpResponse, boolean, boolean)} but
   * accepts response stream for document downloads. If the HTTP status code represents failure, the
   * response stream is converted to a String response to throw the appropriate exception.
   *
   * @see Translator#checkResponse(HttpResponse, boolean, boolean)
   */
  private void checkResponse(HttpResponseStream response) throws DeepLException {
    if (response.getCode() >= HttpURLConnection.HTTP_OK
        && response.getCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
      return;
    }
    if (response.getBody() == null) {
      throw new DeepLException("response stream is empty");
    }
    checkResponse(response.toStringResponse(), true, false);
  }

  /**
   * Checks the response HTTP status is OK, otherwise throws corresponding exception.
   *
   * @param response Response received from DeepL API.
   * @param inDocumentDownload True if document download function is used, otherwise false.
   * @param usingGlossary True if a glossary function is used, otherwise false.
   * @throws DeepLException Throws {@link DeepLException} or a derived exception depending on the
   *     type of error.
   */
  protected void checkResponse(
      HttpResponse response, boolean inDocumentDownload, boolean usingGlossary)
      throws DeepLException {
    if (response.getCode() >= 200 && response.getCode() < 300) {
      return;
    }

    String messageSuffix = "";
    String body = response.getBody();
    if (body != null && !body.isEmpty()) {
      try {
        messageSuffix = ", error message: " + jsonParser.parseErrorMessage(body);
      } catch (JsonSyntaxException ignored) {
        messageSuffix = ", response: " + body;
      }
    }

    switch (response.getCode()) {
      case HttpURLConnection.HTTP_BAD_REQUEST:
        throw new DeepLException("Bad request" + messageSuffix);
      case HttpURLConnection.HTTP_FORBIDDEN:
        throw new AuthorizationException("Authorization failure, check auth_key" + messageSuffix);
      case HttpURLConnection.HTTP_NOT_FOUND:
        if (usingGlossary) {
          throw new GlossaryNotFoundException("Glossary not found" + messageSuffix);
        } else {
          throw new NotFoundException("Not found" + messageSuffix);
        }
      case 429:
        throw new TooManyRequestsException(
            "Too many requests, DeepL servers are currently experiencing high load"
                + messageSuffix);
      case 456:
        throw new QuotaExceededException(
            "Quota for this billing period has been exceeded" + messageSuffix);
      case HttpURLConnection.HTTP_UNAVAILABLE:
        {
          if (inDocumentDownload) {
            throw new DocumentNotReadyException("Document not ready" + messageSuffix);
          } else {
            throw new DeepLException("Service unavailable" + messageSuffix);
          }
        }
      default:
        throw new DeepLException("Unknown error" + messageSuffix);
    }
  }

  private int calculateDocumentWaitTimeMillis(Long secondsRemaining) {
    // secondsRemaining is currently unreliable, so just poll equidistantly
    return 5000;
  }
}
