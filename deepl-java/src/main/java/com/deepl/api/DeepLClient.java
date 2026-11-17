// Copyright 2025 DeepL SE (https://www.deepl.com)
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
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * Client for the DeepL API. To use the DeepL API, initialize an instance of this class using your
 * DeepL Authentication Key as found in your <a href="https://www.deepl.com/pro-account/">DeepL
 * account</a>.
 *
 * <p>Arguments not marked <code>@Nullable</code> are required: a <code>null</code> one, or a <code>
 * null</code> entry of a list argument, throws {@link NullPointerException} naming the parameter,
 * before any request. Optional arguments and option setters treat <code>null</code> as not given.
 */
public class DeepLClient {

  /** Default MIME type of translation memory (TMX) files. */
  public static final String TRANSLATION_MEMORY_FILE_CONTENT_TYPE = "application/xml";

  /** Time to wait between polls of the status of a translation memory job, in milliseconds. */
  private static final long TRANSLATION_MEMORY_JOB_POLL_INTERVAL_MILLIS = 5000;

  // Set by the translation memory file helpers while they wait, so the base wait reports each
  // fetched job status to them without changing the overridable waitUntilTranslationMemoryJobDone.
  private final ThreadLocal<TranslationMemoryJob[]> fetchedTranslationMemoryJobSink =
      new ThreadLocal<>();

  /** Base URL for DeepL API Free accounts. */
  private static final String DEEPL_SERVER_URL_FREE = "https://api-free.deepl.com";
  /** Base URL for DeepL API Pro accounts */
  private static final String DEEPL_SERVER_URL_PRO = "https://api.deepl.com";

  protected final Parser jsonParser = new Parser();
  protected final HttpClientWrapper httpClientWrapper;
  // Package-private so offline tests can shorten the wait between document status requests.
  int documentPollIntervalMillis = 5000;

  /**
   * Initializes a new DeepLClient object using your Authentication Key.
   *
   * <p>Note: This function does not establish a connection to the DeepL API. To check connectivity,
   * use {@link DeepLClient#getUsage()}.
   *
   * @param authKey DeepL Authentication Key as found in your <a
   *     href="https://www.deepl.com/pro-account/">DeepL account</a>.
   * @throws IllegalArgumentException If authKey is empty or blank.
   * @throws NullPointerException If authKey is <code>null</code>.
   */
  public DeepLClient(String authKey) throws IllegalArgumentException {
    this(authKey, new DeepLClientOptions());
  }

  /**
   * Initializes a new DeepLClient object using your Authentication Key.
   *
   * <p>Note: This function does not establish a connection to the DeepL API. To check connectivity,
   * use {@link DeepLClient#getUsage()}.
   *
   * @param authKey DeepL Authentication Key as found in your <a
   *     href="https://www.deepl.com/pro-account/">DeepL account</a>.
   * @param options Additional options controlling client behaviour, or <code>null</code> for the
   *     default options.
   * @throws IllegalArgumentException If authKey is empty or blank, or if the server URL set in the
   *     options is not an absolute <code>http</code> or <code>https</code> URL with a host.
   * @throws NullPointerException If authKey is <code>null</code>.
   */
  public DeepLClient(String authKey, @Nullable DeepLClientOptions options)
      throws IllegalArgumentException {
    Objects.requireNonNull(authKey, "authKey");
    if (authKey.trim().isEmpty()) {
      throw new IllegalArgumentException("authKey must not be empty");
    }
    if (options == null) {
      options = new DeepLClientOptions();
    }

    String sanitizedAuthKey = authKey.trim();
    String serverUrl =
        (options.getServerUrl() != null)
            ? options.getServerUrl()
            : (isFreeAccountAuthKey(sanitizedAuthKey)
                ? DEEPL_SERVER_URL_FREE
                : DEEPL_SERVER_URL_PRO);

    this.httpClientWrapper =
        ApiClientHelper.createHttpClientWrapper(sanitizedAuthKey, serverUrl, options);
  }

  /**
   * Builds the user-agent String which contains platform information.
   *
   * @return A string containing the client library version, java version and operating system.
   */
  static String constructUserAgentString(boolean sendPlatformInfo, @Nullable AppInfo appInfo) {
    StringBuilder sb = new StringBuilder();
    sb.append("deepl-java/1.18.0");
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
   * @throws NullPointerException If authKey is <code>null</code>.
   */
  public static boolean isFreeAccountAuthKey(String authKey) {
    return Objects.requireNonNull(authKey, "authKey").endsWith(":fx");
  }

  /**
   * Translate specified text from source language into target language.
   *
   * @param text Text to translate; must not be <code>null</code> or empty.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Text translated into specified target language, and detected source language.
   * @throws NullPointerException If the text or targetLang is <code>null</code>; checked before any
   *     request.
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
   * Functions the same as {@link DeepLClient#translateText(String, String, String,
   * TextTranslationOptions)} but with default options.
   *
   * @see DeepLClient#translateText(String, String, String, TextTranslationOptions)
   */
  public TextResult translateText(String text, @Nullable String sourceLang, String targetLang)
      throws DeepLException, InterruptedException {
    return translateText(text, sourceLang, targetLang, null);
  }

  /**
   * Functions the same as {@link DeepLClient#translateText(String, String, String,
   * TextTranslationOptions)} but accepts {@link Language} objects for source and target languages,
   * and uses default options.
   *
   * @see DeepLClient#translateText(String, String, String, TextTranslationOptions)
   */
  public TextResult translateText(String text, @Nullable Language sourceLang, Language targetLang)
      throws DeepLException, InterruptedException {
    return translateText(
        text,
        (sourceLang != null) ? sourceLang.getCode() : null,
        targetLanguageCode(targetLang),
        null);
  }

  /**
   * Functions the same as {@link DeepLClient#translateText(String, String, String,
   * TextTranslationOptions)} but accepts {@link Language} objects for source and target languages.
   *
   * @see DeepLClient#translateText(String, String, String, TextTranslationOptions)
   */
  public TextResult translateText(
      String text,
      @Nullable Language sourceLang,
      Language targetLang,
      @Nullable TextTranslationOptions options)
      throws DeepLException, InterruptedException {
    return translateText(
        text,
        (sourceLang != null) ? sourceLang.getCode() : null,
        targetLanguageCode(targetLang),
        options);
  }

  /**
   * Translate specified texts from source language into target language.
   *
   * @param texts List of texts to translate; neither the list nor a text may be <code>null</code>
   *     or empty.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return List of texts translated into specified target language, and detected source language.
   * @throws NullPointerException If the list, a text or targetLang is <code>null</code>; checked
   *     before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<TextResult> translateText(
      List<String> texts,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable TextTranslationOptions options)
      throws DeepLException, InterruptedException {
    Map<String, Object> requestData =
        createTextTranslationData(texts, sourceLang, targetLang, options);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff(
            "/v2/translate", jsonBodyWithExtraParameters(requestData, options));
    checkResponse(response, false);
    return jsonParser.parseTextResult(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#translateText(List, String, String,
   * TextTranslationOptions)} but accepts {@link Language} objects for source and target languages,
   * and uses default options.
   *
   * @see DeepLClient#translateText(List, String, String, TextTranslationOptions)
   */
  public List<TextResult> translateText(
      List<String> texts, @Nullable Language sourceLang, Language targetLang)
      throws DeepLException, InterruptedException {
    return translateText(
        texts,
        (sourceLang != null) ? sourceLang.getCode() : null,
        targetLanguageCode(targetLang),
        null);
  }

  /**
   * Functions the same as {@link DeepLClient#translateText(List, String, String,
   * TextTranslationOptions)} but accepts {@link Language} objects for source and target languages.
   *
   * @see DeepLClient#translateText(List, String, String, TextTranslationOptions)
   */
  public List<TextResult> translateText(
      List<String> texts,
      @Nullable Language sourceLang,
      Language targetLang,
      @Nullable TextTranslationOptions options)
      throws DeepLException, InterruptedException {
    return translateText(
        texts,
        (sourceLang != null) ? sourceLang.getCode() : null,
        targetLanguageCode(targetLang),
        options);
  }

  /**
   * Functions the same as {@link DeepLClient#translateText(List, String, String,
   * TextTranslationOptions)} but uses default options.
   *
   * @see DeepLClient#translateText(List, String, String, TextTranslationOptions)
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
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff("/v2/usage");
    checkResponse(response, false);
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
    return getLanguages(false);
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
    return getLanguages(true);
  }

  private List<Language> getLanguages(boolean target) throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff("/v3/languages?resource=translate_text");
    checkResponse(response, false);
    return jsonParser.parseV3Languages(response.getBody(), target);
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
        httpClientWrapper.sendGetRequestWithBackoff("/v3/languages?resource=glossary");
    checkResponse(response, false);
    return jsonParser.parseV3GlossaryLanguagePairs(response.getBody());
  }

  /**
   * Translate specified document content from source language to target language and store the
   * translated document content to specified stream.
   *
   * <p>The upload is retried only when the request was not sent: after HTTP 429 or a failure while
   * connecting (for example a DNS resolution failure, a refused connection, an unreachable host or
   * network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param inputFile File to upload to be translated.
   * @param outputFile File to download translated document to.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Status when document translation completed, this allows the number of billed characters
   *     to be queried.
   * @throws IllegalArgumentException If the input file does not exist; checked before any request.
   * @throws NullPointerException If inputFile, outputFile or targetLang is <code>null</code>;
   *     checked before any request, not wrapped.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DocumentTranslationException If the target or source language is empty (before any
   *     request, without document handle), the output path is occupied, a local file cannot be read
   *     or written, or any error occurs while communicating with the DeepL API. The exception
   *     includes the document handle that may be used to retrieve the document.
   */
  public DocumentStatus translateDocument(
      File inputFile,
      File outputFile,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable DocumentTranslationOptions options)
      throws DocumentTranslationException, InterruptedException {
    Objects.requireNonNull(inputFile, "inputFile");
    Objects.requireNonNull(outputFile, "outputFile");
    checkDocumentInputFile(inputFile);
    checkDocumentLanguages(sourceLang, targetLang);
    // Checked before the try block: its handler deletes the output file.
    if (outputFile.exists()) {
      throw new DocumentTranslationException(
          "Error occurred during document translation: File already exists at output path",
          new IOException("File already exists at output path"),
          null);
    }
    try (InputStream inputStream = new FileInputStream(inputFile);
        OutputStream outputStream = new FileOutputStream(outputFile)) {
      return translateDocument(
          inputStream, inputFile.getName(), outputStream, sourceLang, targetLang, options);
    } catch (IOException exception) {
      outputFile.delete();
      throw new DocumentTranslationException(
          "Error occurred during document translation: " + exception.getMessage(), exception, null);
    } catch (Exception exception) {
      outputFile.delete();
      throw exception;
    }
  }

  /**
   * Functions the same as {@link DeepLClient#translateDocument(File, File, String, String,
   * DocumentTranslationOptions)} but uses default options.
   *
   * @see DeepLClient#translateDocument(File, File, String, String, DocumentTranslationOptions)
   */
  public DocumentStatus translateDocument(
      File inputFile, File outputFile, @Nullable String sourceLang, String targetLang)
      throws DocumentTranslationException, InterruptedException {
    return translateDocument(inputFile, outputFile, sourceLang, targetLang, null);
  }

  /**
   * Translate specified document content from source language to target language and store the
   * translated document content to specified stream. On return, input stream will be at end of
   * stream and neither stream will be closed.
   *
   * <p>The upload is retried only when the request was not sent: after HTTP 429 or a failure while
   * connecting (for example a DNS resolution failure, a refused connection, an unreachable host or
   * network, or a connect timeout), but not after a TLS or proxy failure.
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
   * @throws NullPointerException If inputStream, fileName, outputStream or targetLang is <code>null
   *     </code>; checked before any request, not wrapped.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DocumentTranslationException If the target or source language is empty (before any
   *     request, without document handle), or any error occurs while communicating with the DeepL
   *     API. The exception includes the document handle that may be used to retrieve the document.
   */
  public DocumentStatus translateDocument(
      InputStream inputStream,
      String fileName,
      OutputStream outputStream,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable DocumentTranslationOptions options)
      throws DocumentTranslationException, InterruptedException {
    Objects.requireNonNull(inputStream, "inputStream");
    Objects.requireNonNull(fileName, "fileName");
    Objects.requireNonNull(outputStream, "outputStream");
    checkDocumentLanguages(sourceLang, targetLang);
    DocumentHandle handle = null;
    try {
      handle = translateDocumentUpload(inputStream, fileName, sourceLang, targetLang, options);
      DocumentStatus status =
          translateDocumentWaitUntilDone(handle, options != null ? options.getTimeout() : null);
      translateDocumentDownload(handle, outputStream);
      return status;
    } catch (InterruptedException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new DocumentTranslationException(
          "Error occurred during document translation: " + exception.getMessage(),
          exception,
          handle);
    }
  }

  /**
   * Functions the same as {@link DeepLClient#translateDocument(InputStream, String, OutputStream,
   * String, String, DocumentTranslationOptions)} but uses default options.
   *
   * @see DeepLClient#translateDocument(InputStream, String, OutputStream, String, String,
   *     DocumentTranslationOptions)
   */
  public DocumentStatus translateDocument(
      InputStream inputFile,
      String fileName,
      OutputStream outputFile,
      @Nullable String sourceLang,
      String targetLang)
      throws DocumentTranslationException, InterruptedException {
    return translateDocument(inputFile, fileName, outputFile, sourceLang, targetLang, null);
  }

  /**
   * Upload document at specified input path for translation from source language to target
   * language. See the <a href= "https://www.deepl.com/docs-api/translating-documents/">DeepL API
   * documentation</a> for the currently supported document types.
   *
   * <p>The upload is retried only when the request was not sent: after HTTP 429 or a failure while
   * connecting (for example a DNS resolution failure, a refused connection, an unreachable host or
   * network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param inputFile File containing document to be translated.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Handle associated with the in-progress document translation.
   * @throws IllegalArgumentException If the input file does not exist; checked before any request.
   * @throws NullPointerException If inputFile or targetLang is <code>null</code>; checked before
   *     any request.
   * @throws IOException If the input file cannot be read.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DocumentHandle translateDocumentUpload(
      File inputFile,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable DocumentTranslationOptions options)
      throws DeepLException, IOException, InterruptedException {
    Objects.requireNonNull(inputFile, "inputFile");
    checkDocumentInputFile(inputFile);
    Iterable<KeyValuePair<String, String>> params =
        createHttpParams(sourceLang, targetLang, options);
    try (FileInputStream inputStream = new FileInputStream(inputFile)) {
      HttpResponse response =
          httpClientWrapper.uploadWithBackoff(
              "/v2/document", params, uploadFileName(inputFile.getName(), options), inputStream);
      checkResponse(response, false);
      return jsonParser.parseDocumentHandle(response.getBody());
    }
  }

  /**
   * Functions the same as {@link DeepLClient#translateDocumentUpload(File, String, String,
   * DocumentTranslationOptions)} but uses default options.
   *
   * @see DeepLClient#translateDocumentUpload(File, String, String, DocumentTranslationOptions)
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
   * <p>The upload is retried only when the request was not sent: after HTTP 429 or a failure while
   * connecting (for example a DNS resolution failure, a refused connection, an unreachable host or
   * network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param inputStream Stream containing document to be translated. On return, input stream will be
   *     at end of stream and will not be closed.
   * @param fileName Name of the input file. The file extension is used to determine file type.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Handle associated with the in-progress document translation.
   * @throws NullPointerException If inputStream, fileName or targetLang is <code>null</code>;
   *     checked before any request.
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
    Objects.requireNonNull(inputStream, "inputStream");
    Objects.requireNonNull(fileName, "fileName");

    Iterable<KeyValuePair<String, String>> params =
        createHttpParams(sourceLang, targetLang, options);
    HttpResponse response =
        httpClientWrapper.uploadWithBackoff(
            "/v2/document", params, uploadFileName(fileName, options), inputStream);
    checkResponse(response, false);
    return jsonParser.parseDocumentHandle(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#translateDocumentUpload(InputStream, String, String,
   * String, DocumentTranslationOptions)} but uses default options.
   *
   * @see DeepLClient#translateDocumentUpload(InputStream, String, String, String,
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
   * @throws NullPointerException If handle is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DocumentStatus translateDocumentGetStatus(DocumentHandle handle)
      throws DeepLException, InterruptedException {
    Objects.requireNonNull(handle, "handle");

    ArrayList<KeyValuePair<String, String>> params = new ArrayList<>();
    params.add(new KeyValuePair<>("document_key", handle.getDocumentKey()));
    String relativeUrl =
        String.format("/v2/document/%s", encodePathSegment(handle.getDocumentId()));
    HttpResponse response = httpClientWrapper.sendRequestWithBackoff(relativeUrl, params);
    checkResponse(response, false);
    return jsonParser.parseDocumentStatus(response.getBody());
  }

  /**
   * Checks document translation status and waits until document translation is complete or fails
   * due to an error.
   *
   * @param handle Handle associated with document translation to wait for.
   * @return Status when document translation completed, this allows the number of billed characters
   *     to be queried.
   * @throws NullPointerException If handle is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted while waiting for the document
   *     translation to complete.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public DocumentStatus translateDocumentWaitUntilDone(DocumentHandle handle)
      throws InterruptedException, DeepLException {
    return translateDocumentWaitUntilDone(handle, null);
  }

  /**
   * Checks document translation status and waits until document translation is complete, fails due
   * to an error, or the given timeout expires. A status not known to this version of the library
   * counts as in progress, so waiting continues.
   *
   * @param handle Handle associated with document translation to wait for.
   * @param timeout Maximum time to wait, or <code>null</code> to wait without limit.
   * @return Status when document translation completed, this allows the number of billed characters
   *     to be queried.
   * @throws NullPointerException If handle is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted while waiting for the document
   *     translation to complete.
   * @throws DeepLException If the document translation failed, the timeout expired, or any error
   *     occurs while communicating with the DeepL API.
   */
  public DocumentStatus translateDocumentWaitUntilDone(
      DocumentHandle handle, @Nullable Duration timeout)
      throws InterruptedException, DeepLException {
    Objects.requireNonNull(handle, "handle");

    Long timeoutNanos = null;
    if (timeout != null) {
      try {
        timeoutNanos = timeout.toNanos();
      } catch (ArithmeticException exception) {
        // Longer than about 292 years: treated as no limit.
      }
    }
    long deadline = timeoutNanos != null ? System.nanoTime() + timeoutNanos : 0;
    DocumentStatus status = translateDocumentGetStatus(handle);
    while (status.ok() && !status.done()) {
      long sleepMillis = calculateDocumentWaitTimeMillis(status.getSecondsRemaining());
      if (timeoutNanos != null) {
        long remainingMillis = (deadline - System.nanoTime()) / 1_000_000;
        if (remainingMillis <= 0) {
          throw new DeepLException(
              "Timed out after " + timeout + " waiting for the document translation");
        }
        sleepMillis = Math.min(sleepMillis, remainingMillis);
      }
      Thread.sleep(sleepMillis);
      status = translateDocumentGetStatus(handle);
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
   * @throws NullPointerException If handle or outputFile is <code>null</code>; checked before any
   *     request.
   * @throws IOException If the output path is occupied.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void translateDocumentDownload(DocumentHandle handle, File outputFile)
      throws DeepLException, IOException, InterruptedException {
    Objects.requireNonNull(handle, "handle");
    Objects.requireNonNull(outputFile, "outputFile");

    // Checked before the try block: its handler deletes the output file.
    if (outputFile.exists()) {
      throw new IOException("File already exists at output path");
    }
    try {
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
   * @throws NullPointerException If handle or outputStream is <code>null</code>; checked before any
   *     request.
   * @throws IOException If an I/O error occurs while writing to the output stream.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void translateDocumentDownload(DocumentHandle handle, OutputStream outputStream)
      throws DeepLException, IOException, InterruptedException {
    Objects.requireNonNull(handle, "handle");
    Objects.requireNonNull(outputStream, "outputStream");

    ArrayList<KeyValuePair<String, String>> params = new ArrayList<>();
    params.add(new KeyValuePair<>("document_key", handle.getDocumentKey()));
    String relativeUrl =
        String.format("/v2/document/%s/result", encodePathSegment(handle.getDocumentId()));
    httpClientWrapper.downloadWithBackoff(relativeUrl, params, this::checkResponse, outputStream);
  }

  /**
   * Creates a glossary in your DeepL account with the specified details and returns a {@link
   * GlossaryInfo} object with details about the newly created glossary. The glossary can be used in
   * translations to override translations for specific terms (words). The glossary source and
   * target languages must match the languages of translations for which it will be used.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLang Language code of the source terms language.
   * @param targetLang Language code of the target terms language.
   * @param entries Glossary entries to add to the glossary; must not be empty.
   * @return {@link GlossaryInfo} object with details about the newly created glossary.
   * @throws NullPointerException If name, sourceLang, targetLang or entries is <code>null</code>;
   *     checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If name or entries is empty.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public GlossaryInfo createGlossary(
      String name, String sourceLang, String targetLang, GlossaryEntries entries)
      throws DeepLException, InterruptedException {
    validateParameter("name", name);
    Objects.requireNonNull(sourceLang, "sourceLang");
    Objects.requireNonNull(targetLang, "targetLang");
    Objects.requireNonNull(entries, "entries");
    if (entries.isEmpty()) {
      throw new IllegalArgumentException("Parameter entries must not be empty");
    }
    return createGlossaryInternal(name, sourceLang, targetLang, "tsv", entries.toTsv());
  }

  /**
   * Creates a glossary in your DeepL account with the specified details and returns a {@link
   * GlossaryInfo} object with details about the newly created glossary. The glossary can be used in
   * translations to override translations for specific terms (words). The glossary source and
   * target languages must match the languages of translations for which it will be used.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLang Language code of the source terms language.
   * @param targetLang Language code of the target terms language.
   * @param csvFile File containing CSV content for glossary.
   * @return {@link GlossaryInfo} object with details about the newly created glossary.
   * @throws NullPointerException If name, sourceLang, targetLang or csvFile is <code>null</code>;
   *     checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If name is empty.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   * @throws IOException If an I/O error occurs.
   */
  public GlossaryInfo createGlossaryFromCsv(
      String name, String sourceLang, String targetLang, File csvFile)
      throws DeepLException, InterruptedException, IOException {
    Objects.requireNonNull(csvFile, "csvFile");
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
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLang Language code of the source terms language.
   * @param targetLang Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @return {@link GlossaryInfo} object with details about the newly created glossary.
   * @throws NullPointerException If name, sourceLang, targetLang or csvContent is <code>null</code>
   *     ; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If name is empty.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public GlossaryInfo createGlossaryFromCsv(
      String name, String sourceLang, String targetLang, String csvContent)
      throws DeepLException, InterruptedException {
    Objects.requireNonNull(csvContent, "csvContent");
    return createGlossaryInternal(name, sourceLang, targetLang, "csv", csvContent);
  }

  /**
   * Retrieves information about the glossary with the specified ID and returns a {@link
   * GlossaryInfo} object containing details. This does not retrieve the glossary entries; to
   * retrieve entries use {@link DeepLClient#getGlossaryEntries(String)}
   *
   * @param glossaryId ID of glossary to retrieve.
   * @return {@link GlossaryInfo} object with details about the specified glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public GlossaryInfo getGlossary(String glossaryId) throws DeepLException, InterruptedException {
    Objects.requireNonNull(glossaryId, "glossaryId");

    String relativeUrl = String.format("/v2/glossaries/%s", glossaryId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseGlossaryInfo(response.getBody());
  }

  /**
   * Retrieves information about all glossaries and returns an array of {@link GlossaryInfo} objects
   * containing details. This does not retrieve the glossary entries; to retrieve entries use {@link
   * DeepLClient#getGlossaryEntries(String)}
   *
   * @return Array of {@link GlossaryInfo} objects with details about each glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<GlossaryInfo> listGlossaries() throws DeepLException, InterruptedException {
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff("/v2/glossaries");
    checkResponse(response, false);
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
    Objects.requireNonNull(glossary, "glossary");

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
    Objects.requireNonNull(glossaryId, "glossaryId");

    String relativeUrl = String.format("/v2/glossaries/%s/entries", glossaryId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
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
    Objects.requireNonNull(glossary, "glossary");

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
    Objects.requireNonNull(glossaryId, "glossaryId");

    String relativeUrl = String.format("/v2/glossaries/%s", glossaryId);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
  }

  /**
   * Checks the specified texts, languages and options are valid, and returns the fields of the JSON
   * request body, without the extra body parameters.
   *
   * @param texts Iterable of texts to translate.
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language.
   * @param options Options influencing translation.
   * @return Fields of the JSON request body.
   */
  private static Map<String, Object> createTextTranslationData(
      List<String> texts,
      @Nullable String sourceLang,
      String targetLang,
      @Nullable TextTranslationOptions options) {
    String formality = null;
    if (options != null) {
      formality =
          options.getFormalityString() != null
              ? options.getFormalityString()
              : formalityValue(options.getFormality());
    }
    Map<String, Object> params =
        createHttpParamsCommonWithFormalityValue(
            lowerCaseBaseSubtag(sourceLang),
            lowerCaseBaseSubtag(targetLang),
            formality,
            options != null ? options.getGlossaryId() : null,
            options != null ? options.getGlossaryIds() : null,
            false,
            false);
    params.put("text", validatedTexts(texts));
    // Always send show_billed_characters, remove when the API default is changed to true
    params.put("show_billed_characters", true);

    if (options != null) {
      // Note: formality and glossaryId are added above
      if (options.getSentenceSplittingModeString() != null) {
        params.put("split_sentences", options.getSentenceSplittingModeString());
      } else if (options.getSentenceSplittingMode() != null) {
        switch (options.getSentenceSplittingMode()) {
          case Off:
            params.put("split_sentences", "0");
            break;
          case NoNewlines:
            params.put("split_sentences", "nonewlines");
            break;
          case All:
            params.put("split_sentences", "1");
            break;
          default:
            break;
        }
      }
      if (options.isPreserveFormatting()) {
        params.put("preserve_formatting", true);
      }
      if (options.getContext() != null) {
        params.put("context", options.getContext());
      }
      if (options.getModelType() != null) {
        params.put("model_type", options.getModelType());
      }
      if (options.getTagHandling() != null) {
        params.put("tag_handling", options.getTagHandling());
      }
      if (options.getTagHandlingVersion() != null) {
        params.put("tag_handling_version", options.getTagHandlingVersion());
      }
      if (!options.isOutlineDetection()) {
        params.put("outline_detection", false);
      }
      if (options.getSplittingTags() != null) {
        params.put("splitting_tags", splitTags("splittingTags", options.getSplittingTags()));
      }
      if (options.getNonSplittingTags() != null) {
        params.put(
            "non_splitting_tags", splitTags("nonSplittingTags", options.getNonSplittingTags()));
      }
      if (options.getIgnoreTags() != null) {
        params.put("ignore_tags", splitTags("ignoreTags", options.getIgnoreTags()));
      }
      if (options.getStyleId() != null) {
        params.put("style_id", options.getStyleId());
      }
      if (options.getStyleProfileId() != null) {
        params.put("style_profile_id", options.getStyleProfileId());
      }
      if (options.getTranslationMemoryId() != null) {
        params.put("translation_memory_id", options.getTranslationMemoryId());
      }
      if (options.getTranslationMemoryThreshold() != null) {
        if (options.getTranslationMemoryId() == null) {
          throw new IllegalArgumentException(
              "translationMemoryThreshold requires translationMemoryId");
        }
        params.put("translation_memory_threshold", options.getTranslationMemoryThreshold());
      }
      if (options.getCustomInstructions() != null) {
        params.put(
            "custom_instructions", toList("customInstructions", options.getCustomInstructions()));
      }
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
    String formality = null;
    if (options != null) {
      formality =
          options.getFormalityString() != null
              ? options.getFormalityString()
              : formalityValue(options.getFormality());
    }
    Map<String, Object> params =
        createHttpParamsCommonWithFormalityValue(
            lowerCaseBaseSubtag(sourceLang),
            lowerCaseBaseSubtag(targetLang),
            formality,
            options != null ? options.getGlossaryId() : null,
            options != null ? options.getGlossaryIds() : null,
            false,
            // Only document translation requires sourceLang with a glossary.
            true);

    if (options != null) {
      if (options.getStyleId() != null) {
        params.put("style_id", options.getStyleId());
      }
      if (options.getStyleProfileId() != null) {
        params.put("style_profile_id", options.getStyleProfileId());
      }
      if (options.getTranslationMemoryId() != null) {
        params.put("translation_memory_id", options.getTranslationMemoryId());
      }
      if (options.getTranslationMemoryThreshold() != null) {
        if (options.getTranslationMemoryId() == null) {
          throw new IllegalArgumentException(
              "translationMemoryThreshold requires translationMemoryId");
        }
        params.put("translation_memory_threshold", options.getTranslationMemoryThreshold());
      }
      if (options.getOutputFormat() != null) {
        params.put("output_format", options.getOutputFormat());
      }
      if (options.getEnableWatermark() != null) {
        // The API ignores "1" here; only "true" enables the watermark.
        params.put("enable_watermark", options.getEnableWatermark());
      }
    }

    if (options != null && options.getExtraBodyParameters() != null) {
      for (Map.Entry<String, ?> entry : options.getExtraBodyParameters().entrySet()) {
        params.remove(entry.getKey());
        params.put(entry.getKey(), entry.getValue());
      }
    }
    return toFormParams(params);
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
    return toFormParams(
        createHttpParamsCommonWithFormalityValue(
            sourceLang,
            targetLang,
            formalityValue(formality),
            glossaryId,
            glossaryIds,
            true,
            true));
  }

  /**
   * The common translation parameters as request fields; <code>glossary_ids</code> is a list. Form
   * and multipart bodies go through {@link #toFormParams(Map)}.
   */
  private static Map<String, Object> createHttpParamsCommonWithFormalityValue(
      @Nullable String sourceLang,
      String targetLang,
      @Nullable String formality,
      @Nullable String glossaryId,
      @Nullable List<String> glossaryIds,
      boolean standardizeLanguages,
      boolean requireSourceLangForGlossary) {
    checkValidLanguages(sourceLang, targetLang);
    if (standardizeLanguages) {
      targetLang = LanguageCode.standardize(targetLang);
      sourceLang = sourceLang == null ? null : LanguageCode.standardize(sourceLang);
    }

    Map<String, Object> params = new LinkedHashMap<>();
    if (sourceLang != null) {
      params.put("source_lang", sourceLang);
    }
    params.put("target_lang", targetLang);

    if (formality != null) {
      params.put("formality", formality);
    }

    if (glossaryId != null) {
      if (sourceLang == null && requireSourceLangForGlossary) {
        throw new IllegalArgumentException("sourceLang is required if using a glossary");
      }
      params.put("glossary_id", glossaryId);
    }

    if (glossaryIds != null && !glossaryIds.isEmpty()) {
      if (glossaryId != null) {
        throw new IllegalArgumentException(
            "glossaryIds cannot be used together with glossaryId; use one or the other");
      }
      if (sourceLang == null && requireSourceLangForGlossary) {
        throw new IllegalArgumentException("sourceLang is required if using glossaries");
      }
      if (glossaryIds.size() > 5) {
        throw new IllegalArgumentException("glossaryIds cannot contain more than 5 glossary IDs");
      }
      params.put("glossary_ids", new ArrayList<>(glossaryIds));
    }

    return params;
  }

  /**
   * Lower-cases the base subtag of a language code and keeps the rest as given, so <code>"EN-gb"
   * </code> becomes <code>"en-gb"</code> and <code>"zh-Hans"</code> stays <code>"zh-Hans"</code>.
   */
  static @Nullable String lowerCaseBaseSubtag(@Nullable String langCode) {
    if (langCode == null) {
      return null;
    }
    int dash = langCode.indexOf('-');
    return dash < 0
        ? langCode.toLowerCase(Locale.ENGLISH)
        : langCode.substring(0, dash).toLowerCase(Locale.ENGLISH) + langCode.substring(dash);
  }

  /** The wire value of the given formality, or <code>null</code> if it is <code>null</code>. */
  private static @Nullable String formalityValue(@Nullable Formality formality) {
    if (formality == null) {
      return null;
    }
    switch (formality) {
      case More:
        return "more";
      case Less:
        return "less";
      case PreferMore:
        return "prefer_more";
      case PreferLess:
        return "prefer_less";
      case Default:
      default:
        return "default";
    }
  }

  private static List<String> toList(String name, Iterable<String> values) {
    List<String> list = new ArrayList<>();
    for (String value : values) {
      list.add(Objects.requireNonNull(value, name));
    }
    return list;
  }

  /** The tags of the given entries, each split at its commas, as JSON bodies need a list. */
  private static List<String> splitTags(String name, Iterable<String> tags) {
    List<String> list = new ArrayList<>();
    for (String entry : tags) {
      list.addAll(Arrays.asList(Objects.requireNonNull(entry, name).split(",")));
    }
    return list;
  }

  /** Copies the texts to send, rejecting empty ones. */
  private static List<String> validatedTexts(List<String> texts) {
    Objects.requireNonNull(texts, "texts");
    for (String text : texts) {
      Objects.requireNonNull(text, "text");
      if (text.isEmpty()) throw new IllegalArgumentException("text must not be empty");
    }
    return new ArrayList<>(texts);
  }

  /**
   * Converts request fields to form or multipart parameters: a collection is comma-joined, any
   * other value sent as its <code>toString()</code>, and a <code>null</code> value left out.
   */
  private static ArrayList<KeyValuePair<String, String>> toFormParams(Map<String, ?> fields) {
    ArrayList<KeyValuePair<String, String>> params = new ArrayList<>();
    for (Map.Entry<String, ?> entry : fields.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Collection) {
        List<String> items = new ArrayList<>();
        for (Object item : (Collection<?>) value) {
          items.add(String.valueOf(item));
        }
        params.add(new KeyValuePair<>(entry.getKey(), String.join(",", items)));
      } else if (value != null) {
        params.add(new KeyValuePair<>(entry.getKey(), value.toString()));
      }
    }
    return params;
  }

  /**
   * Checks the specified source and target language are valid.
   *
   * @param sourceLang Language code of the input language, or <code>null</code> to use
   *     auto-detection.
   * @param targetLang Language code of the desired output language; must not be <code>null</code>.
   * @throws IllegalArgumentException If either language code is empty.
   * @throws NullPointerException If targetLang is <code>null</code>.
   */
  protected static void checkValidLanguages(@Nullable String sourceLang, String targetLang)
      throws IllegalArgumentException {
    Objects.requireNonNull(targetLang, "targetLang");
    if (sourceLang != null && sourceLang.isEmpty()) {
      throw new IllegalArgumentException("sourceLang must be null or non-empty");
    }
    if (targetLang.isEmpty()) {
      throw new IllegalArgumentException("targetLang must not be empty");
    }
  }

  /** Returns the code of the target language object, which must not be null. */
  private static String targetLanguageCode(Language targetLang) {
    return Objects.requireNonNull(targetLang, "targetLang").getCode();
  }

  /** Creates a glossary with given details. */
  private GlossaryInfo createGlossaryInternal(
      String name, String sourceLang, String targetLang, String entriesFormat, String entries)
      throws DeepLException, InterruptedException {
    validateParameter("name", name);
    Objects.requireNonNull(sourceLang, "sourceLang");
    Objects.requireNonNull(targetLang, "targetLang");
    Map<String, Object> requestData = new LinkedHashMap<>();
    requestData.put("name", name);
    requestData.put("source_lang", sourceLang);
    requestData.put("target_lang", targetLang);
    requestData.put("entries_format", entriesFormat);
    requestData.put("entries", entries);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestRetryingOnlyUnprocessed(
            "/v2/glossaries", jsonParser.getGson().toJson(requestData));
    checkResponse(response, false);
    return jsonParser.parseGlossaryInfo(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#checkResponse(HttpResponse, boolean)} but accepts
   * response stream for document downloads. If the HTTP status code represents failure, the
   * response stream is converted to a String response to throw the appropriate exception.
   *
   * @see DeepLClient#checkResponse(HttpResponse, boolean)
   */
  private void checkResponse(HttpResponseStream response) throws DeepLException {
    if (response.getCode() >= HttpURLConnection.HTTP_OK
        && response.getCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
      return;
    }
    // A response without body is mapped by its status like one with an empty body.
    checkResponse(response.toStringResponse(), true);
  }

  /**
   * Checks the response HTTP status is OK, otherwise throws corresponding exception.
   *
   * @param response Response received from DeepL API.
   * @param inDocumentDownload True if document download function is used, otherwise false.
   * @throws DeepLException Throws {@link DeepLException} or a derived exception depending on the
   *     type of error.
   */
  protected void checkResponse(HttpResponse response, boolean inDocumentDownload)
      throws DeepLException {
    ApiClientHelper.checkResponse(jsonParser, response, inDocumentDownload);
  }

  /** The file name of an upload: the filename option if set, else the given name. */
  private static String uploadFileName(
      String fileName, @Nullable DocumentTranslationOptions options) {
    return options != null && options.getFilename() != null ? options.getFilename() : fileName;
  }

  private static String encodePathSegment(String value) throws DeepLException {
    try {
      return ApiClientHelper.encodeQueryComponent(value);
    } catch (UnsupportedEncodingException exception) {
      throw new DeepLException("Error while URL-encoding request", exception);
    }
  }

  private int calculateDocumentWaitTimeMillis(Long secondsRemaining) {
    // secondsRemaining is currently unreliable, so just poll equidistantly
    return documentPollIntervalMillis;
  }

  /**
   * Improves the given text, optionally in the given writing style or tone.
   *
   * @param text Text to improve, must not be empty.
   * @param targetLang Optional language code the improved text should be in, e.g. "EN-US".
   * @param options Optional {@link TextRephraseOptions}, may be <code>null</code>.
   * @throws IllegalArgumentException If text is empty.
   * @throws NullPointerException If text is <code>null</code>; checked before any request.
   * @throws DeepLException If the request fails or the response contains no improvement.
   */
  public WriteResult rephraseText(
      String text, @Nullable String targetLang, @Nullable TextRephraseOptions options)
      throws InterruptedException, DeepLException {
    ArrayList<String> texts = new ArrayList<>();
    texts.add(text);
    return this.rephraseText(texts, targetLang, options).get(0);
  }

  /**
   * Improves the given texts, optionally in the given writing style or tone.
   *
   * @param texts Texts to improve, none may be empty.
   * @param targetLang Optional language code the improved texts should be in, e.g. "EN-US".
   * @param options Optional {@link TextRephraseOptions}, may be <code>null</code>.
   * @return One {@link WriteResult} per input text, in the same order.
   * @throws IllegalArgumentException If a text is empty.
   * @throws NullPointerException If the list or a text is <code>null</code>; checked before any
   *     request.
   * @throws DeepLException If the request fails or the response does not contain one improvement
   *     per input text.
   */
  public List<WriteResult> rephraseText(
      List<String> texts, @Nullable String targetLang, @Nullable TextRephraseOptions options)
      throws InterruptedException, DeepLException {
    Map<String, Object> requestData = createWriteRequestData(texts, targetLang, options);
    return sendWriteRequest(
        "rephrase", jsonBodyWithExtraParameters(requestData, options), texts.size());
  }

  /**
   * Corrects grammar, spelling and punctuation of the given text without rewriting it.
   *
   * @param text Text to correct, must not be empty.
   * @param targetLang Optional language code the corrected text should be in, e.g. "EN-US".
   * @param options Optional {@link CorrectTextOptions}, may be <code>null</code>.
   * @throws IllegalArgumentException If text is empty.
   * @throws NullPointerException If text is <code>null</code>; checked before any request.
   * @throws DeepLException If the request fails or the response contains no correction.
   */
  public WriteResult correctText(
      String text, @Nullable String targetLang, @Nullable CorrectTextOptions options)
      throws InterruptedException, DeepLException {
    ArrayList<String> texts = new ArrayList<>();
    texts.add(text);
    return this.correctText(texts, targetLang, options).get(0);
  }

  /**
   * Corrects grammar, spelling and punctuation of the given texts without rewriting them.
   *
   * @param texts Texts to correct, none may be empty.
   * @param targetLang Optional language code the corrected texts should be in, e.g. "EN-US".
   * @param options Optional {@link CorrectTextOptions}, may be <code>null</code>.
   * @return One {@link WriteResult} per input text, in the same order.
   * @throws IllegalArgumentException If a text is empty.
   * @throws NullPointerException If the list or a text is <code>null</code>; checked before any
   *     request.
   * @throws DeepLException If the request fails or the response does not contain one correction per
   *     input text.
   */
  public List<WriteResult> correctText(
      List<String> texts, @Nullable String targetLang, @Nullable CorrectTextOptions options)
      throws InterruptedException, DeepLException {
    Map<String, Object> requestData = createWriteRequestData(texts, targetLang, null);
    return sendWriteRequest(
        "correct", jsonBodyWithExtraParameters(requestData, options), texts.size());
  }

  private List<WriteResult> sendWriteRequest(String operation, String jsonBody, int textCount)
      throws InterruptedException, DeepLException {
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff(
            String.format("/v2/write/%s", operation), jsonBody);
    checkResponse(response, false);
    List<WriteResult> results = jsonParser.parseWriteResult(response.getBody());
    if (results.size() != textCount || results.contains(null)) {
      throw new DeepLException(
          String.format(
              "Failed to parse the response of the DeepL API: expected %d improvements, got %d",
              textCount, results.size()));
    }
    return results;
  }

  /**
   * Creates a glossary in your DeepL account with the specified details and returns a {@link
   * MultilingualGlossaryInfo} object with details about the newly created glossary. The glossary
   * will contain the glossary dictionaries specified in <paramref name="glossaryDicts" /> each with
   * their own source language, target language and entries.The glossary can be used in translations
   * to override translations for specific terms (words). The glossary must contain a glossary
   * dictionary that matches the languages of translations for which it will be used.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param glossaryDicts {@link MultilingualGlossaryDictionaryInfo} The dictionaries of the
   *     glossary
   * @return {@link MultilingualGlossaryInfo} object with details about the newly created glossary.
   * @throws NullPointerException If name or glossaryDicts is <code>null</code> or contains <code>
   *     null</code>, or a dictionary's entries are <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo createMultilingualGlossary(
      String name, List<MultilingualGlossaryDictionaryEntries> glossaryDicts)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return createMultilingualGlossary(name, glossaryDicts, null);
  }

  /**
   * Functions the same as {@link DeepLClient#createMultilingualGlossary(String, List)} but accepts
   * options; the extra body parameters of the options are added to the request last.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param glossaryDicts The dictionaries of the glossary.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link MultilingualGlossaryInfo} object with details about the newly created glossary.
   * @throws NullPointerException If name or glossaryDicts is <code>null</code> or contains <code>
   *     null</code>, or a dictionary's entries are <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo createMultilingualGlossary(
      String name,
      List<MultilingualGlossaryDictionaryEntries> glossaryDicts,
      @Nullable CreateMultilingualGlossaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    validateParameter("name", name);
    Objects.requireNonNull(glossaryDicts, "glossaryDicts");
    if (glossaryDicts.isEmpty()) {
      throw new IllegalArgumentException("Parameter dictionaries must not be empty");
    }
    List<Map<String, Object>> dictionaries = new ArrayList<>();
    for (MultilingualGlossaryDictionaryEntries dictionary : glossaryDicts) {
      Objects.requireNonNull(dictionary, "glossaryDicts");
      Objects.requireNonNull(dictionary.getEntries(), "entries");
      if (dictionary.getEntries().isEmpty()) {
        throw new IllegalArgumentException("Parameter entries must not be empty");
      }
      dictionaries.add(
          createGlossaryDictionaryData(
              dictionary.getSourceLanguageCode(),
              dictionary.getTargetLanguageCode(),
              entriesTsv(dictionary.getEntries()),
              "tsv"));
    }
    HttpResponse response =
        httpClientWrapper.sendJsonRequestRetryingOnlyUnprocessed(
            "/v3/glossaries", createGlossaryBody(name, dictionaries, options));
    checkResponse(response, false);
    return jsonParser.parseMultilingualGlossaryInfo(response.getBody());
  }

  /**
   * Creates a glossary in your DeepL account with the specified details and returns a {@link
   * MultilingualGlossaryInfo} object with details about the newly created glossary. The glossary
   * will contain a glossary dictionary with the source and target language codes specified and
   * entries created from the <paramref name="csvFile" />. The glossary can be used in translations
   * to override translations for specific terms (words). The glossary must contain a glossary
   * dictionary that matches the languages of translations for which it will be used.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile String containing CSV content.
   * @return {@link MultilingualGlossaryInfo} object with details about the newly created glossary.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws NullPointerException If name, sourceLanguageCode, targetLanguageCode or csvFile is
   *     <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo createMultilingualGlossaryFromCsv(
      String name, String sourceLanguageCode, String targetLanguageCode, String csvFile)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return createMultilingualGlossaryFromCsv(
        name, sourceLanguageCode, targetLanguageCode, csvFile, null);
  }

  /**
   * Functions the same as {@link DeepLClient#createMultilingualGlossaryFromCsv(String, String,
   * String, String)} but accepts options; the extra body parameters of the options are added to the
   * request last.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link MultilingualGlossaryInfo} object with details about the newly created glossary.
   * @throws NullPointerException If name, sourceLanguageCode, targetLanguageCode or csvContent is
   *     <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo createMultilingualGlossaryFromCsv(
      String name,
      String sourceLanguageCode,
      String targetLanguageCode,
      String csvContent,
      @Nullable CreateMultilingualGlossaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return createGlossaryFromCsvInternal(
        name, sourceLanguageCode, targetLanguageCode, csvContent, options);
  }

  /**
   * Creates a glossary in your DeepL account with the specified details and returns a {@link
   * MultilingualGlossaryInfo} object with details about the newly created glossary. The glossary
   * will contain a glossary dictionary with the source and target language codes specified and
   * entries created from the <paramref name="csvFile" />. The glossary can be used in translations
   * to override translations for specific terms (words). The glossary must contain a glossary
   * dictionary that matches the languages of translations for which it will be used.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile String containing CSV content.
   * @return {@link MultilingualGlossaryInfo} object with details about the newly created glossary.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws NullPointerException If name, sourceLanguageCode, targetLanguageCode or csvFile is
   *     <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryInfo createMultilingualGlossaryFromCsv(
      String name, String sourceLanguageCode, String targetLanguageCode, File csvFile)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    return createMultilingualGlossaryFromCsv(
        name, sourceLanguageCode, targetLanguageCode, csvFile, null);
  }

  /**
   * Functions the same as {@link DeepLClient#createMultilingualGlossaryFromCsv(String, String,
   * String, File)} but accepts options; the extra body parameters of the options are added to the
   * request last.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile File containing CSV content.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link MultilingualGlossaryInfo} object with details about the newly created glossary.
   * @throws NullPointerException If name, sourceLanguageCode, targetLanguageCode or csvFile is
   *     <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryInfo createMultilingualGlossaryFromCsv(
      String name,
      String sourceLanguageCode,
      String targetLanguageCode,
      File csvFile,
      @Nullable CreateMultilingualGlossaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    Objects.requireNonNull(csvFile, "csvFile");
    try (FileInputStream stream = new FileInputStream(csvFile)) {
      String csvContent = StreamUtil.readStream(stream);
      return createGlossaryFromCsvInternal(
          name, sourceLanguageCode, targetLanguageCode, csvContent, options);
    }
  }

  /**
   * Retrieves information about the glossary with the specified ID and returns a {@link
   * MultilingualGlossaryInfo} object containing details. This does not retrieve the glossary
   * entries; to retrieve entries use {@link
   * DeepLClient#getMultilingualGlossaryDictionaryEntries(String, String, String)}
   *
   * @param glossaryId ID of glossary to retrieve.
   * @return {@link MultilingualGlossaryInfo} object with details about the specified glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public MultilingualGlossaryInfo getMultilingualGlossary(String glossaryId)
      throws DeepLException, InterruptedException {
    String relativeUrl = multilingualGlossaryPath(glossaryId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseMultilingualGlossaryInfo(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getMultilingualGlossary(String)} but accepts a {@link
   * MultilingualGlossaryInfo} object.
   *
   * @param glossary {@link MultilingualGlossaryInfo} object of the glossary to retrieve.
   * @return {@link MultilingualGlossaryInfo} object with details about the specified glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If glossary has no ID.
   * @throws NullPointerException If glossary is <code>null</code>.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public MultilingualGlossaryInfo getMultilingualGlossary(MultilingualGlossaryInfo glossary)
      throws DeepLException, InterruptedException {
    return getMultilingualGlossary(multilingualGlossaryId(glossary));
  }

  /**
   * Retrieves information about all glossaries and returns an array of {@link
   * MultilingualGlossaryInfo} objects containing details. This does not retrieve the glossary
   * entries; to retrieve entries use {@link
   * DeepLClient#getMultilingualGlossaryDictionaryEntries(String, String, String)}
   *
   * @return List of {@link MultilingualGlossaryInfo} objects with details about each glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<MultilingualGlossaryInfo> listMultilingualGlossaries()
      throws DeepLException, InterruptedException {
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff("/v3/glossaries");
    checkResponse(response, false);
    return jsonParser.parseMultilingualGlossaryInfoList(response.getBody());
  }

  /**
   * For the glossary with the specified ID, retrieves the glossary dictionary with its entries for
   * the given source and target language code pair.
   *
   * @param glossaryId ID of glossary for which to retrieve entries.
   * @param sourceLanguageCode Source language code for the requested glossary dictionary.
   * @param targetLanguageCode Target language code of the requested glossary dictionary.
   * @return {@link MultilingualGlossaryDictionaryEntries} object containing a glossary dictionary
   *     with entries.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryEntries getMultilingualGlossaryDictionaryEntries(
      String glossaryId, String sourceLanguageCode, String targetLanguageCode)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    validateParameter("glossaryId", glossaryId);
    String queryString = createLanguageQueryParams(sourceLanguageCode, targetLanguageCode);
    String relativeUrl = multilingualGlossaryPath(glossaryId) + "/entries" + queryString;
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseMultilingualGlossaryDictionaryEntries(response.getBody());
  }

  /**
   * For the glossary with the specified ID, retrieves the glossary dictionary with its entries for
   * the given {@link MultilingualGlossaryDictionaryInfo} glossary dictionary.
   *
   * @param glossaryId ID of glossary for which to retrieve entries.
   * @param glossaryDict The requested glossary dictionary.
   * @return {@link MultilingualGlossaryDictionaryEntries} object containing a glossary dictionary
   *     with entries.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryEntries getMultilingualGlossaryDictionaryEntries(
      String glossaryId, MultilingualGlossaryDictionaryInfo glossaryDict)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    Objects.requireNonNull(glossaryDict, "glossaryDict");
    return getMultilingualGlossaryDictionaryEntries(
        glossaryId, glossaryDict.getSourceLanguageCode(), glossaryDict.getTargetLanguageCode());
  }

  /**
   * For the specified glossary, retrieves the glossary dictionary with its entries for the given
   * source and target language code pair.
   *
   * @param glossary The glossary for which to retrieve entries.
   * @param sourceLanguageCode Source language code for the requested glossary dictionary.
   * @param targetLanguageCode Target language code of the requested glossary dictionary.
   * @return {@link MultilingualGlossaryDictionaryEntries} object containing a glossary dictionary
   *     with entries.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryEntries getMultilingualGlossaryDictionaryEntries(
      MultilingualGlossaryInfo glossary, String sourceLanguageCode, String targetLanguageCode)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return getMultilingualGlossaryDictionaryEntries(
        multilingualGlossaryId(glossary), sourceLanguageCode, targetLanguageCode);
  }

  /**
   * For the specified glossary, retrieves the glossary dictionary with its entries for the given
   * {@link MultilingualGlossaryDictionaryInfo} glossary dictionary.
   *
   * @param glossary The glossary for which to retrieve entries.
   * @param glossaryDict The requested glossary dictionary.
   * @return {@link MultilingualGlossaryDictionaryEntries} object containing a glossary dictionary
   *     with entries.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryEntries getMultilingualGlossaryDictionaryEntries(
      MultilingualGlossaryInfo glossary, MultilingualGlossaryDictionaryInfo glossaryDict)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    Objects.requireNonNull(glossaryDict, "glossaryDict");
    return getMultilingualGlossaryDictionaryEntries(
        multilingualGlossaryId(glossary),
        glossaryDict.getSourceLanguageCode(),
        glossaryDict.getTargetLanguageCode());
  }

  /**
   * Replaces a glossary dictionary with given entries for the source and target language codes. If
   * no such glossary dictionary exists for that language pair, a new glossary dictionary will be
   * created for that language pair and entries.
   *
   * @param glossaryId The specified ID of the glossary that contains the dictionary to be
   *     replaced/created
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param entries The source-target entry pairs in the new glossary dictionary.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossaryId, sourceLanguageCode, targetLanguageCode or entries
   *     is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionary(
      String glossaryId,
      String sourceLanguageCode,
      String targetLanguageCode,
      GlossaryEntries entries)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceGlossaryDictionaryInternal(
        glossaryId, sourceLanguageCode, targetLanguageCode, entriesTsv(entries), "tsv", null);
  }

  /**
   * Replaces a glossary dictionary with given entries for the source and target language codes. If
   * no such glossary dictionary exists for that language pair, a new glossary dictionary will be
   * created for that language pair and entries.
   *
   * @param glossaryId The specified ID of the glossary that contains the dictionary to be
   *     replaced/created
   * @param glossaryDict The glossary dictionary to replace the existing glossary dictionary for
   *     that source/target language code pair or to be newly created if no such glossary dictionary
   *     exists.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossaryId or glossaryDict is <code>null</code>; checked before
   *     any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionary(
      String glossaryId, MultilingualGlossaryDictionaryEntries glossaryDict)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceMultilingualGlossaryDictionary(glossaryId, glossaryDict, null);
  }

  /**
   * Functions the same as {@link DeepLClient#replaceMultilingualGlossaryDictionary(String,
   * MultilingualGlossaryDictionaryEntries)} but accepts options; the extra body parameters of the
   * options are added to the request last.
   *
   * @param glossaryId The specified ID of the glossary that contains the dictionary to replace.
   * @param glossaryDict The replacement glossary dictionary.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossaryId or glossaryDict is <code>null</code>; checked before
   *     any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionary(
      String glossaryId,
      MultilingualGlossaryDictionaryEntries glossaryDict,
      @Nullable ReplaceMultilingualGlossaryDictionaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    Objects.requireNonNull(glossaryDict, "glossaryDict");
    return replaceGlossaryDictionaryInternal(
        glossaryId,
        glossaryDict.getSourceLanguageCode(),
        glossaryDict.getTargetLanguageCode(),
        entriesTsv(glossaryDict.getEntries()),
        "tsv",
        options);
  }

  /**
   * Functions the same as {@link DeepLClient#replaceMultilingualGlossaryDictionary(String,
   * MultilingualGlossaryDictionaryEntries, ReplaceMultilingualGlossaryDictionaryOptions)} but
   * accepts a {@link MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary that contains the dictionary to replace.
   * @param glossaryDict The replacement glossary dictionary.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossary or glossaryDict is <code>null</code>; checked before
   *     any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary,
      MultilingualGlossaryDictionaryEntries glossaryDict,
      @Nullable ReplaceMultilingualGlossaryDictionaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceMultilingualGlossaryDictionary(
        multilingualGlossaryId(glossary), glossaryDict, options);
  }

  /**
   * Replaces a glossary dictionary with given entries for given glossary dictionary. If no such
   * glossary dictionary exists for that language pair, a new glossary dictionary will be created
   * for that language pair and entries.
   *
   * @param glossary The specified glossary that contains the dictionary to be replaced/created
   * @param glossaryDict The glossary dictionary to replace the existing glossary dictionary for
   *     that source/target language code pair or to be newly created if no such glossary dictionary
   *     exists.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossary or glossaryDict is <code>null</code>; checked before
   *     any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary, MultilingualGlossaryDictionaryEntries glossaryDict)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceMultilingualGlossaryDictionary(
        multilingualGlossaryId(glossary), glossaryDict, null);
  }

  /**
   * Replaces a glossary dictionary with given entries for the source and target language codes. If
   * no such glossary dictionary exists for that language pair, a new glossary dictionary will be
   * created for that language pair and entries.
   *
   * @param glossary The specified glossary that contains the dictionary to be replaced/created
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param entries The source-target entry pairs in the new glossary dictionary.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossary, sourceLanguageCode, targetLanguageCode or entries is
   *     <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary,
      String sourceLanguageCode,
      String targetLanguageCode,
      GlossaryEntries entries)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceGlossaryDictionaryInternal(
        multilingualGlossaryId(glossary),
        sourceLanguageCode,
        targetLanguageCode,
        entriesTsv(entries),
        "tsv",
        null);
  }

  /**
   * Replaces a glossary dictionary with given entries for the source and target language codes. If
   * no such glossary dictionary exists for that language pair, a new glossary dictionary will be
   * created for that language pair and entries specified in the {@code csvFile}.
   *
   * @param glossaryId The specified Id of the glossary that contains the dictionary to be
   *     replaced/created
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile File containing CSV content.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossaryId, sourceLanguageCode, targetLanguageCode or csvFile
   *     is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionaryFromCsv(
      String glossaryId, String sourceLanguageCode, String targetLanguageCode, File csvFile)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    return replaceMultilingualGlossaryDictionaryFromCsv(
        glossaryId, sourceLanguageCode, targetLanguageCode, csvFile, null);
  }

  /**
   * Replaces a glossary dictionary with given entries for the source and target language codes. If
   * no such glossary dictionary exists for that language pair, a new glossary dictionary will be
   * created for that language pair and entries specified in the {@code csvContent}.
   *
   * @param glossaryId The specified ID of the glossary that contains the dictionary to be
   *     replaced/created
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossaryId, sourceLanguageCode, targetLanguageCode or
   *     csvContent is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionaryFromCsv(
      String glossaryId, String sourceLanguageCode, String targetLanguageCode, String csvContent)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceMultilingualGlossaryDictionaryFromCsv(
        glossaryId, sourceLanguageCode, targetLanguageCode, csvContent, null);
  }

  /**
   * Functions the same as {@link DeepLClient#replaceMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, File)} but accepts options; the extra body parameters of the options are added
   * to the request last.
   *
   * @param glossaryId The specified ID for the glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile File containing CSV content.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossaryId, sourceLanguageCode, targetLanguageCode or csvFile
   *     is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionaryFromCsv(
      String glossaryId,
      String sourceLanguageCode,
      String targetLanguageCode,
      File csvFile,
      @Nullable ReplaceMultilingualGlossaryDictionaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    Objects.requireNonNull(csvFile, "csvFile");
    try (FileInputStream stream = new FileInputStream(csvFile)) {
      String csvContent = StreamUtil.readStream(stream);
      return replaceGlossaryDictionaryInternal(
          glossaryId, sourceLanguageCode, targetLanguageCode, csvContent, "csv", options);
    }
  }

  /**
   * Functions the same as {@link DeepLClient#replaceMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, File)} but accepts a {@link MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile File containing CSV content.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossary, sourceLanguageCode, targetLanguageCode or csvFile is
   *     <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionaryFromCsv(
      MultilingualGlossaryInfo glossary,
      String sourceLanguageCode,
      String targetLanguageCode,
      File csvFile)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    return replaceMultilingualGlossaryDictionaryFromCsv(
        multilingualGlossaryId(glossary), sourceLanguageCode, targetLanguageCode, csvFile, null);
  }

  /**
   * Functions the same as {@link DeepLClient#replaceMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, File, ReplaceMultilingualGlossaryDictionaryOptions)} but accepts a {@link
   * MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile File containing CSV content.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossary, sourceLanguageCode, targetLanguageCode or csvFile is
   *     <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionaryFromCsv(
      MultilingualGlossaryInfo glossary,
      String sourceLanguageCode,
      String targetLanguageCode,
      File csvFile,
      @Nullable ReplaceMultilingualGlossaryDictionaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    return replaceMultilingualGlossaryDictionaryFromCsv(
        multilingualGlossaryId(glossary), sourceLanguageCode, targetLanguageCode, csvFile, options);
  }

  /**
   * Functions the same as {@link DeepLClient#replaceMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, String)} but accepts options; the extra body parameters of the options are
   * added to the request last.
   *
   * @param glossaryId The specified ID for the glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossaryId, sourceLanguageCode, targetLanguageCode or
   *     csvContent is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionaryFromCsv(
      String glossaryId,
      String sourceLanguageCode,
      String targetLanguageCode,
      String csvContent,
      @Nullable ReplaceMultilingualGlossaryDictionaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceGlossaryDictionaryInternal(
        glossaryId, sourceLanguageCode, targetLanguageCode, csvContent, "csv", options);
  }

  /**
   * Functions the same as {@link DeepLClient#replaceMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, String)} but accepts a {@link MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossary, sourceLanguageCode, targetLanguageCode or csvContent
   *     is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionaryFromCsv(
      MultilingualGlossaryInfo glossary,
      String sourceLanguageCode,
      String targetLanguageCode,
      String csvContent)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceMultilingualGlossaryDictionaryFromCsv(
        multilingualGlossaryId(glossary), sourceLanguageCode, targetLanguageCode, csvContent, null);
  }

  /**
   * Functions the same as {@link DeepLClient#replaceMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, String, ReplaceMultilingualGlossaryDictionaryOptions)} but accepts a {@link
   * MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link MultilingualGlossaryDictionaryInfo} object with details about the newly replaced
   *     glossary dictionary.
   * @throws NullPointerException If glossary, sourceLanguageCode, targetLanguageCode or csvContent
   *     is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionaryFromCsv(
      MultilingualGlossaryInfo glossary,
      String sourceLanguageCode,
      String targetLanguageCode,
      String csvContent,
      @Nullable ReplaceMultilingualGlossaryDictionaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceMultilingualGlossaryDictionaryFromCsv(
        multilingualGlossaryId(glossary),
        sourceLanguageCode,
        targetLanguageCode,
        csvContent,
        options);
  }

  /**
   * Updates a glossary dictionary with given entries for the source and target language codes. The
   * glossary dictionary must belong to the glossary with the ID specified in <paramref
   * name="glossaryId" />. If a dictionary for the provided language pair already exists, the
   * dictionary entries are merged.
   *
   * @param glossaryId The specified ID of the glossary that contains the dictionary to be
   *     updated/created
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param entries The source-target entry pairs in the new glossary dictionary.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossaryId, sourceLanguageCode, targetLanguageCode or entries
   *     is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionary(
      String glossaryId,
      String sourceLanguageCode,
      String targetLanguageCode,
      GlossaryEntries entries)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateGlossaryDictionaryInternal(
        glossaryId, sourceLanguageCode, targetLanguageCode, entriesTsv(entries), "tsv", null);
  }

  /**
   * Updates a glossary dictionary with given entries for the source and target language codes. The
   * glossary dictionary must belong to the glossary specified in <paramref name="glossary" />. If a
   * dictionary for the provided language pair already exists, the dictionary entries are merged.
   *
   * @param glossary The specified ID for the glossary that contains the dictionary to be
   *     updated/created
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param entries The source-target entry pairs in the new glossary dictionary.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossary, sourceLanguageCode, targetLanguageCode or entries is
   *     <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary,
      String sourceLanguageCode,
      String targetLanguageCode,
      GlossaryEntries entries)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateGlossaryDictionaryInternal(
        multilingualGlossaryId(glossary),
        sourceLanguageCode,
        targetLanguageCode,
        entriesTsv(entries),
        "tsv",
        null);
  }

  /**
   * Updates a glossary dictionary with given glossary dictionary specified in <paramref
   * name="glossaryDict" />. The glossary dictionary must belong to the glossary with the ID
   * specified in <paramref name="glossaryId" />. If a dictionary for the provided language pair
   * already exists, the dictionary entries are merged.
   *
   * @param glossaryId The specified ID of the glossary that contains the dictionary to be
   *     updated/created
   * @param glossaryDict The glossary dictionary to be created/updated
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossaryId or glossaryDict is <code>null</code>; checked before
   *     any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionary(
      String glossaryId, MultilingualGlossaryDictionaryEntries glossaryDict)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateMultilingualGlossaryDictionary(glossaryId, glossaryDict, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryDictionary(String,
   * MultilingualGlossaryDictionaryEntries)} but accepts options; the extra body parameters of the
   * options are added to the request last.
   *
   * @param glossaryId The specified ID of the glossary that contains the dictionary to update.
   * @param glossaryDict The glossary dictionary whose entries are merged into the glossary.
   * @param options Options for the request, or <code>null</code>; only its extra body parameters
   *     are read.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossaryId or glossaryDict is <code>null</code>; checked before
   *     any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionary(
      String glossaryId,
      MultilingualGlossaryDictionaryEntries glossaryDict,
      @Nullable BaseRequestOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    Objects.requireNonNull(glossaryDict, "glossaryDict");
    return updateGlossaryDictionaryInternal(
        glossaryId,
        glossaryDict.getSourceLanguageCode(),
        glossaryDict.getTargetLanguageCode(),
        entriesTsv(glossaryDict.getEntries()),
        "tsv",
        options);
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryDictionary(String,
   * MultilingualGlossaryDictionaryEntries, BaseRequestOptions)} but accepts a {@link
   * MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary that contains the dictionary to update.
   * @param glossaryDict The glossary dictionary whose entries are merged into the glossary.
   * @param options Options for the request, or <code>null</code>; only its extra body parameters
   *     are read.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossary or glossaryDict is <code>null</code>; checked before
   *     any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary,
      MultilingualGlossaryDictionaryEntries glossaryDict,
      @Nullable BaseRequestOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateMultilingualGlossaryDictionary(
        multilingualGlossaryId(glossary), glossaryDict, options);
  }

  /**
   * Updates a glossary dictionary with given entries for the source and target language codes. If a
   * dictionary for the provided language pair already exists, the dictionary entries are merged.
   *
   * @param glossary The specified glossary that contains the dictionary to be updated/created
   * @param glossaryDict The glossary dictionary to be created/updated
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossary or glossaryDict is <code>null</code>; checked before
   *     any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary, MultilingualGlossaryDictionaryEntries glossaryDict)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateMultilingualGlossaryDictionary(
        multilingualGlossaryId(glossary), glossaryDict, null);
  }

  /**
   * Updates a glossary's name with the provided parameter
   *
   * @param glossaryId The specified ID of the glossary whose name will be updated
   * @param name The new name of the glossary
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryName(String glossaryId, String name)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateMultilingualGlossaryName(glossaryId, name, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryName(String, String)} but
   * accepts a {@link MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary to rename.
   * @param name The new name of the glossary.
   * @return {@link MultilingualGlossaryInfo} object with details about the renamed glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryName(
      MultilingualGlossaryInfo glossary, String name)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateMultilingualGlossaryName(multilingualGlossaryId(glossary), name, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryName(String, String)} but
   * accepts options; the extra body parameters of the options are added to the request last.
   *
   * @param glossaryId The specified ID of the glossary to rename.
   * @param name The new name of the glossary.
   * @param options Options for the request, or <code>null</code>; only its extra body parameters
   *     are read.
   * @return {@link MultilingualGlossaryInfo} object with details about the renamed glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryName(
      String glossaryId, String name, @Nullable BaseRequestOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    validateParameter("name", name);
    Map<String, Object> requestData = new LinkedHashMap<>();
    requestData.put("name", name);
    String jsonBody = jsonBodyWithExtraParameters(requestData, options);
    String relativeUrl = multilingualGlossaryPath(glossaryId);
    HttpResponse response =
        httpClientWrapper.sendJsonPatchRequestWithBackoff(relativeUrl, jsonBody);
    checkResponse(response, false);
    return jsonParser.parseMultilingualGlossaryInfo(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryName(String, String,
   * BaseRequestOptions)} but accepts a {@link MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary to rename.
   * @param name The new name of the glossary.
   * @param options Options for the request, or <code>null</code>; only its extra body parameters
   *     are read.
   * @return {@link MultilingualGlossaryInfo} object with details about the renamed glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryName(
      MultilingualGlossaryInfo glossary, String name, @Nullable BaseRequestOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateMultilingualGlossaryName(multilingualGlossaryId(glossary), name, options);
  }

  /**
   * Updates the glossary with the specified ID: renames it and/or merges entries into its
   * dictionaries. Only the fields set in the options are sent; a glossary dictionary that does not
   * exist yet is created.
   *
   * @param glossaryId The specified ID of the glossary to update.
   * @param options The new name and/or the dictionaries to merge, or <code>null</code>.
   * @return {@link MultilingualGlossaryInfo} object with details about the updated glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossary(
      String glossaryId, @Nullable UpdateMultilingualGlossaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    String relativeUrl = multilingualGlossaryPath(glossaryId);
    Map<String, Object> requestData = new LinkedHashMap<>();
    if (options != null && options.getName() != null) {
      requestData.put("name", options.getName());
    }
    if (options != null && options.getDictionaries() != null) {
      List<Map<String, Object>> dictionaries = new ArrayList<>();
      for (MultilingualGlossaryDictionaryEntries dictionary : options.getDictionaries()) {
        Objects.requireNonNull(dictionary, "dictionaries");
        dictionaries.add(
            createGlossaryDictionaryData(
                dictionary.getSourceLanguageCode(),
                dictionary.getTargetLanguageCode(),
                entriesTsv(dictionary.getEntries()),
                "tsv"));
      }
      requestData.put("dictionaries", dictionaries);
    }
    String jsonBody = jsonBodyWithExtraParameters(requestData, options);
    HttpResponse response =
        httpClientWrapper.sendJsonPatchRequestWithBackoff(relativeUrl, jsonBody);
    checkResponse(response, false);
    return jsonParser.parseMultilingualGlossaryInfo(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossary(String,
   * UpdateMultilingualGlossaryOptions)} but accepts a {@link MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary to update.
   * @param options The new name and/or the dictionaries to merge, or <code>null</code>.
   * @return {@link MultilingualGlossaryInfo} object with details about the updated glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossary(
      MultilingualGlossaryInfo glossary, @Nullable UpdateMultilingualGlossaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateMultilingualGlossary(multilingualGlossaryId(glossary), options);
  }

  /**
   * Updates a glossary dictionary correlating to the specified ID with given entries in the {@code
   * csvFile} for the source and target language codes. If a dictionary for the provided language
   * pair already exists, the dictionary entries are merged.
   *
   * @param glossaryId The specified ID of the glossary that contains the dictionary to be
   *     updated/created
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile {@link File} containing CSV content.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossaryId, sourceLanguageCode, targetLanguageCode or csvFile
   *     is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionaryFromCsv(
      String glossaryId, String sourceLanguageCode, String targetLanguageCode, File csvFile)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    return updateMultilingualGlossaryDictionaryFromCsv(
        glossaryId, sourceLanguageCode, targetLanguageCode, csvFile, null);
  }

  /**
   * Updates a glossary dictionary with given entries in the {@code csvFile} for the source and
   * target language codes. If a dictionary for the provided language pair already exists, the
   * dictionary entries are merged.
   *
   * @param glossaryId The specified ID of the glossary that contains the dictionary to be
   *     updated/created
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossaryId, sourceLanguageCode, targetLanguageCode or
   *     csvContent is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionaryFromCsv(
      String glossaryId, String sourceLanguageCode, String targetLanguageCode, String csvContent)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateMultilingualGlossaryDictionaryFromCsv(
        glossaryId, sourceLanguageCode, targetLanguageCode, csvContent, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, File)} but accepts options; the extra body parameters of the options are added
   * to the request last.
   *
   * @param glossaryId The specified ID for the glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile File containing CSV content.
   * @param options Options for the request, or <code>null</code>; only its extra body parameters
   *     are read.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossaryId, sourceLanguageCode, targetLanguageCode or csvFile
   *     is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionaryFromCsv(
      String glossaryId,
      String sourceLanguageCode,
      String targetLanguageCode,
      File csvFile,
      @Nullable BaseRequestOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    Objects.requireNonNull(csvFile, "csvFile");
    try (FileInputStream stream = new FileInputStream(csvFile)) {
      String csvContent = StreamUtil.readStream(stream);
      return updateGlossaryDictionaryInternal(
          glossaryId, sourceLanguageCode, targetLanguageCode, csvContent, "csv", options);
    }
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, File)} but accepts a {@link MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile File containing CSV content.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossary, sourceLanguageCode, targetLanguageCode or csvFile is
   *     <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionaryFromCsv(
      MultilingualGlossaryInfo glossary,
      String sourceLanguageCode,
      String targetLanguageCode,
      File csvFile)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    return updateMultilingualGlossaryDictionaryFromCsv(
        multilingualGlossaryId(glossary), sourceLanguageCode, targetLanguageCode, csvFile, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, File, BaseRequestOptions)} but accepts a {@link MultilingualGlossaryInfo}
   * object.
   *
   * @param glossary The glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile File containing CSV content.
   * @param options Options for the request, or <code>null</code>; only its extra body parameters
   *     are read.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossary, sourceLanguageCode, targetLanguageCode or csvFile is
   *     <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionaryFromCsv(
      MultilingualGlossaryInfo glossary,
      String sourceLanguageCode,
      String targetLanguageCode,
      File csvFile,
      @Nullable BaseRequestOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    return updateMultilingualGlossaryDictionaryFromCsv(
        multilingualGlossaryId(glossary), sourceLanguageCode, targetLanguageCode, csvFile, options);
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, String)} but accepts options; the extra body parameters of the options are
   * added to the request last.
   *
   * @param glossaryId The specified ID for the glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @param options Options for the request, or <code>null</code>; only its extra body parameters
   *     are read.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossaryId, sourceLanguageCode, targetLanguageCode or
   *     csvContent is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionaryFromCsv(
      String glossaryId,
      String sourceLanguageCode,
      String targetLanguageCode,
      String csvContent,
      @Nullable BaseRequestOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateGlossaryDictionaryInternal(
        glossaryId, sourceLanguageCode, targetLanguageCode, csvContent, "csv", options);
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, String)} but accepts a {@link MultilingualGlossaryInfo} object.
   *
   * @param glossary The glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossary, sourceLanguageCode, targetLanguageCode or csvContent
   *     is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionaryFromCsv(
      MultilingualGlossaryInfo glossary,
      String sourceLanguageCode,
      String targetLanguageCode,
      String csvContent)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateMultilingualGlossaryDictionaryFromCsv(
        multilingualGlossaryId(glossary), sourceLanguageCode, targetLanguageCode, csvContent, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateMultilingualGlossaryDictionaryFromCsv(String,
   * String, String, String, BaseRequestOptions)} but accepts a {@link MultilingualGlossaryInfo}
   * object.
   *
   * @param glossary The glossary.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvContent String containing CSV content.
   * @param options Options for the request, or <code>null</code>; only its extra body parameters
   *     are read.
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws NullPointerException If glossary, sourceLanguageCode, targetLanguageCode or csvContent
   *     is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionaryFromCsv(
      MultilingualGlossaryInfo glossary,
      String sourceLanguageCode,
      String targetLanguageCode,
      String csvContent,
      @Nullable BaseRequestOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateMultilingualGlossaryDictionaryFromCsv(
        multilingualGlossaryId(glossary),
        sourceLanguageCode,
        targetLanguageCode,
        csvContent,
        options);
  }

  /**
   * Deletes the glossary with the specified ID.
   *
   * @param glossaryId ID of glossary to delete.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public void deleteMultilingualGlossary(String glossaryId)
      throws DeepLException, InterruptedException {
    String relativeUrl = multilingualGlossaryPath(glossaryId);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    this.checkResponse(response, false);
  }

  /**
   * Deletes the specified glossary.
   *
   * @param glossary {@link MultilingualGlossaryInfo} object corresponding to glossary to delete.
   * @throws IllegalArgumentException If glossary has no ID.
   * @throws NullPointerException If glossary is <code>null</code>.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public void deleteMultilingualGlossary(MultilingualGlossaryInfo glossary)
      throws DeepLException, InterruptedException {
    this.deleteMultilingualGlossary(multilingualGlossaryId(glossary));
  }

  /**
   * Deletes the glossary dictionary with the source and target language codes specified in the
   * glossary with the specified ID.
   *
   * @param glossaryId ID of glossary that contains the glossary dictionary to delete.
   * @param sourceLanguageCode Source language code of the glossary dictionary to be deleted.
   * @param targetLanguageCode Target language code of the glossary dictionary to be deleted.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public void deleteMultilingualGlossaryDictionary(
      String glossaryId, String sourceLanguageCode, String targetLanguageCode)
      throws DeepLException, InterruptedException, IllegalArgumentException {
    String queryString = createLanguageQueryParams(sourceLanguageCode, targetLanguageCode);
    String relativeUrl = multilingualGlossaryPath(glossaryId) + "/dictionaries" + queryString;
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    this.checkResponse(response, false);
  }

  /**
   * Deletes the specified glossary dictionary in the glossary with the specified ID.
   *
   * @param glossaryId ID of glossary that contains the glossary dictionary to delete.
   * @param glossaryDict {@link MultilingualGlossaryDictionaryInfo} object corresponding to glossary
   *     dictionary to delete.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public void deleteMultilingualGlossaryDictionary(
      String glossaryId, MultilingualGlossaryDictionaryInfo glossaryDict)
      throws DeepLException, InterruptedException, IllegalArgumentException {
    Objects.requireNonNull(glossaryDict, "glossaryDict");
    deleteMultilingualGlossaryDictionary(
        glossaryId, glossaryDict.getSourceLanguageCode(), glossaryDict.getTargetLanguageCode());
  }

  /**
   * Deletes the specified glossary dictionary in the glossary in the specified glossary.
   *
   * @param glossary The glossary that contains the glossary dictionary to delete.
   * @param sourceLanguageCode Source language code of the glossary dictionary to be deleted.
   * @param targetLanguageCode Target language code of the glossary dictionary to be deleted.
   * @throws IllegalArgumentException If glossary has no ID.
   * @throws NullPointerException If glossary is <code>null</code>.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public void deleteMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary, String sourceLanguageCode, String targetLanguageCode)
      throws DeepLException, InterruptedException, IllegalArgumentException {
    deleteMultilingualGlossaryDictionary(
        multilingualGlossaryId(glossary), sourceLanguageCode, targetLanguageCode);
  }

  /**
   * Deletes the specified glossary dictionary in the glossary in the specified glossary.
   *
   * @param glossary The glossary that contains the glossary dictionary to delete.
   * @param glossaryDict {@link MultilingualGlossaryDictionaryInfo} object corresponding to glossary
   *     dictionary to delete.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If glossary has no ID.
   * @throws NullPointerException If glossary is <code>null</code>.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public void deleteMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary, MultilingualGlossaryDictionaryInfo glossaryDict)
      throws DeepLException, InterruptedException, IllegalArgumentException {
    Objects.requireNonNull(glossaryDict, "glossaryDict");
    deleteMultilingualGlossaryDictionary(
        multilingualGlossaryId(glossary),
        glossaryDict.getSourceLanguageCode(),
        glossaryDict.getTargetLanguageCode());
  }

  /**
   * Creates a Spoken Terms collection with the given term lists and returns a {@link SpokenTerms}
   * object with details about the new collection.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param name User-defined name of the collection.
   * @param termLists Term lists of the collection, one {@link SpokenTermsEntries} per language.
   * @param options Additional options, or <code>null</code>; see {@link CreateSpokenTermsOptions}.
   * @return {@link SpokenTerms} object with details about the new collection.
   * @throws IllegalArgumentException If a term is invalid, as checked for glossary terms.
   * @throws NullPointerException If name or termLists is <code>null</code> or contains <code>null
   *     </code>, or a term list's terms are or contain <code>null</code>; checked before any
   *     request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public SpokenTerms createSpokenTerms(
      String name, List<SpokenTermsEntries> termLists, @Nullable CreateSpokenTermsOptions options)
      throws DeepLException, InterruptedException {
    Objects.requireNonNull(name, "name");
    Map<String, Object> requestData = new LinkedHashMap<>();
    requestData.put("name", name);
    requestData.put("term_lists", createSpokenTermsListsData(termLists));
    String jsonBody = jsonBodyWithExtraParameters(requestData, options);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestRetryingOnlyUnprocessed("/v3/spoken-terms", jsonBody);
    checkResponse(response, false);
    return jsonParser.parseSpokenTerms(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#createSpokenTerms(String, List,
   * CreateSpokenTermsOptions)} without options.
   *
   * @see DeepLClient#createSpokenTerms(String, List, CreateSpokenTermsOptions)
   */
  public SpokenTerms createSpokenTerms(String name, List<SpokenTermsEntries> termLists)
      throws DeepLException, InterruptedException {
    return createSpokenTerms(name, termLists, null);
  }

  /**
   * Retrieves all Spoken Terms collections in your account. The returned {@link SpokenTerms}
   * objects do not contain the terms; to retrieve them use {@link
   * DeepLClient#getSpokenTermsEntries(String, String)}.
   *
   * @return List of {@link SpokenTerms} objects, one per collection.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<SpokenTerms> listSpokenTerms() throws DeepLException, InterruptedException {
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff("/v3/spoken-terms");
    checkResponse(response, false);
    return jsonParser.parseSpokenTermsList(response.getBody());
  }

  /**
   * Retrieves information about the Spoken Terms collection with the given ID. This does not
   * retrieve the terms; to retrieve them use {@link DeepLClient#getSpokenTermsEntries(String,
   * String)}.
   *
   * @param spokenTermsId ID of the collection to retrieve.
   * @return {@link SpokenTerms} object with details about the collection.
   * @throws NotFoundException If no collection with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public SpokenTerms getSpokenTerms(String spokenTermsId)
      throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(spokenTermsPath(spokenTermsId));
    checkResponse(response, false);
    return jsonParser.parseSpokenTerms(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getSpokenTerms(String)} but accepts a {@link
   * SpokenTerms} object.
   *
   * @see DeepLClient#getSpokenTerms(String)
   */
  public SpokenTerms getSpokenTerms(SpokenTerms spokenTerms)
      throws DeepLException, InterruptedException {
    return getSpokenTerms(spokenTermsId(spokenTerms));
  }

  /**
   * Updates the name and/or term lists of the Spoken Terms collection with the given ID. The
   * entries of each given term list are merged into the existing term list of that language. To
   * replace a term list instead, use {@link DeepLClient#replaceSpokenTermsList(String,
   * SpokenTermsEntries)}.
   *
   * @param spokenTermsId ID of the collection to update.
   * @param options The fields to change, see {@link UpdateSpokenTermsOptions}; only the fields set
   *     are sent. <code>null</code> changes nothing.
   * @return {@link SpokenTerms} object with details about the updated collection.
   * @throws NotFoundException If no collection with the given ID is found.
   * @throws IllegalArgumentException If a term is invalid, as checked for glossary terms.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public SpokenTerms updateSpokenTerms(
      String spokenTermsId, @Nullable UpdateSpokenTermsOptions options)
      throws DeepLException, InterruptedException {
    String relativeUrl = spokenTermsPath(spokenTermsId);
    Map<String, Object> requestData = new LinkedHashMap<>();
    if (options != null && options.getName() != null) {
      requestData.put("name", options.getName());
    }
    if (options != null && options.getTermLists() != null) {
      requestData.put("term_lists", createSpokenTermsListsData(options.getTermLists()));
    }
    String jsonBody = jsonBodyWithExtraParameters(requestData, options);
    HttpResponse response =
        httpClientWrapper.sendJsonPatchRequestWithBackoff(relativeUrl, jsonBody);
    checkResponse(response, false);
    return jsonParser.parseSpokenTerms(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#updateSpokenTerms(String, UpdateSpokenTermsOptions)}
   * but accepts a {@link SpokenTerms} object.
   *
   * @see DeepLClient#updateSpokenTerms(String, UpdateSpokenTermsOptions)
   */
  public SpokenTerms updateSpokenTerms(
      SpokenTerms spokenTerms, @Nullable UpdateSpokenTermsOptions options)
      throws DeepLException, InterruptedException {
    return updateSpokenTerms(spokenTermsId(spokenTerms), options);
  }

  /**
   * Retrieves the terms of the given language in the Spoken Terms collection with the given ID.
   *
   * @param spokenTermsId ID of the collection.
   * @param lang Language code (ISO 639-1) of the term list, for example "en".
   * @return {@link SpokenTermsEntries} object containing the terms.
   * @throws NotFoundException If the collection or its term list for the language is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public SpokenTermsEntries getSpokenTermsEntries(String spokenTermsId, String lang)
      throws DeepLException, InterruptedException {
    String relativeUrl = spokenTermsPath(spokenTermsId) + "/entries" + langQueryString(lang);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseSpokenTermsEntries(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getSpokenTermsEntries(String, String)} but accepts a
   * {@link SpokenTerms} object.
   *
   * @see DeepLClient#getSpokenTermsEntries(String, String)
   */
  public SpokenTermsEntries getSpokenTermsEntries(SpokenTerms spokenTerms, String lang)
      throws DeepLException, InterruptedException {
    return getSpokenTermsEntries(spokenTermsId(spokenTerms), lang);
  }

  /**
   * Replaces the term list of the language of {@code entries} in the Spoken Terms collection with
   * the given ID, or adds it if the collection has no term list for that language.
   *
   * @param spokenTermsId ID of the collection.
   * @param entries Language and terms of the new term list.
   * @param options Additional options, or <code>null</code>; see {@link
   *     ReplaceSpokenTermsListOptions}.
   * @return {@link SpokenTermsList} object with details about the new term list.
   * @throws NotFoundException If no collection with the given ID is found.
   * @throws IllegalArgumentException If a term is invalid, as checked for glossary terms.
   * @throws NullPointerException If spokenTermsId or entries is <code>null</code>, or the terms are
   *     or contain <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public SpokenTermsList replaceSpokenTermsList(
      String spokenTermsId,
      SpokenTermsEntries entries,
      @Nullable ReplaceSpokenTermsListOptions options)
      throws DeepLException, InterruptedException {
    String relativeUrl = spokenTermsPath(spokenTermsId) + "/term-lists";
    String jsonBody = jsonBodyWithExtraParameters(createSpokenTermsListData(entries), options);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff("PUT", relativeUrl, jsonBody);
    checkResponse(response, false);
    return jsonParser.parseSpokenTermsTermList(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#replaceSpokenTermsList(String, SpokenTermsEntries,
   * ReplaceSpokenTermsListOptions)} without options.
   *
   * @see DeepLClient#replaceSpokenTermsList(String, SpokenTermsEntries,
   *     ReplaceSpokenTermsListOptions)
   */
  public SpokenTermsList replaceSpokenTermsList(String spokenTermsId, SpokenTermsEntries entries)
      throws DeepLException, InterruptedException {
    return replaceSpokenTermsList(spokenTermsId, entries, null);
  }

  /**
   * Functions the same as {@link DeepLClient#replaceSpokenTermsList(String, SpokenTermsEntries,
   * ReplaceSpokenTermsListOptions)} but accepts a {@link SpokenTerms} object.
   *
   * @see DeepLClient#replaceSpokenTermsList(String, SpokenTermsEntries,
   *     ReplaceSpokenTermsListOptions)
   */
  public SpokenTermsList replaceSpokenTermsList(
      SpokenTerms spokenTerms,
      SpokenTermsEntries entries,
      @Nullable ReplaceSpokenTermsListOptions options)
      throws DeepLException, InterruptedException {
    return replaceSpokenTermsList(spokenTermsId(spokenTerms), entries, options);
  }

  /**
   * Functions the same as {@link DeepLClient#replaceSpokenTermsList(String, SpokenTermsEntries)}
   * but accepts a {@link SpokenTerms} object.
   *
   * @see DeepLClient#replaceSpokenTermsList(String, SpokenTermsEntries)
   */
  public SpokenTermsList replaceSpokenTermsList(SpokenTerms spokenTerms, SpokenTermsEntries entries)
      throws DeepLException, InterruptedException {
    return replaceSpokenTermsList(spokenTermsId(spokenTerms), entries, null);
  }

  /**
   * Deletes the Spoken Terms collection with the given ID.
   *
   * @param spokenTermsId ID of the collection to delete.
   * @throws NotFoundException If no collection with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteSpokenTerms(String spokenTermsId) throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendDeleteRequestWithBackoff(spokenTermsPath(spokenTermsId));
    checkResponse(response, false);
  }

  /**
   * Functions the same as {@link DeepLClient#deleteSpokenTerms(String)} but accepts a {@link
   * SpokenTerms} object.
   *
   * @see DeepLClient#deleteSpokenTerms(String)
   */
  public void deleteSpokenTerms(SpokenTerms spokenTerms)
      throws DeepLException, InterruptedException {
    deleteSpokenTerms(spokenTermsId(spokenTerms));
  }

  /**
   * Deletes the term list of the given language from the Spoken Terms collection with the given ID.
   *
   * @param spokenTermsId ID of the collection.
   * @param lang Language code (ISO 639-1) of the term list to delete, for example "en".
   * @throws NotFoundException If the collection or its term list for the language is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteSpokenTermsList(String spokenTermsId, String lang)
      throws DeepLException, InterruptedException {
    String relativeUrl = spokenTermsPath(spokenTermsId) + "/term-lists" + langQueryString(lang);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
  }

  /**
   * Functions the same as {@link DeepLClient#deleteSpokenTermsList(String, String)} but accepts a
   * {@link SpokenTerms} object.
   *
   * @see DeepLClient#deleteSpokenTermsList(String, String)
   */
  public void deleteSpokenTermsList(SpokenTerms spokenTerms, String lang)
      throws DeepLException, InterruptedException {
    deleteSpokenTermsList(spokenTermsId(spokenTerms), lang);
  }

  /**
   * Retrieves the list of all available style rules and returns a list of {@link StyleRuleInfo}
   * objects corresponding to all of your stored style rules.
   *
   * @param page Optional page number for pagination, 0-indexed.
   * @param pageSize Optional number of items per page.
   * @param detailed Optional flag indicating whether to include detailed configuration rules
   *     including the configuredRules and customInstructions properties.
   * @return List of {@link StyleRuleInfo} objects for all available style rules.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public List<StyleRuleInfo> getAllStyleRules(
      @Nullable Integer page, @Nullable Integer pageSize, @Nullable Boolean detailed)
      throws DeepLException, InterruptedException {
    ArrayList<KeyValuePair<String, String>> queryParams = new ArrayList<>();
    if (page != null) {
      queryParams.add(new KeyValuePair<>("page", page.toString()));
    }
    if (pageSize != null) {
      queryParams.add(new KeyValuePair<>("page_size", pageSize.toString()));
    }
    if (detailed != null) {
      queryParams.add(new KeyValuePair<>("detailed", detailed.toString().toLowerCase()));
    }

    String relativeUrl = "/v3/style_rules" + ApiClientHelper.createQueryString(queryParams);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseStyleRuleInfoList(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getAllStyleRules(Integer, Integer, Boolean)} but with
   * default parameters (all null).
   *
   * @see DeepLClient#getAllStyleRules(Integer, Integer, Boolean)
   */
  public List<StyleRuleInfo> getAllStyleRules() throws DeepLException, InterruptedException {
    return getAllStyleRules(null, null, null);
  }

  /**
   * Retrieves one page of the style profiles in your account.
   *
   * @param page Page number to retrieve (starting from 0), or <code>null</code> for the API default
   *     (0).
   * @param pageSize Number of items per page, or <code>null</code> for the API default (10). The
   *     API reduces values above 100 to 100.
   * @return List of {@link StyleProfile} objects on the requested page.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<StyleProfile> listStyleProfiles(@Nullable Integer page, @Nullable Integer pageSize)
      throws DeepLException, InterruptedException {
    ArrayList<KeyValuePair<String, String>> queryParams = new ArrayList<>();
    if (page != null) {
      queryParams.add(new KeyValuePair<>("page", page.toString()));
    }
    if (pageSize != null) {
      queryParams.add(new KeyValuePair<>("page_size", pageSize.toString()));
    }
    String relativeUrl = "/v3/style_profiles" + ApiClientHelper.createQueryString(queryParams);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseStyleProfileList(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#listStyleProfiles(Integer, Integer)} but with default
   * parameters (all null).
   *
   * @see DeepLClient#listStyleProfiles(Integer, Integer)
   */
  public List<StyleProfile> listStyleProfiles() throws DeepLException, InterruptedException {
    return listStyleProfiles(null, null);
  }

  /**
   * Creates a style profile and returns a {@link StyleProfile} object with details about it. If any
   * of the given component IDs cannot be used, the request fails and no style profile is created.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param name Name of the style profile.
   * @param options Optional description, industry and linked components, or <code>null</code>.
   *     Fields left <code>null</code> are not sent.
   * @return {@link StyleProfile} object with details about the new style profile.
   * @throws NullPointerException If name is <code>null</code>; checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleProfile createStyleProfile(String name, @Nullable CreateStyleProfileOptions options)
      throws DeepLException, InterruptedException {
    Objects.requireNonNull(name, "name");

    HttpResponse response =
        httpClientWrapper.sendJsonRequestRetryingOnlyUnprocessed(
            "/v3/style_profiles", createStyleProfileBody(name, options));
    checkResponse(response, false);
    return jsonParser.parseStyleProfile(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#createStyleProfile(String, CreateStyleProfileOptions)}
   * but without options.
   *
   * @see DeepLClient#createStyleProfile(String, CreateStyleProfileOptions)
   */
  public StyleProfile createStyleProfile(String name) throws DeepLException, InterruptedException {
    return createStyleProfile(name, null);
  }

  /**
   * Retrieves the style profile with the given ID.
   *
   * @param styleProfileId ID of the style profile to retrieve.
   * @return {@link StyleProfile} object with details about the style profile.
   * @throws IllegalArgumentException If the ID is empty.
   * @throws NullPointerException If the ID is <code>null</code>.
   * @throws NotFoundException If no style profile with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleProfile getStyleProfile(String styleProfileId)
      throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(styleProfilePath(styleProfileId));
    checkResponse(response, false);
    return jsonParser.parseStyleProfile(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getStyleProfile(String)} but accepts a {@link
   * StyleProfile} object.
   *
   * @see DeepLClient#getStyleProfile(String)
   */
  public StyleProfile getStyleProfile(StyleProfile styleProfile)
      throws DeepLException, InterruptedException {
    return getStyleProfile(styleProfileId(styleProfile));
  }

  /**
   * Updates the style profile with the given ID and returns the updated {@link StyleProfile}.
   * Fields left <code>null</code> are not sent and stay unchanged. An empty string clears the
   * description or industry; a component list that is sent replaces the linked list in full, and an
   * empty list unlinks all components of that kind. With nothing set, nothing changes.
   *
   * @param styleProfileId ID of the style profile to update.
   * @param options Fields to change, including the new name, or <code>null</code>.
   * @return {@link StyleProfile} object with details about the updated style profile.
   * @throws IllegalArgumentException If the ID is empty.
   * @throws NullPointerException If the ID is <code>null</code>.
   * @throws NotFoundException If no style profile with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleProfile updateStyleProfile(
      String styleProfileId, @Nullable UpdateStyleProfileOptions options)
      throws DeepLException, InterruptedException {
    String relativeUrl = styleProfilePath(styleProfileId);
    HttpResponse response =
        httpClientWrapper.sendJsonPatchRequestWithBackoff(
            relativeUrl,
            createStyleProfileBody(options != null ? options.getName() : null, options));
    checkResponse(response, false);
    return jsonParser.parseStyleProfile(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleProfile(String, UpdateStyleProfileOptions)}
   * but accepts a {@link StyleProfile} object.
   *
   * @see DeepLClient#updateStyleProfile(String, UpdateStyleProfileOptions)
   */
  public StyleProfile updateStyleProfile(
      StyleProfile styleProfile, @Nullable UpdateStyleProfileOptions options)
      throws DeepLException, InterruptedException {
    return updateStyleProfile(styleProfileId(styleProfile), options);
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleProfile(String, UpdateStyleProfileOptions)}
   * but without options; sends an empty update.
   *
   * @see DeepLClient#updateStyleProfile(String, UpdateStyleProfileOptions)
   */
  public StyleProfile updateStyleProfile(String styleProfileId)
      throws DeepLException, InterruptedException {
    return updateStyleProfile(styleProfileId, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleProfile(StyleProfile,
   * UpdateStyleProfileOptions)} but without options; sends an empty update.
   *
   * @see DeepLClient#updateStyleProfile(String, UpdateStyleProfileOptions)
   */
  public StyleProfile updateStyleProfile(StyleProfile styleProfile)
      throws DeepLException, InterruptedException {
    return updateStyleProfile(styleProfile, null);
  }

  /**
   * Deletes the style profile with the given ID.
   *
   * @param styleProfileId ID of the style profile to delete.
   * @throws IllegalArgumentException If the ID is empty.
   * @throws NullPointerException If the ID is <code>null</code>.
   * @throws NotFoundException If no style profile with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteStyleProfile(String styleProfileId)
      throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendDeleteRequestWithBackoff(styleProfilePath(styleProfileId));
    checkResponse(response, false);
  }

  /**
   * Functions the same as {@link DeepLClient#deleteStyleProfile(String)} but accepts a {@link
   * StyleProfile} object.
   *
   * @see DeepLClient#deleteStyleProfile(String)
   */
  public void deleteStyleProfile(StyleProfile styleProfile)
      throws DeepLException, InterruptedException {
    deleteStyleProfile(styleProfileId(styleProfile));
  }

  /**
   * Retrieves the languages supported by the given DeepL API resource, along with the features each
   * language supports for that resource.
   *
   * @param resource Resource to retrieve languages for, for example "translate_text" or "glossary".
   *     See {@link LanguageResourceName} for the known values.
   * @param options Options influencing the request, or <code>null</code> to return only stable
   *     languages and features.
   * @return List of {@link LanguageSupport} objects with per-feature support information.
   * @throws IllegalArgumentException If resource is empty.
   * @throws NullPointerException If resource is <code>null</code>.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<LanguageSupport> getLanguagesForResource(
      String resource, @Nullable GetLanguagesForResourceOptions options)
      throws DeepLException, InterruptedException {
    validateParameter("resource", resource);
    ArrayList<KeyValuePair<String, String>> queryParams = new ArrayList<>();
    queryParams.add(new KeyValuePair<>("resource", resource));
    if (options != null && options.getInclude() != null) {
      for (String value : options.getInclude()) {
        queryParams.add(new KeyValuePair<>("include", value));
      }
    }
    String relativeUrl = "/v3/languages" + ApiClientHelper.createQueryString(queryParams);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseLanguageSupportList(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getLanguagesForResource(String,
   * GetLanguagesForResourceOptions)} but returns only stable languages and features.
   *
   * @see DeepLClient#getLanguagesForResource(String, GetLanguagesForResourceOptions)
   */
  public List<LanguageSupport> getLanguagesForResource(String resource)
      throws DeepLException, InterruptedException {
    return getLanguagesForResource(resource, null);
  }

  /**
   * Functions the same as {@link DeepLClient#getLanguagesForResource(String,
   * GetLanguagesForResourceOptions)} but accepts a {@link LanguageResourceName}.
   *
   * @see DeepLClient#getLanguagesForResource(String, GetLanguagesForResourceOptions)
   */
  public List<LanguageSupport> getLanguagesForResource(
      LanguageResourceName resource, @Nullable GetLanguagesForResourceOptions options)
      throws DeepLException, InterruptedException {
    return getLanguagesForResource(
        Objects.requireNonNull(resource, "resource").getValue(), options);
  }

  /**
   * Functions the same as {@link DeepLClient#getLanguagesForResource(String,
   * GetLanguagesForResourceOptions)} but accepts a {@link LanguageResourceName} and returns only
   * stable languages and features.
   *
   * @see DeepLClient#getLanguagesForResource(String, GetLanguagesForResourceOptions)
   */
  public List<LanguageSupport> getLanguagesForResource(LanguageResourceName resource)
      throws DeepLException, InterruptedException {
    return getLanguagesForResource(Objects.requireNonNull(resource, "resource").getValue(), null);
  }

  /**
   * Retrieves the resources supported by the DeepL API, along with their features and whether each
   * feature requires support from the source and/or target language.
   *
   * @return List of {@link LanguageResource} objects, one per supported resource.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<LanguageResource> getLanguageResources() throws DeepLException, InterruptedException {
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff("/v3/languages/resources");
    checkResponse(response, false);
    return jsonParser.parseLanguageResourceList(response.getBody());
  }

  /**
   * Retrieves a list of translation memories available for the account associated with the DeepL
   * API auth key. The maximum number of translation memories returned is controlled by pageSize
   * (max 25).
   *
   * @param page Page number to retrieve (starting from 0), or <code>null</code>.
   * @param pageSize Number of items per page, or <code>null</code>.
   * @return List of {@link TranslationMemoryInfo} objects.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<TranslationMemoryInfo> listTranslationMemories(
      @Nullable Integer page, @Nullable Integer pageSize)
      throws DeepLException, InterruptedException {
    ArrayList<KeyValuePair<String, String>> queryParams = new ArrayList<>();
    if (page != null) {
      queryParams.add(new KeyValuePair<>("page", page.toString()));
    }
    if (pageSize != null) {
      queryParams.add(new KeyValuePair<>("page_size", pageSize.toString()));
    }

    String relativeUrl =
        "/v3/translation_memories" + ApiClientHelper.createQueryString(queryParams);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseTranslationMemoryInfoList(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#listTranslationMemories(Integer, Integer)} but with
   * default parameters (all null).
   *
   * @see DeepLClient#listTranslationMemories(Integer, Integer)
   */
  public List<TranslationMemoryInfo> listTranslationMemories()
      throws DeepLException, InterruptedException {
    return listTranslationMemories(null, null);
  }

  /**
   * Retrieves information about the translation memory with the specified ID and returns a {@link
   * TranslationMemoryInfo} object containing details.
   *
   * @param translationMemoryId ID of the translation memory to retrieve.
   * @return {@link TranslationMemoryInfo} object with details about the translation memory.
   * @throws NotFoundException If no translation memory with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public TranslationMemoryInfo getTranslationMemory(String translationMemoryId)
      throws DeepLException, InterruptedException, NotFoundException {
    validateParameter("translationMemoryId", translationMemoryId);
    String relativeUrl = translationMemoryPath(translationMemoryId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseTranslationMemoryInfo(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getTranslationMemory(String)} but accepts a {@link
   * TranslationMemoryInfo} object.
   *
   * @see DeepLClient#getTranslationMemory(String)
   */
  public TranslationMemoryInfo getTranslationMemory(TranslationMemoryInfo translationMemory)
      throws DeepLException, InterruptedException, NotFoundException {
    return getTranslationMemory(translationMemoryId(translationMemory));
  }

  /**
   * Retrieves one page of the segments of the translation memory with the specified ID.
   *
   * <p>Pagination is cursor-based: omit the page cursor on the first call, then pass the previous
   * response's {@link TranslationMemorySegments#getNextPageCursor()} to fetch the next page. An
   * absent next page cursor means the last page has been returned.
   *
   * @param translationMemoryId ID of the translation memory to retrieve the segments of.
   * @param options Options influencing the returned page, or <code>null</code>.
   * @return {@link TranslationMemorySegments} object containing the requested page.
   * @throws NotFoundException If no translation memory with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public TranslationMemorySegments listTranslationMemorySegments(
      String translationMemoryId, @Nullable TranslationMemorySegmentsOptions options)
      throws DeepLException, InterruptedException, NotFoundException {
    validateParameter("translationMemoryId", translationMemoryId);
    ArrayList<KeyValuePair<String, String>> queryParams = new ArrayList<>();
    if (options != null) {
      if (options.getPageSize() != null) {
        queryParams.add(new KeyValuePair<>("page_size", options.getPageSize().toString()));
      }
      if (options.getPageCursor() != null) {
        queryParams.add(new KeyValuePair<>("page_cursor", options.getPageCursor()));
      }
      if (options.getFilterText() != null) {
        queryParams.add(new KeyValuePair<>("filter_text", options.getFilterText()));
      }
      if (options.getFilterCaseSensitive() != null) {
        queryParams.add(
            new KeyValuePair<>(
                "filter_case_sensitive", options.getFilterCaseSensitive().toString()));
      }
    }

    String relativeUrl =
        translationMemoryPath(translationMemoryId)
            + "/segments"
            + ApiClientHelper.createQueryString(queryParams);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseTranslationMemorySegments(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#listTranslationMemorySegments(String,
   * TranslationMemorySegmentsOptions)} but with default options.
   *
   * @see DeepLClient#listTranslationMemorySegments(String, TranslationMemorySegmentsOptions)
   */
  public TranslationMemorySegments listTranslationMemorySegments(String translationMemoryId)
      throws DeepLException, InterruptedException, NotFoundException {
    return listTranslationMemorySegments(translationMemoryId, null);
  }

  /**
   * Functions the same as {@link DeepLClient#listTranslationMemorySegments(String,
   * TranslationMemorySegmentsOptions)} but accepts a {@link TranslationMemoryInfo} object.
   *
   * @see DeepLClient#listTranslationMemorySegments(String, TranslationMemorySegmentsOptions)
   */
  public TranslationMemorySegments listTranslationMemorySegments(
      TranslationMemoryInfo translationMemory, @Nullable TranslationMemorySegmentsOptions options)
      throws DeepLException, InterruptedException, NotFoundException {
    return listTranslationMemorySegments(translationMemoryId(translationMemory), options);
  }

  /**
   * Functions the same as {@link DeepLClient#listTranslationMemorySegments(String,
   * TranslationMemorySegmentsOptions)} but accepts a {@link TranslationMemoryInfo} object and uses
   * default options.
   *
   * @see DeepLClient#listTranslationMemorySegments(String, TranslationMemorySegmentsOptions)
   */
  public TranslationMemorySegments listTranslationMemorySegments(
      TranslationMemoryInfo translationMemory)
      throws DeepLException, InterruptedException, NotFoundException {
    return listTranslationMemorySegments(translationMemoryId(translationMemory), null);
  }

  /**
   * Deletes the translation memory with the specified ID.
   *
   * @param translationMemoryId ID of the translation memory to delete.
   * @throws NotFoundException If no translation memory with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteTranslationMemory(String translationMemoryId)
      throws DeepLException, InterruptedException, NotFoundException {
    validateParameter("translationMemoryId", translationMemoryId);
    String relativeUrl = translationMemoryPath(translationMemoryId);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
  }

  /**
   * Functions the same as {@link DeepLClient#deleteTranslationMemory(String)} but accepts a {@link
   * TranslationMemoryInfo} object.
   *
   * @see DeepLClient#deleteTranslationMemory(String)
   */
  public void deleteTranslationMemory(TranslationMemoryInfo translationMemory)
      throws DeepLException, InterruptedException, NotFoundException {
    deleteTranslationMemory(translationMemoryId(translationMemory));
  }

  /**
   * Creates an import job for a new translation memory.
   *
   * <p>The job only declares the file; upload the TMX file itself to the returned upload URL with
   * {@link DeepLClient#uploadTranslationMemoryFile(TranslationMemoryImport, byte[])}, then poll
   * {@link DeepLClient#getTranslationMemoryJob(String)} for the outcome. Use {@link
   * DeepLClient#importTranslationMemoryFromFilepath(File, ImportTranslationMemoryOptions)} to do
   * all three steps at once.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param fileName Name of the TMX file to import, for example "legal.tmx".
   * @param contentLength Size of the TMX file in bytes.
   * @param options Options for the import job (content type, display name, extra body parameters),
   *     or <code>null</code>.
   * @return {@link TranslationMemoryImport} object with the job ID and upload URL.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public TranslationMemoryImport createTranslationMemoryImport(
      String fileName, long contentLength, @Nullable CreateTranslationMemoryImportOptions options)
      throws DeepLException, InterruptedException {
    validateParameter("fileName", fileName);
    if (contentLength <= 0) {
      throw new IllegalArgumentException("contentLength must be greater than 0");
    }

    Map<String, Object> sourceFile = new LinkedHashMap<>();
    sourceFile.put("file_name", fileName);
    sourceFile.put("content_length", contentLength);
    Map<String, Object> requestData = new LinkedHashMap<>();
    requestData.put("source_file", sourceFile);
    if (options != null) {
      putIfNotNull(sourceFile, "content_type", options.getContentType());
      if (options.getDisplayName() != null) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("display_name", options.getDisplayName());
        requestData.put("parameters", parameters);
      }
    }

    String jsonBody = jsonBodyWithExtraParameters(requestData, options);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestRetryingOnlyUnprocessed(
            "/v3/translation_memories/import", jsonBody);
    checkResponse(response, false);
    return jsonParser.parseTranslationMemoryImport(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#createTranslationMemoryImport(String, long,
   * CreateTranslationMemoryImportOptions)} but with default options.
   *
   * @see DeepLClient#createTranslationMemoryImport(String, long,
   *     CreateTranslationMemoryImportOptions)
   */
  public TranslationMemoryImport createTranslationMemoryImport(String fileName, long contentLength)
      throws DeepLException, InterruptedException {
    return createTranslationMemoryImport(
        fileName, contentLength, (CreateTranslationMemoryImportOptions) null);
  }

  /**
   * Functions the same as {@link DeepLClient#createTranslationMemoryImport(String, long,
   * CreateTranslationMemoryImportOptions)} but takes the content type and display name as
   * parameters.
   *
   * @param fileName Name of the TMX file to import, for example "legal.tmx".
   * @param contentLength Size of the TMX file in bytes.
   * @param contentType Optional MIME type of the file, defaults to "application/xml".
   * @param displayName Optional name for the resulting translation memory, defaults to the file
   *     name.
   * @see DeepLClient#createTranslationMemoryImport(String, long,
   *     CreateTranslationMemoryImportOptions)
   */
  public TranslationMemoryImport createTranslationMemoryImport(
      String fileName,
      long contentLength,
      @Nullable String contentType,
      @Nullable String displayName)
      throws DeepLException, InterruptedException {
    return createTranslationMemoryImport(
        fileName,
        contentLength,
        new CreateTranslationMemoryImportOptions()
            .setContentType(contentType)
            .setDisplayName(displayName));
  }

  /**
   * Uploads a TMX file to the upload URL of an import job, which starts processing.
   *
   * <p>The upload URL is a pre-signed storage URL outside of the DeepL API, so the authentication
   * key is not sent with this request.
   *
   * @param uploadUrl Upload URL returned by {@link
   *     DeepLClient#createTranslationMemoryImport(String, long,
   *     CreateTranslationMemoryImportOptions)}.
   * @param fileContent Content of the TMX file to upload.
   * @param contentType MIME type of the file, which must match the content type declared when the
   *     import job was created. Optional, defaults to "application/xml" when <code>null</code>.
   * @throws NullPointerException If uploadUrl or fileContent is <code>null</code>; checked before
   *     any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while uploading the file.
   */
  public void uploadTranslationMemoryFile(
      String uploadUrl, byte[] fileContent, @Nullable String contentType)
      throws DeepLException, InterruptedException {
    validateParameter("uploadUrl", uploadUrl);
    if (contentType == null) {
      contentType = TRANSLATION_MEMORY_FILE_CONTENT_TYPE;
    }
    validateParameter("contentType", contentType);
    Objects.requireNonNull(fileContent, "fileContent");
    HttpResponse response =
        httpClientWrapper.sendAssetPutRequestWithBackoff(uploadUrl, fileContent, contentType);
    if (response.getCode() < 200 || response.getCode() >= 300) {
      throw new DeepLException(
              String.format(
                  "Error uploading translation memory file, HTTP status: %d", response.getCode()))
          .setHttpStatusCode(response.getCode());
    }
  }

  /**
   * Functions the same as {@link DeepLClient#uploadTranslationMemoryFile(String, byte[], String)}
   * but uses the default content type "application/xml".
   *
   * @see DeepLClient#uploadTranslationMemoryFile(String, byte[], String)
   */
  public void uploadTranslationMemoryFile(String uploadUrl, byte[] fileContent)
      throws DeepLException, InterruptedException {
    uploadTranslationMemoryFile(uploadUrl, fileContent, TRANSLATION_MEMORY_FILE_CONTENT_TYPE);
  }

  /**
   * Functions the same as {@link DeepLClient#uploadTranslationMemoryFile(String, byte[], String)}
   * but accepts the {@link TranslationMemoryImport} object holding the upload URL.
   *
   * @see DeepLClient#uploadTranslationMemoryFile(String, byte[], String)
   */
  public void uploadTranslationMemoryFile(
      TranslationMemoryImport translationMemoryImport,
      byte[] fileContent,
      @Nullable String contentType)
      throws DeepLException, InterruptedException {
    Objects.requireNonNull(translationMemoryImport, "translationMemoryImport");

    uploadTranslationMemoryFile(translationMemoryImport.getUploadUrl(), fileContent, contentType);
  }

  /**
   * Functions the same as {@link DeepLClient#uploadTranslationMemoryFile(TranslationMemoryImport,
   * byte[], String)} but uses the default content type "application/xml".
   *
   * @see DeepLClient#uploadTranslationMemoryFile(TranslationMemoryImport, byte[], String)
   */
  public void uploadTranslationMemoryFile(
      TranslationMemoryImport translationMemoryImport, byte[] fileContent)
      throws DeepLException, InterruptedException {
    uploadTranslationMemoryFile(
        translationMemoryImport, fileContent, TRANSLATION_MEMORY_FILE_CONTENT_TYPE);
  }

  /**
   * Creates an export job for the translation memory with the specified ID.
   *
   * <p>Poll {@link DeepLClient#getTranslationMemoryJob(String)} for the download URL of the
   * exported TMX file. Use {@link DeepLClient#exportTranslationMemoryToFilepath(String, File)} to
   * do both steps and write the file at once.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param translationMemoryId ID of the translation memory to export.
   * @return {@link TranslationMemoryExport} object with the job ID, and whether the API reused a
   *     previously completed export.
   * @throws NotFoundException If no translation memory with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public TranslationMemoryExport createTranslationMemoryExport(String translationMemoryId)
      throws DeepLException, InterruptedException, NotFoundException {
    validateParameter("translationMemoryId", translationMemoryId);
    String relativeUrl = translationMemoryPath(translationMemoryId) + "/export";
    HttpResponse response = httpClientWrapper.sendRequestRetryingOnlyUnprocessed(relativeUrl);
    checkResponse(response, false);
    // 200 means the API reused a previously completed export, 202 that it started a new one.
    return jsonParser.parseTranslationMemoryExport(response.getBody(), response.getCode() == 200);
  }

  /**
   * Functions the same as {@link DeepLClient#createTranslationMemoryExport(String)} but accepts a
   * {@link TranslationMemoryInfo} object.
   *
   * @see DeepLClient#createTranslationMemoryExport(String)
   */
  public TranslationMemoryExport createTranslationMemoryExport(
      TranslationMemoryInfo translationMemory)
      throws DeepLException, InterruptedException, NotFoundException {
    return createTranslationMemoryExport(translationMemoryId(translationMemory));
  }

  /**
   * Retrieves the status of the translation memory import or export job with the specified ID.
   *
   * @param jobId ID of the job to query.
   * @return {@link TranslationMemoryJob} object with the current status of the job.
   * @throws NotFoundException If no job with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public TranslationMemoryJob getTranslationMemoryJob(String jobId)
      throws DeepLException, InterruptedException, NotFoundException {
    validateParameter("jobId", jobId);
    String relativeUrl = translationMemoryJobPath(jobId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false);
    return jsonParser.parseTranslationMemoryJob(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getTranslationMemoryJob(String)} but accepts a {@link
   * TranslationMemoryJob} object.
   *
   * @see DeepLClient#getTranslationMemoryJob(String)
   */
  public TranslationMemoryJob getTranslationMemoryJob(TranslationMemoryJob job)
      throws DeepLException, InterruptedException, NotFoundException {
    return getTranslationMemoryJob(translationMemoryJobId(job));
  }

  /**
   * Polls the translation memory job with the specified ID until it completes, sleeping between
   * requests, and returns the completed job. A status not known to this version of the library
   * counts as in progress, so waiting continues.
   *
   * <p>Note that an import job keeps reporting {@link
   * TranslationMemoryJobResult.Status#AwaitingInput} for a while after its file has been uploaded,
   * because the API detects the upload asynchronously. That status is therefore polled through like
   * any other non-terminal one. A job whose file is never uploaded does not finish on its own, so
   * use {@link DeepLClient#waitUntilTranslationMemoryJobDone(String, Duration)} to pass a timeout
   * when that is a possibility.
   *
   * @param jobId ID of the job to wait for.
   * @return {@link TranslationMemoryJob} object with the status of the finished job.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the job failed or expired, or any error occurs while communicating
   *     with the DeepL API.
   */
  public TranslationMemoryJob waitUntilTranslationMemoryJobDone(String jobId)
      throws DeepLException, InterruptedException {
    return waitUntilTranslationMemoryJobDone(jobId, null);
  }

  /**
   * Functions the same as {@link DeepLClient#waitUntilTranslationMemoryJobDone(String)} but gives
   * up after the specified timeout.
   *
   * @param jobId ID of the job to wait for.
   * @param timeout Maximum time to wait before throwing, or <code>null</code> to wait indefinitely.
   *     The job status is polled every 5 seconds; the last wait is cut short at the timeout.
   * @return {@link TranslationMemoryJob} object with the status of the finished job.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the timeout is exceeded, the job failed or expired, or any error
   *     occurs while communicating with the DeepL API.
   * @see DeepLClient#waitUntilTranslationMemoryJobDone(String)
   */
  public TranslationMemoryJob waitUntilTranslationMemoryJobDone(
      String jobId, @Nullable Duration timeout) throws DeepLException, InterruptedException {
    Long timeoutNanos = null;
    if (timeout != null) {
      try {
        timeoutNanos = timeout.toNanos();
      } catch (ArithmeticException exception) {
        // Longer than about 292 years: treated as no limit.
      }
    }
    long deadline = timeoutNanos != null ? System.nanoTime() + timeoutNanos : 0;
    TranslationMemoryJob job = recordFetchedJob(getTranslationMemoryJob(jobId));
    while (!job.done() && job.ok()) {
      // The API always returns exactly one result; an empty list would never reach a terminal
      // status and the no-timeout overload would poll forever.
      if (job.getResults().isEmpty()) {
        throw new DeepLException("Translation memory job " + jobId + " returned no results");
      }
      long sleepMillis = TRANSLATION_MEMORY_JOB_POLL_INTERVAL_MILLIS;
      if (timeoutNanos != null) {
        long remainingMillis = (deadline - System.nanoTime()) / 1_000_000;
        if (remainingMillis <= 0) {
          throw new DeepLException(
              String.format(
                  "Manual timeout of %ds exceeded for translation memory job",
                  timeout.getSeconds()));
        }
        sleepMillis = Math.min(sleepMillis, remainingMillis);
      }
      Thread.sleep(sleepMillis);
      job = recordFetchedJob(getTranslationMemoryJob(jobId));
    }
    if (!job.ok()) {
      TranslationMemoryJobResult result = job.getResult();
      String message =
          (result != null && result.getErrorMessage() != null)
              ? result.getErrorMessage()
              : "Unknown error";
      throw new DeepLException(message);
    }
    return job;
  }

  /**
   * Functions the same as {@link DeepLClient#waitUntilTranslationMemoryJobDone(String)} but accepts
   * a {@link TranslationMemoryJob} object.
   *
   * @see DeepLClient#waitUntilTranslationMemoryJobDone(String)
   */
  public TranslationMemoryJob waitUntilTranslationMemoryJobDone(TranslationMemoryJob job)
      throws DeepLException, InterruptedException {
    return waitUntilTranslationMemoryJobDone(translationMemoryJobId(job), null);
  }

  /**
   * Functions the same as {@link DeepLClient#waitUntilTranslationMemoryJobDone(String, Duration)}
   * but accepts a {@link TranslationMemoryJob} object.
   *
   * @see DeepLClient#waitUntilTranslationMemoryJobDone(String, Duration)
   */
  public TranslationMemoryJob waitUntilTranslationMemoryJobDone(
      TranslationMemoryJob job, @Nullable Duration timeout)
      throws DeepLException, InterruptedException {
    return waitUntilTranslationMemoryJobDone(translationMemoryJobId(job), timeout);
  }

  /**
   * Downloads the TMX file of a completed export job to the specified output file.
   *
   * <p>The download URL is a pre-signed storage URL outside of the DeepL API, so the authentication
   * key is not sent with this request.
   *
   * @param job Completed export job carrying the download URL.
   * @param outputFile File to download the exported translation memory to.
   * @throws NullPointerException If job or outputFile is <code>null</code>; checked before any
   *     request.
   * @throws IOException If the output path is occupied.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the job has no download URL, or any error occurs while downloading.
   */
  public void downloadTranslationMemoryExport(TranslationMemoryJob job, File outputFile)
      throws DeepLException, IOException, InterruptedException {
    Objects.requireNonNull(job, "job");
    Objects.requireNonNull(outputFile, "outputFile");

    // Checked before the try so the cleanup below only ever deletes a file this call created:
    // otherwise the guard protecting an existing file would be what destroys it.
    if (outputFile.exists()) {
      throw new IOException("File already exists at output path");
    }
    try {
      try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
        downloadTranslationMemoryExport(job, outputStream);
      }
    } catch (Exception exception) {
      outputFile.delete();
      throw exception;
    }
  }

  /**
   * Downloads the TMX file of a completed export job to the specified output stream. The output
   * stream is not closed.
   *
   * @param job Completed export job carrying the download URL.
   * @param outputStream Stream to download the exported translation memory to.
   * @throws NullPointerException If job or outputStream is <code>null</code>; checked before any
   *     request.
   * @throws IOException If an I/O error occurs while writing to the output stream.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the job has no download URL, or any error occurs while downloading.
   * @see DeepLClient#downloadTranslationMemoryExport(TranslationMemoryJob, File)
   */
  public void downloadTranslationMemoryExport(TranslationMemoryJob job, OutputStream outputStream)
      throws DeepLException, IOException, InterruptedException {
    Objects.requireNonNull(job, "job");
    Objects.requireNonNull(outputStream, "outputStream");

    TranslationMemoryJobResult result = job.getResult();
    String downloadUrl = (result == null) ? null : result.getDownloadUrl();
    if (downloadUrl == null || downloadUrl.isEmpty()) {
      throw new DeepLException(
          "Translation memory export job has no download URL, it may not have completed yet");
    }

    httpClientWrapper.downloadAssetWithBackoff(
        downloadUrl,
        response -> {
          if (response.getCode() < 200 || response.getCode() >= 300) {
            throw new DeepLException(
                    String.format(
                        "Error downloading translation memory export, HTTP status: %d",
                        response.getCode()))
                .setHttpStatusCode(response.getCode());
          }
        },
        outputStream);
  }

  /**
   * Imports a TMX file as a new translation memory: creates the import job, uploads the file, and
   * waits for processing to finish.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param inputFile TMX file to import.
   * @param displayName Optional name for the resulting translation memory, defaults to the file
   *     name.
   * @return {@link TranslationMemoryJob} object for the completed import; its result carries the ID
   *     of the new translation memory.
   * @throws IllegalArgumentException If the input file does not exist.
   * @throws NullPointerException If inputFile is <code>null</code>; checked before any request, not
   *     wrapped.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws TranslationMemoryJobException If the input file cannot be read, the import fails, or
   *     any error occurs while communicating with the DeepL API.
   */
  public TranslationMemoryJob importTranslationMemoryFromFilepath(
      File inputFile, @Nullable String displayName)
      throws TranslationMemoryJobException, InterruptedException {
    return importTranslationMemoryFromFilepath(inputFile, displayName, null);
  }

  /**
   * Functions the same as {@link DeepLClient#importTranslationMemoryFromFilepath(File, String)} but
   * gives up waiting for the import after the specified timeout.
   *
   * @param inputFile TMX file to import.
   * @param displayName Optional name for the resulting translation memory, defaults to the file
   *     name.
   * @param timeout Maximum time to wait for the import to finish, or <code>null</code> to wait
   *     indefinitely. The job status is polled every 5 seconds; the last wait is cut short at the
   *     timeout.
   * @return {@link TranslationMemoryJob} object for the completed import; its result carries the ID
   *     of the new translation memory.
   * @throws IllegalArgumentException If the input file does not exist.
   * @throws NullPointerException If inputFile is <code>null</code>; checked before any request, not
   *     wrapped.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws TranslationMemoryJobException If the input file cannot be read, the timeout is
   *     exceeded, the import fails, or any error occurs while communicating with the DeepL API.
   * @see DeepLClient#importTranslationMemoryFromFilepath(File, String)
   */
  public TranslationMemoryJob importTranslationMemoryFromFilepath(
      File inputFile, @Nullable String displayName, @Nullable Duration timeout)
      throws TranslationMemoryJobException, InterruptedException {
    byte[] fileContent = readTranslationMemoryFile(inputFile);
    String jobId = null;
    TranslationMemoryJob[] lastJob = new TranslationMemoryJob[1];
    try {
      TranslationMemoryImport translationMemoryImport =
          createTranslationMemoryImport(inputFile.getName(), fileContent.length, null, displayName);
      jobId = translationMemoryImport.getJobId();
      uploadTranslationMemoryFile(translationMemoryImport, fileContent);
      return waitTrackingLastJob(jobId, timeout, lastJob);
    } catch (InterruptedException exception) {
      throw exception;
    } catch (Exception exception) {
      throw translationMemoryJobError(exception, jobId, lastJob[0]);
    }
  }

  /**
   * Imports a TMX file as a new translation memory: creates the import job, uploads the file, and
   * waits for processing to finish.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param inputFile TMX file to import.
   * @param options Options for the import (display name, content type, timeout, extra body
   *     parameters), or <code>null</code>.
   * @return {@link TranslationMemoryJob} object for the completed import; its result carries the ID
   *     of the new translation memory.
   * @throws IllegalArgumentException If the input file does not exist.
   * @throws NullPointerException If inputFile is <code>null</code>; checked before any request, not
   *     wrapped.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws TranslationMemoryJobException If the input file cannot be read, the timeout is
   *     exceeded, the import fails, or any error occurs while communicating with the DeepL API.
   */
  public TranslationMemoryJob importTranslationMemoryFromFilepath(
      File inputFile, @Nullable ImportTranslationMemoryOptions options)
      throws TranslationMemoryJobException, InterruptedException {
    byte[] fileContent = readTranslationMemoryFile(inputFile);
    return importTranslationMemoryFromFilepath(fileContent, inputFile.getName(), options);
  }

  /**
   * Functions the same as {@link DeepLClient#importTranslationMemoryFromFilepath(File,
   * ImportTranslationMemoryOptions)} but takes the TMX file content and its name.
   *
   * @param fileContent Content of the TMX file to import.
   * @param fileName Name of the TMX file, for example "legal.tmx".
   * @param options Options for the import, or <code>null</code>.
   * @see DeepLClient#importTranslationMemoryFromFilepath(File, ImportTranslationMemoryOptions)
   * @throws NullPointerException If fileContent or fileName is <code>null</code>; checked before
   *     any request, not wrapped.
   */
  public TranslationMemoryJob importTranslationMemoryFromFilepath(
      byte[] fileContent, String fileName, @Nullable ImportTranslationMemoryOptions options)
      throws TranslationMemoryJobException, InterruptedException {
    Objects.requireNonNull(fileContent, "fileContent");
    Objects.requireNonNull(fileName, "fileName");
    CreateTranslationMemoryImportOptions createOptions = new CreateTranslationMemoryImportOptions();
    String contentType = TRANSLATION_MEMORY_FILE_CONTENT_TYPE;
    Duration timeout = null;
    if (options != null) {
      createOptions
          .setContentType(options.getContentType())
          .setDisplayName(options.getDisplayName())
          .setExtraBodyParameters(options.getExtraBodyParameters());
      if (options.getContentType() != null) {
        contentType = options.getContentType();
      }
      timeout = options.getTimeout();
    }
    String jobId = null;
    TranslationMemoryJob[] lastJob = new TranslationMemoryJob[1];
    try {
      TranslationMemoryImport translationMemoryImport =
          createTranslationMemoryImport(fileName, fileContent.length, createOptions);
      jobId = translationMemoryImport.getJobId();
      uploadTranslationMemoryFile(translationMemoryImport, fileContent, contentType);
      return waitTrackingLastJob(jobId, timeout, lastJob);
    } catch (InterruptedException exception) {
      throw exception;
    } catch (Exception exception) {
      throw translationMemoryJobError(exception, jobId, lastJob[0]);
    }
  }

  /**
   * Functions the same as {@link DeepLClient#importTranslationMemoryFromFilepath(byte[], String,
   * ImportTranslationMemoryOptions)} but with default options.
   *
   * @see DeepLClient#importTranslationMemoryFromFilepath(byte[], String,
   *     ImportTranslationMemoryOptions)
   */
  public TranslationMemoryJob importTranslationMemoryFromFilepath(
      byte[] fileContent, String fileName)
      throws TranslationMemoryJobException, InterruptedException {
    return importTranslationMemoryFromFilepath(fileContent, fileName, null);
  }

  /**
   * Functions the same as {@link DeepLClient#importTranslationMemoryFromFilepath(File, String)} but
   * lets the API name the translation memory after the file.
   *
   * @see DeepLClient#importTranslationMemoryFromFilepath(File, String)
   */
  public TranslationMemoryJob importTranslationMemoryFromFilepath(File inputFile)
      throws TranslationMemoryJobException, InterruptedException {
    return importTranslationMemoryFromFilepath(inputFile, (String) null);
  }

  /**
   * Exports a translation memory to a TMX file: creates the export job, waits for it to finish, and
   * writes the result to the specified output file.
   *
   * @param translationMemoryId ID of the translation memory to export.
   * @param outputFile File to write the exported translation memory to.
   * @return {@link TranslationMemoryJob} object for the completed export.
   * @throws NullPointerException If translationMemoryId or outputFile is <code>null</code>; checked
   *     before any request, not wrapped.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws TranslationMemoryJobException If the output path is occupied, its directory does not
   *     exist or the file cannot be created there (checked before the export job is created),
   *     writing the file fails, the export fails, or any error occurs while communicating with the
   *     DeepL API. A local file failure carries the {@link IOException} as its cause.
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      String translationMemoryId, File outputFile)
      throws TranslationMemoryJobException, InterruptedException {
    return exportTranslationMemoryToFilepath(translationMemoryId, outputFile, null);
  }

  /**
   * Functions the same as {@link DeepLClient#exportTranslationMemoryToFilepath(String, File)} but
   * gives up waiting for the export after the specified timeout.
   *
   * @param translationMemoryId ID of the translation memory to export.
   * @param outputFile File to write the exported translation memory to.
   * @param timeout Maximum time to wait for the export to finish, or <code>null</code> to wait
   *     indefinitely. The job status is polled every 5 seconds; the last wait is cut short at the
   *     timeout.
   * @return {@link TranslationMemoryJob} object for the completed export.
   * @throws NullPointerException If translationMemoryId or outputFile is <code>null</code>; checked
   *     before any request, not wrapped.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws TranslationMemoryJobException If the output path is occupied, its directory does not
   *     exist or the file cannot be created there (checked before the export job is created),
   *     writing the file fails, the timeout is exceeded, the export fails, or any error occurs
   *     while communicating with the DeepL API. A local file failure carries the {@link
   *     IOException} as its cause.
   * @see DeepLClient#exportTranslationMemoryToFilepath(String, File)
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      String translationMemoryId, File outputFile, @Nullable Duration timeout)
      throws TranslationMemoryJobException, InterruptedException {
    Objects.requireNonNull(translationMemoryId, "translationMemoryId");
    checkExportOutputFile(outputFile);
    String jobId = null;
    TranslationMemoryJob[] lastJob = new TranslationMemoryJob[1];
    try {
      jobId = createTranslationMemoryExport(translationMemoryId).getJobId();
      TranslationMemoryJob job = waitTrackingLastJob(jobId, timeout, lastJob);
      downloadTranslationMemoryExport(job, outputFile);
      return job;
    } catch (InterruptedException exception) {
      throw exception;
    } catch (Exception exception) {
      throw translationMemoryJobError(exception, jobId, lastJob[0]);
    }
  }

  /**
   * Functions the same as {@link DeepLClient#exportTranslationMemoryToFilepath(String, File)} but
   * accepts a {@link TranslationMemoryInfo} object. A <code>null</code> object throws {@link
   * NullPointerException}, not wrapped; an object without ID is wrapped in a {@link
   * TranslationMemoryJobException} without job ID, like every other failure.
   *
   * @see DeepLClient#exportTranslationMemoryToFilepath(String, File)
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      TranslationMemoryInfo translationMemory, File outputFile)
      throws TranslationMemoryJobException, InterruptedException {
    return exportTranslationMemoryToFilepath(
        translationMemoryIdForJob(translationMemory), outputFile);
  }

  /**
   * Functions the same as {@link DeepLClient#exportTranslationMemoryToFilepath(String, File,
   * Duration)} but accepts a {@link TranslationMemoryInfo} object. A <code>null</code> object
   * throws {@link NullPointerException}, not wrapped; an object without ID is wrapped in a {@link
   * TranslationMemoryJobException} without job ID, like every other failure.
   *
   * @see DeepLClient#exportTranslationMemoryToFilepath(String, File, Duration)
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      TranslationMemoryInfo translationMemory, File outputFile, @Nullable Duration timeout)
      throws TranslationMemoryJobException, InterruptedException {
    return exportTranslationMemoryToFilepath(
        translationMemoryIdForJob(translationMemory), outputFile, timeout);
  }

  /**
   * Functions the same as {@link DeepLClient#exportTranslationMemoryToFilepath(String, File,
   * Duration)} but writes the exported TMX file to the specified output stream, which is not
   * closed.
   *
   * @param translationMemoryId ID of the translation memory to export.
   * @param outputStream Stream to write the exported translation memory to.
   * @param timeout Maximum time to wait for the export to finish, or <code>null</code> to wait
   *     indefinitely.
   * @see DeepLClient#exportTranslationMemoryToFilepath(String, File, Duration)
   * @throws NullPointerException If translationMemoryId or outputStream is <code>null</code>;
   *     checked before any request, not wrapped.
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      String translationMemoryId, OutputStream outputStream, @Nullable Duration timeout)
      throws TranslationMemoryJobException, InterruptedException {
    Objects.requireNonNull(translationMemoryId, "translationMemoryId");
    Objects.requireNonNull(outputStream, "outputStream");
    String jobId = null;
    TranslationMemoryJob[] lastJob = new TranslationMemoryJob[1];
    try {
      jobId = createTranslationMemoryExport(translationMemoryId).getJobId();
      TranslationMemoryJob job = waitTrackingLastJob(jobId, timeout, lastJob);
      downloadTranslationMemoryExport(job, outputStream);
      return job;
    } catch (InterruptedException exception) {
      throw exception;
    } catch (Exception exception) {
      throw translationMemoryJobError(exception, jobId, lastJob[0]);
    }
  }

  /**
   * Functions the same as {@link DeepLClient#exportTranslationMemoryToFilepath(String,
   * OutputStream, Duration)} but waits indefinitely.
   *
   * @see DeepLClient#exportTranslationMemoryToFilepath(String, OutputStream, Duration)
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      String translationMemoryId, OutputStream outputStream)
      throws TranslationMemoryJobException, InterruptedException {
    return exportTranslationMemoryToFilepath(translationMemoryId, outputStream, null);
  }

  /**
   * Functions the same as {@link DeepLClient#exportTranslationMemoryToFilepath(String,
   * OutputStream, Duration)} but accepts a {@link TranslationMemoryInfo} object. A <code>null
   * </code> object throws {@link NullPointerException}, not wrapped; an object without ID is
   * wrapped in a {@link TranslationMemoryJobException} without job ID, like every other failure.
   *
   * @see DeepLClient#exportTranslationMemoryToFilepath(String, OutputStream, Duration)
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      TranslationMemoryInfo translationMemory,
      OutputStream outputStream,
      @Nullable Duration timeout)
      throws TranslationMemoryJobException, InterruptedException {
    return exportTranslationMemoryToFilepath(
        translationMemoryIdForJob(translationMemory), outputStream, timeout);
  }

  /**
   * Functions the same as {@link DeepLClient#exportTranslationMemoryToFilepath(String,
   * OutputStream, Duration)} but accepts a {@link TranslationMemoryInfo} object and waits
   * indefinitely. A <code>null</code> object throws {@link NullPointerException}, not wrapped; an
   * object without ID is wrapped in a {@link TranslationMemoryJobException} without job ID, like
   * every other failure.
   *
   * @see DeepLClient#exportTranslationMemoryToFilepath(String, OutputStream, Duration)
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      TranslationMemoryInfo translationMemory, OutputStream outputStream)
      throws TranslationMemoryJobException, InterruptedException {
    return exportTranslationMemoryToFilepath(
        translationMemoryIdForJob(translationMemory), outputStream, null);
  }

  /** Resolves the ID for the file helpers, which wrap every failure, argument errors included. */
  private static String translationMemoryIdForJob(TranslationMemoryInfo translationMemory)
      throws TranslationMemoryJobException {
    try {
      return translationMemoryId(translationMemory);
    } catch (IllegalArgumentException exception) {
      throw translationMemoryJobError(exception, null, null);
    }
  }

  /** Extracts the ID of the given translation memory. */
  private static String translationMemoryId(TranslationMemoryInfo translationMemory) {
    return idOf(
        "translationMemory", translationMemory, TranslationMemoryInfo::getTranslationMemoryId);
  }

  /** Validates the translation memory ID and builds its URL path with the ID escaped. */
  private String translationMemoryPath(String translationMemoryId) throws DeepLException {
    validateParameter("translationMemoryId", translationMemoryId);
    try {
      return "/v3/translation_memories/"
          + ApiClientHelper.encodeQueryComponent(translationMemoryId);
    } catch (UnsupportedEncodingException exception) {
      throw new DeepLException("Error while URL-encoding request", exception);
    }
  }

  /** Validates the translation memory job ID and builds its URL path with the ID escaped. */
  private String translationMemoryJobPath(String jobId) throws DeepLException {
    validateParameter("jobId", jobId);
    try {
      return "/v3/translation_memories/jobs/" + ApiClientHelper.encodeQueryComponent(jobId);
    } catch (UnsupportedEncodingException exception) {
      throw new DeepLException("Error while URL-encoding request", exception);
    }
  }

  /** Extracts the ID of the given translation memory job. */
  private static String translationMemoryJobId(TranslationMemoryJob job) {
    return idOf("job", job, TranslationMemoryJob::getJobId);
  }

  /**
   * Reads a TMX file for import; a missing file is an argument error, raised before any request.
   */
  private static byte[] readTranslationMemoryFile(File inputFile)
      throws TranslationMemoryJobException {
    Objects.requireNonNull(inputFile, "inputFile");
    if (!inputFile.exists()) {
      throw new IllegalArgumentException("inputFile must be an existing file");
    }
    try {
      return Files.readAllBytes(inputFile.toPath());
    } catch (IOException exception) {
      throw translationMemoryJobError(exception, null, null);
    }
  }

  /** Checks the output file of an export so a bad path fails before the export job is created. */
  private static void checkExportOutputFile(File outputFile) throws TranslationMemoryJobException {
    Objects.requireNonNull(outputFile, "outputFile");
    if (outputFile.exists()) {
      throw translationMemoryJobError(
          new IOException("File already exists at output path"), null, null);
    }
    File directory = outputFile.getAbsoluteFile().getParentFile();
    if (directory == null || !directory.isDirectory()) {
      throw translationMemoryJobError(
          new FileNotFoundException("Directory of output path does not exist"), null, null);
    }
    // Creating and deleting the file proves it can be written, e.g. not in a read-only directory.
    try {
      Files.createFile(outputFile.toPath());
      Files.delete(outputFile.toPath());
    } catch (IOException exception) {
      throw translationMemoryJobError(exception, null, null);
    }
  }

  /** Wraps a failure of the translation memory file helpers with the job ID and last job. */
  private static TranslationMemoryJobException translationMemoryJobError(
      Exception exception, @Nullable String jobId, @Nullable TranslationMemoryJob job) {
    return new TranslationMemoryJobException(exception.getMessage(), exception, jobId, job);
  }

  /** Waits for a file helper's job, keeping the last fetched job status in {@code lastJob[0]}. */
  private TranslationMemoryJob waitTrackingLastJob(
      String jobId, @Nullable Duration timeout, TranslationMemoryJob[] lastJob)
      throws DeepLException, InterruptedException {
    TranslationMemoryJob[] previous = fetchedTranslationMemoryJobSink.get();
    fetchedTranslationMemoryJobSink.set(lastJob);
    try {
      TranslationMemoryJob job = waitUntilTranslationMemoryJobDone(jobId, timeout);
      lastJob[0] = job;
      return job;
    } finally {
      if (previous == null) {
        fetchedTranslationMemoryJobSink.remove();
      } else {
        fetchedTranslationMemoryJobSink.set(previous);
      }
    }
  }

  /** Reports a fetched job status to a waiting file helper, if any. */
  private TranslationMemoryJob recordFetchedJob(TranslationMemoryJob job) {
    TranslationMemoryJob[] sink = fetchedTranslationMemoryJobSink.get();
    if (sink != null) {
      sink[0] = job;
    }
    return job;
  }

  /**
   * translateDocument wraps the language errors of its upload, like every failure but a missing
   * file.
   */
  private static void checkDocumentLanguages(@Nullable String sourceLang, String targetLang)
      throws DocumentTranslationException {
    try {
      checkValidLanguages(sourceLang, targetLang);
    } catch (IllegalArgumentException exception) {
      throw new DocumentTranslationException(
          "Error occurred during document translation: " + exception.getMessage(), exception, null);
    }
  }

  /** A missing input file is an argument error, raised before any request. */
  private static void checkDocumentInputFile(File inputFile) {
    if (!inputFile.exists()) {
      throw new IllegalArgumentException("inputFile must be an existing file");
    }
  }

  /** Extracts the ID of the given Spoken Terms collection. */
  private static String spokenTermsId(SpokenTerms spokenTerms) {
    return idOf("spokenTerms", spokenTerms, SpokenTerms::getSpokenTermsId);
  }

  /** Validates the Spoken Terms collection ID and builds its URL path with the ID escaped. */
  private String spokenTermsPath(String spokenTermsId) throws DeepLException {
    validateParameter("spokenTermsId", spokenTermsId);
    try {
      return "/v3/spoken-terms/" + ApiClientHelper.encodeQueryComponent(spokenTermsId);
    } catch (UnsupportedEncodingException exception) {
      throw new DeepLException("Error while URL-encoding request", exception);
    }
  }

  /** Serializes a JSON request body, adding the extra body parameters last. */
  private String jsonBodyWithExtraParameters(
      Map<String, Object> requestData, @Nullable BaseRequestOptions options) {
    Gson gson = jsonParser.getGson();
    // The SDK's own null fields are dropped here; an extra null value is sent as JSON null.
    JsonObject body = gson.toJsonTree(requestData).getAsJsonObject();
    if (options != null && options.getExtraBodyParameters() != null) {
      for (Map.Entry<String, ?> entry : options.getExtraBodyParameters().entrySet()) {
        Object value = entry.getValue();
        body.add(entry.getKey(), value == null ? JsonNull.INSTANCE : gson.toJsonTree(value));
      }
    }
    // JsonObject.toString() keeps explicit nulls, unlike Gson.toJson() with default settings.
    return body.toString();
  }

  /** Extracts the ID of the given style profile. */
  private static String styleProfileId(StyleProfile styleProfile) {
    return idOf("styleProfile", styleProfile, StyleProfile::getStyleProfileId);
  }

  /** Validates the style profile ID and builds its URL path with the ID escaped. */
  private String styleProfilePath(String styleProfileId) throws DeepLException {
    validateParameter("styleProfileId", styleProfileId);
    try {
      return "/v3/style_profiles/" + ApiClientHelper.encodeQueryComponent(styleProfileId);
    } catch (UnsupportedEncodingException exception) {
      throw new DeepLException("Error while URL-encoding request", exception);
    }
  }

  /** Builds the JSON body of a style profile request, adding the extra body parameters last. */
  private String createStyleProfileBody(
      @Nullable String name, @Nullable CreateStyleProfileOptions options) {
    Map<String, Object> body = new LinkedHashMap<>();
    if (name != null) {
      body.put("name", name);
    }
    if (options != null) {
      putIfNotNull(body, "description", options.getDescription());
      putIfNotNull(body, "industry", options.getIndustry());
      putIfNotNull(body, "glossary_ids", options.getGlossaryIds());
      putIfNotNull(body, "style_rule_ids", options.getStyleRuleIds());
      putIfNotNull(body, "translation_memory_ids", options.getTranslationMemoryIds());
      putIfNotNull(
          body, "glossary_priority_order_enabled", options.getGlossaryPriorityOrderEnabled());
    }
    return jsonBodyWithExtraParameters(body, options);
  }

  private static void putIfNotNull(Map<String, Object> body, String key, @Nullable Object value) {
    if (value != null) {
      body.put(key, value);
    }
  }

  /** Builds the query string that selects the term list of the given language. */
  private static String langQueryString(String lang) {
    validateParameter("lang", lang);
    List<KeyValuePair<String, String>> queryParams = new ArrayList<>();
    queryParams.add(new KeyValuePair<>("lang", lang));
    return ApiClientHelper.createQueryString(queryParams);
  }

  /**
   * Builds the JSON object of one term list; the API expects the terms newline-separated, so each
   * term is trimmed and then checked like a glossary term: a term containing a newline would split.
   */
  private static Map<String, Object> createSpokenTermsListData(SpokenTermsEntries entries) {
    Objects.requireNonNull(entries, "entries");
    validateParameter("lang", entries.getLang());
    List<String> terms = new ArrayList<>();
    for (String term : entries.getEntries()) {
      Objects.requireNonNull(term, "entries");
      String trimmed = trimWhitespace(term);
      GlossaryEntries.validateGlossaryTerm(trimmed);
      terms.add(trimmed);
    }
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("lang", entries.getLang());
    data.put("entries", String.join("\n", terms));
    return data;
  }

  /** Removes leading and trailing whitespace as defined by {@link Character#isWhitespace}. */
  private static String trimWhitespace(String term) {
    int start = 0;
    int end = term.length();
    while (start < end && Character.isWhitespace(term.charAt(start))) {
      start++;
    }
    while (end > start && Character.isWhitespace(term.charAt(end - 1))) {
      end--;
    }
    return term.substring(start, end);
  }

  private static List<Map<String, Object>> createSpokenTermsListsData(
      List<SpokenTermsEntries> termLists) {
    Objects.requireNonNull(termLists, "termLists");
    List<Map<String, Object>> data = new ArrayList<>();
    for (SpokenTermsEntries entries : termLists) {
      Objects.requireNonNull(entries, "termLists");
      data.add(createSpokenTermsListData(entries));
    }
    return data;
  }

  /**
   * Creates a new style rule with the specified details and returns a {@link StyleRuleInfo} object
   * with details about the newly created style rule.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param name User-defined name for the style rule.
   * @param language Language code for the style rule (e.g. "en", "de").
   * @param configuredRules Optional configured rules, as a map from category to rule to value in
   *     the API's snake_case form (e.g. {@code "dates_and_times" -> {"calendar_era" ->
   *     "use_bc_and_ad"}}), sent as given.
   * @param customInstructions Optional list of custom instructions for the style rule; the source
   *     language of each must be <code>null</code> or non-empty.
   * @param options Options for the request, or <code>null</code>; its extra body parameters are
   *     added to the request last.
   * @return {@link StyleRuleInfo} object with details about the newly created style rule.
   * @throws NullPointerException If name or language is <code>null</code>, or customInstructions
   *     contains <code>null</code> or an instruction with a <code>null</code> label or prompt;
   *     checked before any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo createStyleRule(
      String name,
      String language,
      @Nullable Map<String, Map<String, String>> configuredRules,
      @Nullable List<CustomInstruction> customInstructions,
      @Nullable CreateStyleRuleOptions options)
      throws DeepLException, InterruptedException {
    validateParameter("name", name);
    validateParameter("language", language);
    if (customInstructions != null) {
      for (CustomInstruction instruction : customInstructions) {
        Objects.requireNonNull(instruction, "customInstructions");
        Objects.requireNonNull(instruction.getLabel(), "label");
        Objects.requireNonNull(instruction.getPrompt(), "prompt");
        validateOptionalLanguage("sourceLanguage", instruction.getSourceLanguage());
      }
    }
    Map<String, Object> requestData = new LinkedHashMap<>();
    requestData.put("name", name);
    requestData.put("language", language);
    putIfNotNull(requestData, "configured_rules", configuredRules);
    putIfNotNull(requestData, "custom_instructions", customInstructions);
    String jsonBody = jsonBodyWithExtraParameters(requestData, options);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestRetryingOnlyUnprocessed("/v3/style_rules", jsonBody);
    checkResponse(response, false);
    return jsonParser.parseStyleRuleInfo(response.getBody());
  }

  /**
   * Retrieves information about the style rule with the specified ID and returns a {@link
   * StyleRuleInfo} object containing details.
   *
   * @param styleId ID of the style rule to retrieve.
   * @return {@link StyleRuleInfo} object with details about the specified style rule.
   * @throws NotFoundException If no style rule with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo getStyleRule(String styleId)
      throws DeepLException, InterruptedException, NotFoundException {
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(styleRulePath(styleId));
    checkResponse(response, false);
    return jsonParser.parseStyleRuleInfo(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getStyleRule(String)} but accepts a {@link
   * StyleRuleInfo} object.
   *
   * @param styleRule The style rule to retrieve.
   * @return {@link StyleRuleInfo} object with details about the specified style rule.
   * @throws NotFoundException If the style rule is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo getStyleRule(StyleRuleInfo styleRule)
      throws DeepLException, InterruptedException, NotFoundException {
    return getStyleRule(styleRuleId(styleRule));
  }

  /**
   * Updates the name of the style rule with the specified ID and returns the updated {@link
   * StyleRuleInfo} object.
   *
   * @param styleId ID of the style rule to update.
   * @param name New name for the style rule.
   * @return {@link StyleRuleInfo} object with updated details about the style rule.
   * @throws NotFoundException If no style rule with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo updateStyleRuleName(String styleId, String name)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleName(styleId, name, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleName(String, String)} but accepts
   * options; the extra body parameters of the options are added to the request last.
   *
   * @param styleId ID of the style rule to update.
   * @param name New name for the style rule.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link StyleRuleInfo} object with updated details about the style rule.
   * @throws NotFoundException If no style rule with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo updateStyleRuleName(
      String styleId, String name, @Nullable UpdateStyleRuleNameOptions options)
      throws DeepLException, InterruptedException, NotFoundException {
    String relativeUrl = styleRulePath(styleId);
    validateParameter("name", name);
    Map<String, Object> requestData = new LinkedHashMap<>();
    requestData.put("name", name);
    String jsonBody = jsonBodyWithExtraParameters(requestData, options);
    HttpResponse response =
        httpClientWrapper.sendJsonPatchRequestWithBackoff(relativeUrl, jsonBody);
    checkResponse(response, false);
    return jsonParser.parseStyleRuleInfo(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleName(String, String)} but accepts a
   * {@link StyleRuleInfo} object.
   *
   * @param styleRule The style rule to update.
   * @param name New name for the style rule.
   * @return {@link StyleRuleInfo} object with updated details about the style rule.
   * @throws NotFoundException If the style rule is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo updateStyleRuleName(StyleRuleInfo styleRule, String name)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleName(styleRuleId(styleRule), name, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleName(String, String,
   * UpdateStyleRuleNameOptions)} but accepts a {@link StyleRuleInfo} object.
   *
   * @param styleRule The style rule to update.
   * @param name New name for the style rule.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link StyleRuleInfo} object with updated details about the style rule.
   * @throws NotFoundException If the style rule is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo updateStyleRuleName(
      StyleRuleInfo styleRule, String name, @Nullable UpdateStyleRuleNameOptions options)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleName(styleRuleId(styleRule), name, options);
  }

  /**
   * Deletes the style rule with the specified ID.
   *
   * @param styleId ID of the style rule to delete.
   * @throws NotFoundException If no style rule with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteStyleRule(String styleId)
      throws DeepLException, InterruptedException, NotFoundException {
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(styleRulePath(styleId));
    checkResponse(response, false);
  }

  /**
   * Functions the same as {@link DeepLClient#deleteStyleRule(String)} but accepts a {@link
   * StyleRuleInfo} object.
   *
   * @param styleRule The style rule to delete.
   * @throws NotFoundException If the style rule is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteStyleRule(StyleRuleInfo styleRule)
      throws DeepLException, InterruptedException, NotFoundException {
    deleteStyleRule(styleRuleId(styleRule));
  }

  /**
   * Replaces the configured rules of the style rule with the specified ID and returns the updated
   * {@link StyleRuleInfo} object.
   *
   * @param styleId ID of the style rule to update.
   * @param configuredRules The new configured rules, as a map from category to rule to value in the
   *     API's snake_case form, sent as given as the request body.
   * @param options Options for the request, or <code>null</code>; its extra body parameters are
   *     added to the request body as further top-level keys.
   * @return {@link StyleRuleInfo} object with updated details about the style rule.
   * @throws NotFoundException If no style rule with the given ID is found.
   * @throws NullPointerException If styleId or configuredRules is <code>null</code>; checked before
   *     any request.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo updateStyleRuleConfiguredRules(
      String styleId,
      Map<String, Map<String, String>> configuredRules,
      @Nullable UpdateStyleRuleConfiguredRulesOptions options)
      throws DeepLException, InterruptedException, NotFoundException {
    String relativeUrl = styleRulePath(styleId);
    Objects.requireNonNull(configuredRules, "configuredRules");

    return sendStyleRuleConfiguredRules(
        relativeUrl, jsonBodyWithExtraParameters(new LinkedHashMap<>(configuredRules), options));
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleConfiguredRules(String, Map,
   * UpdateStyleRuleConfiguredRulesOptions)} but accepts a {@link StyleRuleInfo} object.
   *
   * @param styleRule The style rule to update.
   * @param configuredRules The new configured rules, sent as given as the request body.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link StyleRuleInfo} object with updated details about the style rule.
   * @throws NotFoundException If the style rule is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo updateStyleRuleConfiguredRules(
      StyleRuleInfo styleRule,
      Map<String, Map<String, String>> configuredRules,
      @Nullable UpdateStyleRuleConfiguredRulesOptions options)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleConfiguredRules(styleRuleId(styleRule), configuredRules, options);
  }

  private StyleRuleInfo sendStyleRuleConfiguredRules(String styleRulePath, String jsonBody)
      throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff(
            "PUT", styleRulePath + "/configured_rules", jsonBody);
    checkResponse(response, false);
    return jsonParser.parseStyleRuleInfo(response.getBody());
  }

  /**
   * Creates a new custom instruction for the style rule with the specified ID and returns the
   * created {@link CustomInstruction} object.
   *
   * <p>The create request is retried only when the request was not sent: after HTTP 429 or a
   * failure while connecting (for example a DNS resolution failure, a refused connection, an
   * unreachable host or network, or a connect timeout), but not after a TLS or proxy failure.
   *
   * @param styleId ID of the style rule to add the custom instruction to.
   * @param label Label for the custom instruction.
   * @param prompt Prompt text for the custom instruction.
   * @param sourceLanguage Optional source language code for the custom instruction, or {@code null}
   *     to leave it unset; must not be empty.
   * @return {@link CustomInstruction} object with details about the newly created custom
   *     instruction.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction createStyleRuleCustomInstruction(
      String styleId, String label, String prompt, @Nullable String sourceLanguage)
      throws DeepLException, InterruptedException {
    return createStyleRuleCustomInstruction(styleId, label, prompt, sourceLanguage, null);
  }

  /**
   * Functions the same as {@link DeepLClient#createStyleRuleCustomInstruction(String, String,
   * String, String)} but accepts options; the extra body parameters of the options are added to the
   * request last.
   *
   * @param styleId ID of the style rule to add the custom instruction to.
   * @param label Label for the custom instruction.
   * @param prompt Prompt text for the custom instruction.
   * @param sourceLanguage Optional source language code for the custom instruction, or {@code null}
   *     to leave it unset; must not be empty.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link CustomInstruction} object with details about the newly created custom
   *     instruction.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction createStyleRuleCustomInstruction(
      String styleId,
      String label,
      String prompt,
      @Nullable String sourceLanguage,
      @Nullable CreateStyleRuleCustomInstructionOptions options)
      throws DeepLException, InterruptedException {
    String relativeUrl = styleRulePath(styleId) + "/custom_instructions";
    String jsonBody = customInstructionBody(label, prompt, sourceLanguage, options);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestRetryingOnlyUnprocessed(relativeUrl, jsonBody);
    checkResponse(response, false);
    return jsonParser.parseCustomInstruction(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#createStyleRuleCustomInstruction(String, String,
   * String, String)} but accepts a {@link StyleRuleInfo} object.
   *
   * @param styleRule The style rule to add the custom instruction to.
   * @param label Label for the custom instruction.
   * @param prompt Prompt text for the custom instruction.
   * @param sourceLanguage Optional source language code for the custom instruction, or {@code null}
   *     to leave it unset; must not be empty.
   * @return {@link CustomInstruction} object with details about the newly created custom
   *     instruction.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction createStyleRuleCustomInstruction(
      StyleRuleInfo styleRule, String label, String prompt, @Nullable String sourceLanguage)
      throws DeepLException, InterruptedException {
    return createStyleRuleCustomInstruction(
        styleRuleId(styleRule), label, prompt, sourceLanguage, null);
  }

  /**
   * Functions the same as {@link DeepLClient#createStyleRuleCustomInstruction(String, String,
   * String, String, CreateStyleRuleCustomInstructionOptions)} but accepts a {@link StyleRuleInfo}
   * object.
   *
   * @param styleRule The style rule to add the custom instruction to.
   * @param label Label for the custom instruction.
   * @param prompt Prompt text for the custom instruction.
   * @param sourceLanguage Optional source language code for the custom instruction, or {@code null}
   *     to leave it unset; must not be empty.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link CustomInstruction} object with details about the newly created custom
   *     instruction.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction createStyleRuleCustomInstruction(
      StyleRuleInfo styleRule,
      String label,
      String prompt,
      @Nullable String sourceLanguage,
      @Nullable CreateStyleRuleCustomInstructionOptions options)
      throws DeepLException, InterruptedException {
    return createStyleRuleCustomInstruction(
        styleRuleId(styleRule), label, prompt, sourceLanguage, options);
  }

  /**
   * Retrieves information about the custom instruction with the specified ID within the style rule
   * with the specified ID and returns a {@link CustomInstruction} object containing details.
   *
   * @param styleId ID of the style rule containing the custom instruction.
   * @param instructionId ID of the custom instruction to retrieve.
   * @return {@link CustomInstruction} object with details about the specified custom instruction.
   * @throws NotFoundException If no style rule or custom instruction with the given IDs is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction getStyleRuleCustomInstruction(String styleId, String instructionId)
      throws DeepLException, InterruptedException, NotFoundException {
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(customInstructionPath(styleId, instructionId));
    checkResponse(response, false);
    return jsonParser.parseCustomInstruction(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getStyleRuleCustomInstruction(String, String)} but
   * accepts a {@link StyleRuleInfo} object.
   *
   * @param styleRule The style rule containing the custom instruction.
   * @param instructionId ID of the custom instruction to retrieve.
   * @return {@link CustomInstruction} object with details about the specified custom instruction.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction getStyleRuleCustomInstruction(
      StyleRuleInfo styleRule, String instructionId)
      throws DeepLException, InterruptedException, NotFoundException {
    return getStyleRuleCustomInstruction(styleRuleId(styleRule), instructionId);
  }

  /**
   * Functions the same as {@link DeepLClient#getStyleRuleCustomInstruction(String, String)} but
   * accepts a {@link CustomInstruction} object.
   *
   * @param styleId ID of the style rule containing the custom instruction.
   * @param instruction The custom instruction to retrieve.
   * @return {@link CustomInstruction} object with details about the specified custom instruction.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction getStyleRuleCustomInstruction(
      String styleId, CustomInstruction instruction)
      throws DeepLException, InterruptedException, NotFoundException {
    return getStyleRuleCustomInstruction(styleId, customInstructionId(instruction));
  }

  /**
   * Functions the same as {@link DeepLClient#getStyleRuleCustomInstruction(String, String)} but
   * accepts a {@link StyleRuleInfo} and a {@link CustomInstruction} object.
   *
   * @param styleRule The style rule containing the custom instruction.
   * @param instruction The custom instruction to retrieve.
   * @return {@link CustomInstruction} object with details about the specified custom instruction.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction getStyleRuleCustomInstruction(
      StyleRuleInfo styleRule, CustomInstruction instruction)
      throws DeepLException, InterruptedException, NotFoundException {
    return getStyleRuleCustomInstruction(styleRuleId(styleRule), customInstructionId(instruction));
  }

  /**
   * Updates the custom instruction with the specified ID within the style rule with the specified
   * ID and returns the updated {@link CustomInstruction} object.
   *
   * @param styleId ID of the style rule containing the custom instruction.
   * @param instructionId ID of the custom instruction to update.
   * @param label New label for the custom instruction.
   * @param prompt New prompt text for the custom instruction.
   * @param sourceLanguage Optional new source language code for the custom instruction, or {@code
   *     null} to leave it unset; must not be empty.
   * @return {@link CustomInstruction} object with updated details about the custom instruction.
   * @throws NotFoundException If no style rule or custom instruction with the given IDs is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction updateStyleRuleCustomInstruction(
      String styleId,
      String instructionId,
      String label,
      String prompt,
      @Nullable String sourceLanguage)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleCustomInstruction(
        styleId, instructionId, label, prompt, sourceLanguage, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleCustomInstruction(String, String,
   * String, String, String)} but accepts options; the extra body parameters of the options are
   * added to the request last.
   *
   * @param styleId ID of the style rule containing the custom instruction.
   * @param instructionId ID of the custom instruction to update.
   * @param label New label for the custom instruction.
   * @param prompt New prompt text for the custom instruction.
   * @param sourceLanguage Optional new source language code for the custom instruction, or {@code
   *     null} to leave it unset; must not be empty.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link CustomInstruction} object with updated details about the custom instruction.
   * @throws NotFoundException If no style rule or custom instruction with the given IDs is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction updateStyleRuleCustomInstruction(
      String styleId,
      String instructionId,
      String label,
      String prompt,
      @Nullable String sourceLanguage,
      @Nullable UpdateStyleRuleCustomInstructionOptions options)
      throws DeepLException, InterruptedException, NotFoundException {
    String relativeUrl = customInstructionPath(styleId, instructionId);
    String jsonBody = customInstructionBody(label, prompt, sourceLanguage, options);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff("PUT", relativeUrl, jsonBody);
    checkResponse(response, false);
    return jsonParser.parseCustomInstruction(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleCustomInstruction(String, String,
   * String, String, String)} but accepts a {@link StyleRuleInfo} object.
   *
   * @param styleRule The style rule containing the custom instruction.
   * @param instructionId ID of the custom instruction to update.
   * @param label New label for the custom instruction.
   * @param prompt New prompt text for the custom instruction.
   * @param sourceLanguage Optional new source language code for the custom instruction, or {@code
   *     null} to leave it unset; must not be empty.
   * @return {@link CustomInstruction} object with updated details about the custom instruction.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction updateStyleRuleCustomInstruction(
      StyleRuleInfo styleRule,
      String instructionId,
      String label,
      String prompt,
      @Nullable String sourceLanguage)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleCustomInstruction(
        styleRuleId(styleRule), instructionId, label, prompt, sourceLanguage, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleCustomInstruction(String, String,
   * String, String, String, UpdateStyleRuleCustomInstructionOptions)} but accepts a {@link
   * StyleRuleInfo} object.
   *
   * @param styleRule The style rule containing the custom instruction.
   * @param instructionId ID of the custom instruction to update.
   * @param label New label for the custom instruction.
   * @param prompt New prompt text for the custom instruction.
   * @param sourceLanguage Optional new source language code for the custom instruction, or {@code
   *     null} to leave it unset; must not be empty.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link CustomInstruction} object with updated details about the custom instruction.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction updateStyleRuleCustomInstruction(
      StyleRuleInfo styleRule,
      String instructionId,
      String label,
      String prompt,
      @Nullable String sourceLanguage,
      @Nullable UpdateStyleRuleCustomInstructionOptions options)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleCustomInstruction(
        styleRuleId(styleRule), instructionId, label, prompt, sourceLanguage, options);
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleCustomInstruction(String, String,
   * String, String, String)} but accepts a {@link CustomInstruction} object.
   *
   * @param styleId ID of the style rule containing the custom instruction.
   * @param instruction The custom instruction to update.
   * @param label New label for the custom instruction.
   * @param prompt New prompt text for the custom instruction.
   * @param sourceLanguage Optional new source language code for the custom instruction, or {@code
   *     null} to leave it unset; must not be empty.
   * @return {@link CustomInstruction} object with updated details about the custom instruction.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction updateStyleRuleCustomInstruction(
      String styleId,
      CustomInstruction instruction,
      String label,
      String prompt,
      @Nullable String sourceLanguage)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleCustomInstruction(
        styleId, customInstructionId(instruction), label, prompt, sourceLanguage, null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleCustomInstruction(String, String,
   * String, String, String, UpdateStyleRuleCustomInstructionOptions)} but accepts a {@link
   * CustomInstruction} object.
   *
   * @param styleId ID of the style rule containing the custom instruction.
   * @param instruction The custom instruction to update.
   * @param label New label for the custom instruction.
   * @param prompt New prompt text for the custom instruction.
   * @param sourceLanguage Optional new source language code for the custom instruction, or {@code
   *     null} to leave it unset; must not be empty.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link CustomInstruction} object with updated details about the custom instruction.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction updateStyleRuleCustomInstruction(
      String styleId,
      CustomInstruction instruction,
      String label,
      String prompt,
      @Nullable String sourceLanguage,
      @Nullable UpdateStyleRuleCustomInstructionOptions options)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleCustomInstruction(
        styleId, customInstructionId(instruction), label, prompt, sourceLanguage, options);
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleCustomInstruction(String, String,
   * String, String, String)} but accepts a {@link StyleRuleInfo} and a {@link CustomInstruction}
   * object.
   *
   * @param styleRule The style rule containing the custom instruction.
   * @param instruction The custom instruction to update.
   * @param label New label for the custom instruction.
   * @param prompt New prompt text for the custom instruction.
   * @param sourceLanguage Optional new source language code for the custom instruction, or {@code
   *     null} to leave it unset; must not be empty.
   * @return {@link CustomInstruction} object with updated details about the custom instruction.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction updateStyleRuleCustomInstruction(
      StyleRuleInfo styleRule,
      CustomInstruction instruction,
      String label,
      String prompt,
      @Nullable String sourceLanguage)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleCustomInstruction(
        styleRuleId(styleRule),
        customInstructionId(instruction),
        label,
        prompt,
        sourceLanguage,
        null);
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleRuleCustomInstruction(String, String,
   * String, String, String, UpdateStyleRuleCustomInstructionOptions)} but accepts a {@link
   * StyleRuleInfo} and a {@link CustomInstruction} object.
   *
   * @param styleRule The style rule containing the custom instruction.
   * @param instruction The custom instruction to update.
   * @param label New label for the custom instruction.
   * @param prompt New prompt text for the custom instruction.
   * @param sourceLanguage Optional new source language code for the custom instruction, or {@code
   *     null} to leave it unset; must not be empty.
   * @param options Options for the request, or <code>null</code>.
   * @return {@link CustomInstruction} object with updated details about the custom instruction.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction updateStyleRuleCustomInstruction(
      StyleRuleInfo styleRule,
      CustomInstruction instruction,
      String label,
      String prompt,
      @Nullable String sourceLanguage,
      @Nullable UpdateStyleRuleCustomInstructionOptions options)
      throws DeepLException, InterruptedException, NotFoundException {
    return updateStyleRuleCustomInstruction(
        styleRuleId(styleRule),
        customInstructionId(instruction),
        label,
        prompt,
        sourceLanguage,
        options);
  }

  /**
   * Deletes the custom instruction with the specified ID from the style rule with the specified ID.
   *
   * @param styleId ID of the style rule containing the custom instruction.
   * @param instructionId ID of the custom instruction to delete.
   * @throws NotFoundException If no style rule or custom instruction with the given IDs is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteStyleRuleCustomInstruction(String styleId, String instructionId)
      throws DeepLException, InterruptedException, NotFoundException {
    HttpResponse response =
        httpClientWrapper.sendDeleteRequestWithBackoff(
            customInstructionPath(styleId, instructionId));
    checkResponse(response, false);
  }

  /**
   * Functions the same as {@link DeepLClient#deleteStyleRuleCustomInstruction(String, String)} but
   * accepts a {@link StyleRuleInfo} object.
   *
   * @param styleRule The style rule containing the custom instruction.
   * @param instructionId ID of the custom instruction to delete.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteStyleRuleCustomInstruction(StyleRuleInfo styleRule, String instructionId)
      throws DeepLException, InterruptedException, NotFoundException {
    deleteStyleRuleCustomInstruction(styleRuleId(styleRule), instructionId);
  }

  /**
   * Functions the same as {@link DeepLClient#deleteStyleRuleCustomInstruction(String, String)} but
   * accepts a {@link CustomInstruction} object.
   *
   * @param styleId ID of the style rule containing the custom instruction.
   * @param instruction The custom instruction to delete.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteStyleRuleCustomInstruction(String styleId, CustomInstruction instruction)
      throws DeepLException, InterruptedException, NotFoundException {
    deleteStyleRuleCustomInstruction(styleId, customInstructionId(instruction));
  }

  /**
   * Functions the same as {@link DeepLClient#deleteStyleRuleCustomInstruction(String, String)} but
   * accepts a {@link StyleRuleInfo} and a {@link CustomInstruction} object.
   *
   * @param styleRule The style rule containing the custom instruction.
   * @param instruction The custom instruction to delete.
   * @throws NotFoundException If the style rule or custom instruction is not found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteStyleRuleCustomInstruction(
      StyleRuleInfo styleRule, CustomInstruction instruction)
      throws DeepLException, InterruptedException, NotFoundException {
    deleteStyleRuleCustomInstruction(styleRuleId(styleRule), customInstructionId(instruction));
  }

  /** Extracts the ID of the given style rule. */
  private static String styleRuleId(StyleRuleInfo styleRule) {
    return idOf("styleRule", styleRule, StyleRuleInfo::getStyleId);
  }

  /** Extracts the ID of the given custom instruction. */
  private static String customInstructionId(CustomInstruction instruction) {
    return idOf("instruction", instruction, CustomInstruction::getId);
  }

  /** Validates the style rule ID and builds its URL path with the ID escaped. */
  private String styleRulePath(String styleId) throws DeepLException {
    validateParameter("styleId", styleId);
    try {
      return "/v3/style_rules/" + ApiClientHelper.encodeQueryComponent(styleId);
    } catch (UnsupportedEncodingException exception) {
      throw new DeepLException("Error while URL-encoding request", exception);
    }
  }

  /** Validates both IDs and builds the custom instruction URL path with the IDs escaped. */
  private String customInstructionPath(String styleId, String instructionId) throws DeepLException {
    String styleRulePath = styleRulePath(styleId);
    validateParameter("instructionId", instructionId);
    try {
      return styleRulePath
          + "/custom_instructions/"
          + ApiClientHelper.encodeQueryComponent(instructionId);
    } catch (UnsupportedEncodingException exception) {
      throw new DeepLException("Error while URL-encoding request", exception);
    }
  }

  /**
   * Builds the JSON body of a custom instruction request, adding the extra body parameters last.
   */
  private String customInstructionBody(
      String label,
      String prompt,
      @Nullable String sourceLanguage,
      @Nullable BaseRequestOptions options) {
    validateParameter("label", label);
    validateParameter("prompt", prompt);
    validateOptionalLanguage("sourceLanguage", sourceLanguage);
    Map<String, Object> requestData = new LinkedHashMap<>();
    requestData.put("label", label);
    requestData.put("prompt", prompt);
    putIfNotNull(requestData, "source_language", sourceLanguage);
    return jsonBodyWithExtraParameters(requestData, options);
  }

  /** Creates a glossary with given details. */
  private MultilingualGlossaryInfo createGlossaryFromCsvInternal(
      String name,
      String sourceLanguageCode,
      String targetLanguageCode,
      String entries,
      @Nullable CreateMultilingualGlossaryOptions options)
      throws DeepLException, InterruptedException {
    validateParameter("name", name);
    Objects.requireNonNull(entries, "csvContent");
    List<Map<String, Object>> dictionaries = new ArrayList<>();
    dictionaries.add(
        createGlossaryDictionaryData(sourceLanguageCode, targetLanguageCode, entries, "csv"));
    HttpResponse response =
        httpClientWrapper.sendJsonRequestRetryingOnlyUnprocessed(
            "/v3/glossaries", createGlossaryBody(name, dictionaries, options));
    checkResponse(response, false);
    return jsonParser.parseMultilingualGlossaryInfo(response.getBody());
  }

  /**
   * Gets the entries in the glossary with the specified ID for the given source and target
   * languages
   */
  private MultilingualGlossaryDictionaryInfo replaceGlossaryDictionaryInternal(
      String glossaryId,
      String sourceLanguageCode,
      String targetLanguageCode,
      String entries,
      String entriesFormat,
      @Nullable ReplaceMultilingualGlossaryDictionaryOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    validateParameter("glossaryId", glossaryId);
    validateParameter("sourceLanguageCode", sourceLanguageCode);
    validateParameter("targetLanguageCode", targetLanguageCode);
    validateParameter("entries", entries);

    Map<String, Object> requestData =
        createGlossaryDictionaryData(
            sourceLanguageCode, targetLanguageCode, entries, entriesFormat);
    String jsonBody = jsonBodyWithExtraParameters(requestData, options);
    String relativeUrl = multilingualGlossaryPath(glossaryId) + "/dictionaries";
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff("PUT", relativeUrl, jsonBody);
    checkResponse(response, false);
    return jsonParser.parseMultilingualGlossaryDictionaryInfo(response.getBody());
  }

  /**
   * Gets the entries in the glossary with the specified ID for the given source and target
   * languages
   */
  private MultilingualGlossaryInfo updateGlossaryDictionaryInternal(
      String glossaryId,
      String sourceLanguageCode,
      String targetLanguageCode,
      String entries,
      String entriesFormat,
      @Nullable BaseRequestOptions options)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    validateParameter("glossaryId", glossaryId);
    validateParameter("sourceLanguageCode", sourceLanguageCode);
    validateParameter("targetLanguageCode", targetLanguageCode);
    validateParameter("entries", entries);

    List<Map<String, Object>> dictionaries = new ArrayList<>();
    dictionaries.add(
        createGlossaryDictionaryData(
            sourceLanguageCode, targetLanguageCode, entries, entriesFormat));
    Map<String, Object> requestData = new LinkedHashMap<>();
    requestData.put("dictionaries", dictionaries);
    String jsonBody = jsonBodyWithExtraParameters(requestData, options);
    String relativeUrl = multilingualGlossaryPath(glossaryId);
    HttpResponse response =
        httpClientWrapper.sendJsonPatchRequestWithBackoff(relativeUrl, jsonBody);
    checkResponse(response, false);
    return jsonParser.parseMultilingualGlossaryInfo(response.getBody());
  }

  /** Extracts the ID of the given multilingual glossary. */
  private static String multilingualGlossaryId(MultilingualGlossaryInfo glossary) {
    return idOf("glossary", glossary, MultilingualGlossaryInfo::getGlossaryId);
  }

  /** Validates the multilingual glossary ID and builds its URL path with the ID escaped. */
  private String multilingualGlossaryPath(String glossaryId) throws DeepLException {
    validateParameter("glossaryId", glossaryId);
    try {
      return "/v3/glossaries/" + ApiClientHelper.encodeQueryComponent(glossaryId);
    } catch (UnsupportedEncodingException exception) {
      throw new DeepLException("Error while URL-encoding request", exception);
    }
  }

  private static String entriesTsv(GlossaryEntries entries) {
    return Objects.requireNonNull(entries, "entries").toTsv();
  }

  /** Creates the JSON object of one glossary dictionary. */
  private static Map<String, Object> createGlossaryDictionaryData(
      String sourceLanguageCode, String targetLanguageCode, String entries, String entriesFormat) {
    validateParameter("sourceLanguageCode", sourceLanguageCode);
    validateParameter("targetLanguageCode", targetLanguageCode);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("source_lang", sourceLanguageCode);
    data.put("target_lang", targetLanguageCode);
    data.put("entries", entries);
    data.put("entries_format", entriesFormat);
    return data;
  }

  /** Creates query string for the source and target languages */
  private String createLanguageQueryParams(String sourceLanguageCode, String targetLanguageCode)
      throws IllegalArgumentException, DeepLException {
    validateParameter("sourceLanguageCode", sourceLanguageCode);
    validateParameter("targetLanguageCode", targetLanguageCode);
    List<KeyValuePair<String, String>> queryParams = new ArrayList<>();
    queryParams.add(new KeyValuePair<>("source_lang", sourceLanguageCode));
    queryParams.add(new KeyValuePair<>("target_lang", targetLanguageCode));
    return ApiClientHelper.createQueryString(queryParams);
  }

  /**
   * The ID of a required resource object: null is a null error, an object without ID an argument
   * error.
   */
  private static <T> String idOf(String paramName, T resource, Function<T, String> getId) {
    Objects.requireNonNull(resource, paramName);
    String id = getId.apply(resource);
    if (id == null) {
      throw new IllegalArgumentException(paramName + " has no ID");
    }
    return id;
  }

  private static void validateParameter(String paramName, String value)
      throws IllegalArgumentException {
    Objects.requireNonNull(value, paramName);
    if (value.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Parameter %s must not be empty", paramName));
    }
  }

  /** Rejects an empty optional language code; null means unset. */
  private static void validateOptionalLanguage(String paramName, @Nullable String value)
      throws IllegalArgumentException {
    if (value != null && value.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Parameter %s must be null or non-empty", paramName));
    }
  }

  private static Map<String, Object> createWriteRequestData(
      List<String> texts, @Nullable String targetLang, @Nullable TextRephraseOptions options) {
    if (targetLang != null) {
      targetLang = lowerCaseBaseSubtag(targetLang);
      checkValidLanguages(null, targetLang);
    }

    Map<String, Object> requestData = new LinkedHashMap<>();
    putIfNotNull(requestData, "target_lang", targetLang);
    if (options != null) {
      putIfNotNull(requestData, "writing_style", options.getWritingStyle());
      putIfNotNull(requestData, "tone", options.getTone());
    }
    requestData.put("text", validatedTexts(texts));
    return requestData;
  }

  /** Builds the JSON body of a glossary creation, adding the extra body parameters last. */
  private String createGlossaryBody(
      String name,
      List<Map<String, Object>> dictionaries,
      @Nullable CreateMultilingualGlossaryOptions options) {
    Map<String, Object> requestData = new LinkedHashMap<>();
    requestData.put("name", name);
    requestData.put("dictionaries", dictionaries);
    return jsonBodyWithExtraParameters(requestData, options);
  }
}
