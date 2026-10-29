// Copyright 2025 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.

package com.deepl.api;

import com.deepl.api.http.HttpResponse;
import com.deepl.api.http.HttpResponseStream;
import com.deepl.api.utils.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class DeepLClient extends Translator {

  /** Default MIME type of translation memory (TMX) files. */
  public static final String TRANSLATION_MEMORY_FILE_CONTENT_TYPE = "application/xml";

  /** Time to wait between polls of the status of a translation memory job, in milliseconds. */
  private static final long TRANSLATION_MEMORY_JOB_POLL_INTERVAL_MILLIS = 5000;

  /**
   * Initializes a new DeepLClient object using your Authentication Key.
   *
   * <p>Note: This function does not establish a connection to the DeepL API. To check connectivity,
   * use {@link DeepLClient#getUsage()}.
   *
   * @param authKey DeepL Authentication Key as found in your <a
   *     href="https://www.deepl.com/pro-account/">DeepL account</a>.
   * @throws IllegalArgumentException If authKey is invalid.
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
   * @param options Additional options controlling Client behaviour.
   * @throws IllegalArgumentException If authKey is invalid.
   * @deprecated Use the constructor that takes {@link DeepLClientOptions} instead of {@link
   *     TranslatorOptions}
   */
  @Deprecated
  public DeepLClient(String authKey, TranslatorOptions options) throws IllegalArgumentException {
    super(authKey, options);
  }

  /**
   * Initializes a new DeepLClient object using your Authentication Key.
   *
   * <p>Note: This function does not establish a connection to the DeepL API. To check connectivity,
   * use {@link DeepLClient#getUsage()}.
   *
   * @param authKey DeepL Authentication Key as found in your <a
   *     href="https://www.deepl.com/pro-account/">DeepL account</a>.
   * @param options Additional options controlling Client behaviour.
   * @throws IllegalArgumentException If authKey is invalid.
   */
  @SuppressWarnings("deprecation")
  public DeepLClient(String authKey, DeepLClientOptions options) throws IllegalArgumentException {
    super(authKey, options);
  }

  public WriteResult rephraseText(
      String text, @Nullable String targetLang, @Nullable TextRephraseOptions options)
      throws InterruptedException, DeepLException {
    ArrayList<String> texts = new ArrayList<>();
    texts.add(text);
    return this.rephraseText(texts, targetLang, options).get(0);
  }

  public List<WriteResult> rephraseText(
      List<String> texts, @Nullable String targetLang, @Nullable TextRephraseOptions options)
      throws InterruptedException, DeepLException {
    Iterable<KeyValuePair<String, String>> params =
        createWriteHttpParams(texts, targetLang, options);
    HttpResponse response =
        httpClientWrapper.sendRequestWithBackoff(
            String.format("/%s/write/rephrase", apiVersion), params);
    checkResponse(response, false, false);
    return jsonParser.parseWriteResult(response.getBody());
  }

  /**
   * Corrects grammar, spelling and punctuation of the given text without rewriting it.
   *
   * @param text Text to correct, must not be empty.
   * @param targetLang Optional language code the corrected text should be in, e.g. "EN-US".
   * @throws IllegalArgumentException If text is empty.
   */
  public WriteResult correctText(String text, @Nullable String targetLang)
      throws InterruptedException, DeepLException {
    ArrayList<String> texts = new ArrayList<>();
    texts.add(text);
    return this.correctText(texts, targetLang).get(0);
  }

  public List<WriteResult> correctText(List<String> texts, @Nullable String targetLang)
      throws InterruptedException, DeepLException {
    Iterable<KeyValuePair<String, String>> params = createWriteHttpParams(texts, targetLang, null);
    HttpResponse response =
        httpClientWrapper.sendRequestWithBackoff(
            String.format("/%s/write/correct", apiVersion), params);
    checkResponse(response, false, false);
    return jsonParser.parseWriteResult(response.getBody());
  }

  /**
   * Creates a glossary in your DeepL account with the specified details and returns a {@link
   * MultilingualGlossaryInfo} object with details about the newly created glossary. The glossary
   * will contain the glossary dictionaries specified in <paramref name="glossaryDicts" /> each with
   * their own source language, target language and entries.The glossary can be used in translations
   * to override translations for specific terms (words). The glossary must contain a glossary
   * dictionary that matches the languages of translations for which it will be used.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param glossaryDicts {@link MultilingualGlossaryDictionaryInfo} The dictionaries of the
   *     glossary
   * @return {@link MultilingualGlossaryInfo} object with details about the newly created glossary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo createMultilingualGlossary(
      String name, List<MultilingualGlossaryDictionaryEntries> glossaryDicts)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    validateParameter("name", name);
    if (glossaryDicts.isEmpty()) {
      throw new IllegalArgumentException("Parameter dictionaries must not be empty");
    }
    ArrayList<KeyValuePair<String, String>> bodyParams =
        createGlossaryHttpParams(name, glossaryDicts);
    HttpResponse response = httpClientWrapper.sendRequestWithBackoff("/v3/glossaries", bodyParams);
    checkResponse(response, false, false);
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
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile String containing CSV content.
   * @return {@link MultilingualGlossaryInfo} object with details about the newly created glossary.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo createMultilingualGlossaryFromCsv(
      String name, String sourceLanguageCode, String targetLanguageCode, String csvFile)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return createGlossaryFromCsvInternal(name, sourceLanguageCode, targetLanguageCode, csvFile);
  }

  /**
   * Creates a glossary in your DeepL account with the specified details and returns a {@link
   * MultilingualGlossaryInfo} object with details about the newly created glossary. The glossary
   * will contain a glossary dictionary with the source and target language codes specified and
   * entries created from the <paramref name="csvFile" />. The glossary can be used in translations
   * to override translations for specific terms (words). The glossary must contain a glossary
   * dictionary that matches the languages of translations for which it will be used.
   *
   * @param name User-defined name to assign to the glossary; must not be empty.
   * @param sourceLanguageCode Language code of the source terms language.
   * @param targetLanguageCode Language code of the target terms language.
   * @param csvFile String containing CSV content.
   * @return {@link MultilingualGlossaryInfo} object with details about the newly created glossary.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryInfo createMultilingualGlossaryFromCsv(
      String name, String sourceLanguageCode, String targetLanguageCode, File csvFile)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    try (FileInputStream stream = new FileInputStream(csvFile)) {
      String csvContent = StreamUtil.readStream(stream);
      return createGlossaryFromCsvInternal(
          name, sourceLanguageCode, targetLanguageCode, csvContent);
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
    String relativeUrl = String.format("/v3/glossaries/%s", glossaryId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, true);
    return jsonParser.parseMultilingualGlossaryInfo(response.getBody());
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
    checkResponse(response, false, false);
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
    String relativeUrl = String.format("/v3/glossaries/%s/entries%s", glossaryId, queryString);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, true);
    return jsonParser
        .parseMultilingualGlossaryDictionaryListResponse(response.getBody())
        .getDictionaries()
        .get(0)
        .getDictionaryEntries();
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
        glossary.getGlossaryId(), sourceLanguageCode, targetLanguageCode);
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
    return getMultilingualGlossaryDictionaryEntries(
        glossary.getGlossaryId(),
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
        glossaryId, sourceLanguageCode, targetLanguageCode, entries.toTsv(), "tsv");
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
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionary(
      String glossaryId, MultilingualGlossaryDictionaryEntries glossaryDict)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceGlossaryDictionaryInternal(
        glossaryId,
        glossaryDict.getSourceLanguageCode(),
        glossaryDict.getTargetLanguageCode(),
        glossaryDict.getEntries().toTsv(),
        "tsv");
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
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary, MultilingualGlossaryDictionaryEntries glossaryDict)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceGlossaryDictionaryInternal(
        glossary.getGlossaryId(),
        glossaryDict.getSourceLanguageCode(),
        glossaryDict.getTargetLanguageCode(),
        glossaryDict.getEntries().toTsv(),
        "tsv");
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
        glossary.getGlossaryId(), sourceLanguageCode, targetLanguageCode, entries.toTsv(), "tsv");
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
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionaryFromCsv(
      String glossaryId, String sourceLanguageCode, String targetLanguageCode, File csvFile)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    try (FileInputStream stream = new FileInputStream(csvFile)) {
      String csvContent = StreamUtil.readStream(stream);
      return replaceGlossaryDictionaryInternal(
          glossaryId, sourceLanguageCode, targetLanguageCode, csvContent, "csv");
    }
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
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryDictionaryInfo replaceMultilingualGlossaryDictionaryFromCsv(
      String glossaryId, String sourceLanguageCode, String targetLanguageCode, String csvContent)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return replaceGlossaryDictionaryInternal(
        glossaryId, sourceLanguageCode, targetLanguageCode, csvContent, "csv");
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
        glossaryId, sourceLanguageCode, targetLanguageCode, entries.toTsv(), "tsv");
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
        glossary.getGlossaryId(), sourceLanguageCode, targetLanguageCode, entries.toTsv(), "tsv");
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
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionary(
      String glossaryId, MultilingualGlossaryDictionaryEntries glossaryDict)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateGlossaryDictionaryInternal(
        glossaryId,
        glossaryDict.getSourceLanguageCode(),
        glossaryDict.getTargetLanguageCode(),
        glossaryDict.getEntries().toTsv(),
        "tsv");
  }

  /**
   * Updates a glossary dictionary with given entries for the source and target language codes. If a
   * dictionary for the provided language pair already exists, the dictionary entries are merged.
   *
   * @param glossary The specified glossary that contains the dictionary to be updated/created
   * @param glossaryDict The glossary dictionary to be created/updated
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary, MultilingualGlossaryDictionaryEntries glossaryDict)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateGlossaryDictionaryInternal(
        glossary.getGlossaryId(),
        glossaryDict.getSourceLanguageCode(),
        glossaryDict.getTargetLanguageCode(),
        glossaryDict.getEntries().toTsv(),
        "tsv");
  }

  /**
   * Updates a glossary's name with the provided parameter
   *
   * @param glossaryId The specified ID of the glossary whose name will be updated
   * @param name The new name of the glossary
   * @return {@link MultilingualGlossaryInfo} object with details about the glossary with the newly
   *     updated glossary dictionary.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryName(String glossaryId, String name)
      throws DeepLException, IllegalArgumentException {

    ArrayList<KeyValuePair<String, String>> bodyParams = new ArrayList<>();
    bodyParams.add(new KeyValuePair<>("name", name));
    String relativeUrl = String.format("/v3/glossaries/%s", glossaryId);
    try {
      HttpResponse response =
          httpClientWrapper.sendPatchRequestWithBackoff(relativeUrl, bodyParams);
      checkResponse(response, false, true);
      return jsonParser.parseMultilingualGlossaryInfo(response.getBody());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new DeepLException("Request was interrupted", e);
    }
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
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   * @throws IOException If an I/O error occurs.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionaryFromCsv(
      String glossaryId, String sourceLanguageCode, String targetLanguageCode, File csvFile)
      throws DeepLException, IllegalArgumentException, InterruptedException, IOException {
    try (FileInputStream stream = new FileInputStream(csvFile)) {
      String csvContent = StreamUtil.readStream(stream);
      return updateGlossaryDictionaryInternal(
          glossaryId, sourceLanguageCode, targetLanguageCode, csvContent, "csv");
    }
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
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws IllegalArgumentException If any argument is invalid.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public MultilingualGlossaryInfo updateMultilingualGlossaryDictionaryFromCsv(
      String glossaryId, String sourceLanguageCode, String targetLanguageCode, String csvContent)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    return updateGlossaryDictionaryInternal(
        glossaryId, sourceLanguageCode, targetLanguageCode, csvContent, "csv");
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
    String relativeUrl = String.format("/v3/glossaries/%s", glossaryId);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    this.checkResponse(response, false, true);
  }

  /**
   * Deletes the specified glossary.
   *
   * @param glossary {@link MultilingualGlossaryInfo} object corresponding to glossary to delete.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public void deleteMultilingualGlossary(MultilingualGlossaryInfo glossary)
      throws DeepLException, InterruptedException {
    this.deleteMultilingualGlossary(glossary.getGlossaryId());
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
    String relativeUrl = String.format("/v3/glossaries/%s/dictionaries%s", glossaryId, queryString);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    this.checkResponse(response, false, true);
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
    deleteMultilingualGlossaryDictionary(
        glossaryId, glossaryDict.getSourceLanguageCode(), glossaryDict.getTargetLanguageCode());
  }

  /**
   * Deletes the specified glossary dictionary in the glossary in the specified glossary.
   *
   * @param glossary The glossary that contains the glossary dictionary to delete.
   * @param sourceLanguageCode Source language code of the glossary dictionary to be deleted.
   * @param targetLanguageCode Target language code of the glossary dictionary to be deleted.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public void deleteMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary, String sourceLanguageCode, String targetLanguageCode)
      throws DeepLException, InterruptedException, IllegalArgumentException {
    deleteMultilingualGlossaryDictionary(
        glossary.getGlossaryId(), sourceLanguageCode, targetLanguageCode);
  }

  /**
   * Deletes the specified glossary dictionary in the glossary in the specified glossary.
   *
   * @param glossary The glossary that contains the glossary dictionary to delete.
   * @param glossaryDict {@link MultilingualGlossaryDictionaryInfo} object corresponding to glossary
   *     dictionary to delete.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public void deleteMultilingualGlossaryDictionary(
      MultilingualGlossaryInfo glossary, MultilingualGlossaryDictionaryInfo glossaryDict)
      throws DeepLException, InterruptedException, IllegalArgumentException {
    deleteMultilingualGlossaryDictionary(
        glossary.getGlossaryId(),
        glossaryDict.getSourceLanguageCode(),
        glossaryDict.getTargetLanguageCode());
  }

  /**
   * Creates a Spoken Terms collection with the given term lists and returns a {@link SpokenTerms}
   * object with details about the new collection.
   *
   * @param name User-defined name of the collection.
   * @param termLists Term lists of the collection, one {@link SpokenTermsEntries} per language.
   * @return {@link SpokenTerms} object with details about the new collection.
   * @throws IllegalArgumentException If a term is invalid, as checked for glossary terms.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API, a {@link
   *     DeepLException} or a derived class will be thrown.
   */
  public SpokenTerms createSpokenTerms(String name, List<SpokenTermsEntries> termLists)
      throws DeepLException, InterruptedException {
    Map<String, Object> requestData = new LinkedHashMap<>();
    requestData.put("name", name);
    requestData.put("term_lists", createSpokenTermsListsData(termLists));
    String jsonBody = jsonParser.getGson().toJson(requestData);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff("/v3/spoken-terms", jsonBody);
    checkResponse(response, false, false);
    return jsonParser.parseSpokenTerms(response.getBody());
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
    checkResponse(response, false, false);
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
    validateParameter("spokenTermsId", spokenTermsId);
    String relativeUrl = String.format("/v3/spoken-terms/%s", spokenTermsId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
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
   * @param name New name of the collection, or <code>null</code> to keep the current name.
   * @param termLists Term lists to merge into the collection, or <code>null</code> to keep the
   *     current term lists.
   * @return {@link SpokenTerms} object with details about the updated collection.
   * @throws NotFoundException If no collection with the given ID is found.
   * @throws IllegalArgumentException If a term is invalid, as checked for glossary terms.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public SpokenTerms updateSpokenTerms(
      String spokenTermsId, @Nullable String name, @Nullable List<SpokenTermsEntries> termLists)
      throws DeepLException, InterruptedException {
    validateParameter("spokenTermsId", spokenTermsId);
    Map<String, Object> requestData = new LinkedHashMap<>();
    if (name != null) {
      requestData.put("name", name);
    }
    if (termLists != null) {
      requestData.put("term_lists", createSpokenTermsListsData(termLists));
    }
    String jsonBody = jsonParser.getGson().toJson(requestData);
    String relativeUrl = String.format("/v3/spoken-terms/%s", spokenTermsId);
    HttpResponse response =
        httpClientWrapper.sendJsonPatchRequestWithBackoff(relativeUrl, jsonBody);
    checkResponse(response, false, false);
    return jsonParser.parseSpokenTerms(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#updateSpokenTerms(String, String, List)} but accepts a
   * {@link SpokenTerms} object.
   *
   * @see DeepLClient#updateSpokenTerms(String, String, List)
   */
  public SpokenTerms updateSpokenTerms(
      SpokenTerms spokenTerms, @Nullable String name, @Nullable List<SpokenTermsEntries> termLists)
      throws DeepLException, InterruptedException {
    return updateSpokenTerms(spokenTermsId(spokenTerms), name, termLists);
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
    validateParameter("spokenTermsId", spokenTermsId);
    String relativeUrl =
        String.format("/v3/spoken-terms/%s/entries%s", spokenTermsId, langQueryString(lang));
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
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
   * @return {@link SpokenTermsList} object with details about the new term list.
   * @throws NotFoundException If no collection with the given ID is found.
   * @throws IllegalArgumentException If a term is invalid, as checked for glossary terms.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public SpokenTermsList replaceSpokenTermsList(String spokenTermsId, SpokenTermsEntries entries)
      throws DeepLException, InterruptedException {
    validateParameter("spokenTermsId", spokenTermsId);
    String jsonBody = jsonParser.getGson().toJson(createSpokenTermsListData(entries));
    String relativeUrl = String.format("/v3/spoken-terms/%s/term-lists", spokenTermsId);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff("PUT", relativeUrl, jsonBody);
    checkResponse(response, false, false);
    return jsonParser.parseSpokenTermsTermList(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#replaceSpokenTermsList(String, SpokenTermsEntries)}
   * but accepts a {@link SpokenTerms} object.
   *
   * @see DeepLClient#replaceSpokenTermsList(String, SpokenTermsEntries)
   */
  public SpokenTermsList replaceSpokenTermsList(SpokenTerms spokenTerms, SpokenTermsEntries entries)
      throws DeepLException, InterruptedException {
    return replaceSpokenTermsList(spokenTermsId(spokenTerms), entries);
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
    validateParameter("spokenTermsId", spokenTermsId);
    String relativeUrl = String.format("/v3/spoken-terms/%s", spokenTermsId);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
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
    validateParameter("spokenTermsId", spokenTermsId);
    String relativeUrl =
        String.format("/v3/spoken-terms/%s/term-lists%s", spokenTermsId, langQueryString(lang));
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
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
    checkResponse(response, false, false);
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
    checkResponse(response, false, false);
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
   * @param name Name of the style profile.
   * @param options Optional description, industry and linked components, or <code>null</code>.
   *     Fields left <code>null</code> are not sent.
   * @return {@link StyleProfile} object with details about the new style profile.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleProfile createStyleProfile(String name, @Nullable StyleProfileOptions options)
      throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff(
            "/v3/style_profiles", createStyleProfileBody(name, options));
    checkResponse(response, false, false);
    return jsonParser.parseStyleProfile(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#createStyleProfile(String, StyleProfileOptions)} but
   * without options.
   *
   * @see DeepLClient#createStyleProfile(String, StyleProfileOptions)
   */
  public StyleProfile createStyleProfile(String name) throws DeepLException, InterruptedException {
    return createStyleProfile(name, null);
  }

  /**
   * Retrieves the style profile with the given ID.
   *
   * @param styleProfileId ID of the style profile to retrieve.
   * @return {@link StyleProfile} object with details about the style profile.
   * @throws IllegalArgumentException If the ID is <code>null</code> or empty.
   * @throws NotFoundException If no style profile with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleProfile getStyleProfile(String styleProfileId)
      throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendGetRequestWithBackoff(styleProfilePath(styleProfileId));
    checkResponse(response, false, false);
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
   * @param name New name of the style profile, or <code>null</code> to keep the current name.
   * @param options Fields to change, or <code>null</code>.
   * @return {@link StyleProfile} object with details about the updated style profile.
   * @throws IllegalArgumentException If the ID is <code>null</code> or empty.
   * @throws NotFoundException If no style profile with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleProfile updateStyleProfile(
      String styleProfileId, @Nullable String name, @Nullable StyleProfileOptions options)
      throws DeepLException, InterruptedException {
    String relativeUrl = styleProfilePath(styleProfileId);
    HttpResponse response =
        httpClientWrapper.sendJsonPatchRequestWithBackoff(
            relativeUrl, createStyleProfileBody(name, options));
    checkResponse(response, false, false);
    return jsonParser.parseStyleProfile(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#updateStyleProfile(String, String,
   * StyleProfileOptions)} but accepts a {@link StyleProfile} object.
   *
   * @see DeepLClient#updateStyleProfile(String, String, StyleProfileOptions)
   */
  public StyleProfile updateStyleProfile(
      StyleProfile styleProfile, @Nullable String name, @Nullable StyleProfileOptions options)
      throws DeepLException, InterruptedException {
    return updateStyleProfile(styleProfileId(styleProfile), name, options);
  }

  /**
   * Deletes the style profile with the given ID.
   *
   * @param styleProfileId ID of the style profile to delete.
   * @throws IllegalArgumentException If the ID is <code>null</code> or empty.
   * @throws NotFoundException If no style profile with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public void deleteStyleProfile(String styleProfileId)
      throws DeepLException, InterruptedException {
    HttpResponse response =
        httpClientWrapper.sendDeleteRequestWithBackoff(styleProfilePath(styleProfileId));
    checkResponse(response, false, false);
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
   * @param resource Resource to retrieve languages for, for example {@link
   *     LanguageResource#RESOURCE_TRANSLATE_TEXT} or {@link LanguageResource#RESOURCE_GLOSSARY}.
   * @param include Optional list of include values, for example {@link
   *     LanguageSupport#INCLUDE_BETA} or {@link LanguageSupport#INCLUDE_EXTERNAL}. By default, or
   *     if <code>null</code>, only stable languages and features are returned.
   * @return List of {@link LanguageSupport} objects with per-feature support information.
   * @throws IllegalArgumentException If resource is <code>null</code> or empty.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public List<LanguageSupport> getLanguagesForResource(
      String resource, @Nullable List<String> include) throws DeepLException, InterruptedException {
    validateParameter("resource", resource);
    ArrayList<KeyValuePair<String, String>> queryParams = new ArrayList<>();
    queryParams.add(new KeyValuePair<>("resource", resource));
    if (include != null) {
      for (String value : include) {
        queryParams.add(new KeyValuePair<>("include", value));
      }
    }
    String relativeUrl = "/v3/languages" + ApiClientHelper.createQueryString(queryParams);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
    return jsonParser.parseLanguageSupportList(response.getBody());
  }

  /**
   * Functions the same as {@link DeepLClient#getLanguagesForResource(String, List)} but returns
   * only stable languages and features.
   *
   * @see DeepLClient#getLanguagesForResource(String, List)
   */
  public List<LanguageSupport> getLanguagesForResource(String resource)
      throws DeepLException, InterruptedException {
    return getLanguagesForResource(resource, null);
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
    checkResponse(response, false, false);
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
    checkResponse(response, false, false);
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
    String relativeUrl = String.format("/v3/translation_memories/%s", translationMemoryId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
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
        String.format("/v3/translation_memories/%s/segments", translationMemoryId)
            + ApiClientHelper.createQueryString(queryParams);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
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
    String relativeUrl = String.format("/v3/translation_memories/%s", translationMemoryId);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
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
   * DeepLClient#importTranslationMemoryFromFilepath(File, String)} to do all three steps at once.
   *
   * @param fileName Name of the TMX file to import, for example "legal.tmx".
   * @param contentLength Size of the TMX file in bytes.
   * @param contentType Optional MIME type of the file, defaults to "application/xml".
   * @param displayName Optional name for the resulting translation memory, defaults to the file
   *     name.
   * @return {@link TranslationMemoryImport} object with the job ID and upload URL.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public TranslationMemoryImport createTranslationMemoryImport(
      String fileName,
      long contentLength,
      @Nullable String contentType,
      @Nullable String displayName)
      throws DeepLException, InterruptedException {
    validateParameter("fileName", fileName);
    if (contentLength <= 0) {
      throw new IllegalArgumentException("contentLength must be greater than 0");
    }

    Map<String, Object> sourceFile = new HashMap<>();
    sourceFile.put("file_name", fileName);
    sourceFile.put("content_length", contentLength);
    if (contentType != null) {
      sourceFile.put("content_type", contentType);
    }
    Map<String, Object> requestData = new HashMap<>();
    requestData.put("source_file", sourceFile);
    if (displayName != null) {
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("display_name", displayName);
      requestData.put("parameters", parameters);
    }

    String jsonBody = jsonParser.getGson().toJson(requestData);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff("/v3/translation_memories/import", jsonBody);
    checkResponse(response, false, false);
    return jsonParser.parseTranslationMemoryImport(response.getBody());
  }

  /**
   * Uploads a TMX file to the upload URL of an import job, which starts processing.
   *
   * <p>The upload URL is a pre-signed storage URL outside of the DeepL API, so the authentication
   * key is not sent with this request.
   *
   * @param uploadUrl Upload URL returned by {@link
   *     DeepLClient#createTranslationMemoryImport(String, long, String, String)}.
   * @param fileContent Content of the TMX file to upload.
   * @param contentType MIME type of the file, which must match the content type declared when the
   *     import job was created.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while uploading the file.
   */
  public void uploadTranslationMemoryFile(String uploadUrl, byte[] fileContent, String contentType)
      throws DeepLException, InterruptedException {
    validateParameter("uploadUrl", uploadUrl);
    validateParameter("contentType", contentType);
    if (fileContent == null) {
      throw new IllegalArgumentException("fileContent must not be null");
    }
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
   * but accepts the {@link TranslationMemoryImport} object holding the upload URL.
   *
   * @see DeepLClient#uploadTranslationMemoryFile(String, byte[], String)
   */
  public void uploadTranslationMemoryFile(
      TranslationMemoryImport translationMemoryImport, byte[] fileContent, String contentType)
      throws DeepLException, InterruptedException {
    if (translationMemoryImport == null) {
      throw new IllegalArgumentException("translationMemoryImport must not be null");
    }
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
    String relativeUrl = String.format("/v3/translation_memories/%s/export", translationMemoryId);
    HttpResponse response = httpClientWrapper.sendRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
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
    String relativeUrl = String.format("/v3/translation_memories/jobs/%s", jobId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
    return jsonParser.parseTranslationMemoryJob(response.getBody());
  }

  /**
   * Polls the translation memory job with the specified ID until it finishes, sleeping between
   * requests, and returns the final status.
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
   *     Note that this is not accurate to the millisecond, as the job status is only polled every 5
   *     seconds.
   * @return {@link TranslationMemoryJob} object with the status of the finished job.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the timeout is exceeded, the job failed or expired, or any error
   *     occurs while communicating with the DeepL API.
   * @see DeepLClient#waitUntilTranslationMemoryJobDone(String)
   */
  public TranslationMemoryJob waitUntilTranslationMemoryJobDone(
      String jobId, @Nullable Duration timeout) throws DeepLException, InterruptedException {
    long startTimeMillis = System.currentTimeMillis();
    TranslationMemoryJob job = getTranslationMemoryJob(jobId);
    while (!job.done()) {
      // The API always returns exactly one result; an empty list would never reach a terminal
      // status and the no-timeout overload would poll forever.
      if (job.getResults().isEmpty()) {
        throw new DeepLException("Translation memory job " + jobId + " returned no results");
      }
      if (timeout != null && System.currentTimeMillis() - startTimeMillis > timeout.toMillis()) {
        throw new DeepLException(
            String.format(
                "Manual timeout of %ds exceeded for translation memory job", timeout.getSeconds()));
      }
      Thread.sleep(TRANSLATION_MEMORY_JOB_POLL_INTERVAL_MILLIS);
      job = getTranslationMemoryJob(jobId);
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
   * Downloads the TMX file of a completed export job to the specified output file.
   *
   * <p>The download URL is a pre-signed storage URL outside of the DeepL API, so the authentication
   * key is not sent with this request.
   *
   * @param job Completed export job carrying the download URL.
   * @param outputFile File to download the exported translation memory to.
   * @throws IOException If the output path is occupied.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the job has no download URL, or any error occurs while downloading.
   */
  public void downloadTranslationMemoryExport(TranslationMemoryJob job, File outputFile)
      throws DeepLException, IOException, InterruptedException {
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
   * @throws IOException If an I/O error occurs.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the job has no download URL, or any error occurs while downloading.
   * @see DeepLClient#downloadTranslationMemoryExport(TranslationMemoryJob, File)
   */
  public void downloadTranslationMemoryExport(TranslationMemoryJob job, OutputStream outputStream)
      throws DeepLException, IOException, InterruptedException {
    if (job == null) {
      throw new IllegalArgumentException("job must not be null");
    }
    TranslationMemoryJobResult result = job.getResult();
    String downloadUrl = (result == null) ? null : result.getDownloadUrl();
    if (downloadUrl == null || downloadUrl.isEmpty()) {
      throw new DeepLException(
          "Translation memory export job has no download URL, it may not have completed yet");
    }

    try (HttpResponseStream response = httpClientWrapper.downloadAssetWithBackoff(downloadUrl)) {
      if (response.getCode() < 200 || response.getCode() >= 300) {
        throw new DeepLException(
                String.format(
                    "Error downloading translation memory export, HTTP status: %d",
                    response.getCode()))
            .setHttpStatusCode(response.getCode());
      }
      assert response.getBody() != null;
      StreamUtil.transferTo(response.getBody(), outputStream);
    }
  }

  /**
   * Imports a TMX file as a new translation memory: creates the import job, uploads the file, and
   * waits for processing to finish.
   *
   * @param inputFile TMX file to import.
   * @param displayName Optional name for the resulting translation memory, defaults to the file
   *     name.
   * @return {@link TranslationMemoryJob} object for the completed import; its result carries the ID
   *     of the new translation memory.
   * @throws IOException If the input file cannot be read.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the import fails, or any error occurs while communicating with the
   *     DeepL API.
   */
  public TranslationMemoryJob importTranslationMemoryFromFilepath(
      File inputFile, @Nullable String displayName)
      throws DeepLException, IOException, InterruptedException {
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
   *     indefinitely. Note that this is not accurate to the millisecond, as the job status is only
   *     polled every 5 seconds.
   * @return {@link TranslationMemoryJob} object for the completed import; its result carries the ID
   *     of the new translation memory.
   * @throws IOException If the input file cannot be read.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the timeout is exceeded, the import fails, or any error occurs while
   *     communicating with the DeepL API.
   * @see DeepLClient#importTranslationMemoryFromFilepath(File, String)
   */
  public TranslationMemoryJob importTranslationMemoryFromFilepath(
      File inputFile, @Nullable String displayName, @Nullable Duration timeout)
      throws DeepLException, IOException, InterruptedException {
    if (inputFile == null || !inputFile.exists()) {
      throw new IllegalArgumentException("inputFile must be an existing file");
    }
    byte[] fileContent = Files.readAllBytes(inputFile.toPath());
    TranslationMemoryImport translationMemoryImport =
        createTranslationMemoryImport(inputFile.getName(), fileContent.length, null, displayName);
    uploadTranslationMemoryFile(translationMemoryImport, fileContent);
    return waitUntilTranslationMemoryJobDone(translationMemoryImport.getJobId(), timeout);
  }

  /**
   * Functions the same as {@link DeepLClient#importTranslationMemoryFromFilepath(File, String)} but
   * lets the API name the translation memory after the file.
   *
   * @see DeepLClient#importTranslationMemoryFromFilepath(File, String)
   */
  public TranslationMemoryJob importTranslationMemoryFromFilepath(File inputFile)
      throws DeepLException, IOException, InterruptedException {
    return importTranslationMemoryFromFilepath(inputFile, null);
  }

  /**
   * Exports a translation memory to a TMX file: creates the export job, waits for it to finish, and
   * writes the result to the specified output file.
   *
   * @param translationMemoryId ID of the translation memory to export.
   * @param outputFile File to write the exported translation memory to.
   * @return {@link TranslationMemoryJob} object for the completed export.
   * @throws IOException If the output path is occupied.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the export fails, or any error occurs while communicating with the
   *     DeepL API.
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      String translationMemoryId, File outputFile)
      throws DeepLException, IOException, InterruptedException {
    return exportTranslationMemoryToFilepath(translationMemoryId, outputFile, null);
  }

  /**
   * Functions the same as {@link DeepLClient#exportTranslationMemoryToFilepath(String, File)} but
   * gives up waiting for the export after the specified timeout.
   *
   * @param translationMemoryId ID of the translation memory to export.
   * @param outputFile File to write the exported translation memory to.
   * @param timeout Maximum time to wait for the export to finish, or <code>null</code> to wait
   *     indefinitely. Note that this is not accurate to the millisecond, as the job status is only
   *     polled every 5 seconds.
   * @return {@link TranslationMemoryJob} object for the completed export.
   * @throws IOException If the output path is occupied.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If the timeout is exceeded, the export fails, or any error occurs while
   *     communicating with the DeepL API.
   * @see DeepLClient#exportTranslationMemoryToFilepath(String, File)
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      String translationMemoryId, File outputFile, @Nullable Duration timeout)
      throws DeepLException, IOException, InterruptedException {
    TranslationMemoryExport translationMemoryExport =
        createTranslationMemoryExport(translationMemoryId);
    TranslationMemoryJob job =
        waitUntilTranslationMemoryJobDone(translationMemoryExport.getJobId(), timeout);
    downloadTranslationMemoryExport(job, outputFile);
    return job;
  }

  /**
   * Functions the same as {@link DeepLClient#exportTranslationMemoryToFilepath(String, File)} but
   * accepts a {@link TranslationMemoryInfo} object.
   *
   * @see DeepLClient#exportTranslationMemoryToFilepath(String, File)
   */
  public TranslationMemoryJob exportTranslationMemoryToFilepath(
      TranslationMemoryInfo translationMemory, File outputFile)
      throws DeepLException, IOException, InterruptedException {
    return exportTranslationMemoryToFilepath(translationMemoryId(translationMemory), outputFile);
  }

  /** Extracts the ID of the given translation memory. */
  private static String translationMemoryId(TranslationMemoryInfo translationMemory)
      throws IllegalArgumentException {
    if (translationMemory == null) {
      throw new IllegalArgumentException("translationMemory must not be null");
    }
    return translationMemory.getTranslationMemoryId();
  }

  /** Extracts the ID of the given Spoken Terms collection. */
  private static String spokenTermsId(SpokenTerms spokenTerms) throws IllegalArgumentException {
    if (spokenTerms == null) {
      throw new IllegalArgumentException("spokenTerms must not be null");
    }
    return spokenTerms.getSpokenTermsId();
  }

  /** Extracts the ID of the given style profile. */
  private static String styleProfileId(StyleProfile styleProfile) throws IllegalArgumentException {
    if (styleProfile == null) {
      throw new IllegalArgumentException("styleProfile must not be null");
    }
    return styleProfile.getStyleProfileId();
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

  /** Builds the JSON body of a style profile request; Gson omits the options left null. */
  private String createStyleProfileBody(
      @Nullable String name, @Nullable StyleProfileOptions options) {
    JsonObject body = new JsonObject();
    if (name != null) {
      body.addProperty("name", name);
    }
    if (options != null) {
      for (Map.Entry<String, JsonElement> entry :
          jsonParser.getGson().toJsonTree(options).getAsJsonObject().entrySet()) {
        body.add(entry.getKey(), entry.getValue());
      }
    }
    return jsonParser.getGson().toJson(body);
  }

  /** Builds the query string that selects the term list of the given language. */
  private static String langQueryString(String lang) {
    List<KeyValuePair<String, String>> queryParams = new ArrayList<>();
    queryParams.add(new KeyValuePair<>("lang", lang));
    return ApiClientHelper.createQueryString(queryParams);
  }

  /**
   * Builds the JSON object of one term list; the API expects the terms newline-separated, so each
   * term is checked like a glossary term first: a term containing a newline would split.
   */
  private static Map<String, Object> createSpokenTermsListData(SpokenTermsEntries entries) {
    for (String term : entries.getEntries()) {
      GlossaryEntries.validateGlossaryTerm(term);
    }
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("lang", entries.getLang());
    data.put("entries", String.join("\n", entries.getEntries()));
    return data;
  }

  private static List<Map<String, Object>> createSpokenTermsListsData(
      List<SpokenTermsEntries> termLists) {
    List<Map<String, Object>> data = new ArrayList<>();
    for (SpokenTermsEntries entries : termLists) {
      data.add(createSpokenTermsListData(entries));
    }
    return data;
  }

  /**
   * Creates a new style rule with the specified details and returns a {@link StyleRuleInfo} object
   * with details about the newly created style rule.
   *
   * @param name User-defined name for the style rule.
   * @param language Language code for the style rule (e.g. "en", "de").
   * @param configuredRules Optional configured rules for the style rule.
   * @param customInstructions Optional list of custom instructions for the style rule.
   * @return {@link StyleRuleInfo} object with details about the newly created style rule.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo createStyleRule(
      String name,
      String language,
      @Nullable ConfiguredRules configuredRules,
      @Nullable List<CustomInstruction> customInstructions)
      throws DeepLException, InterruptedException {
    validateParameter("name", name);
    validateParameter("language", language);
    Map<String, Object> requestData = new HashMap<>();
    requestData.put("name", name);
    requestData.put("language", language);
    if (configuredRules != null) {
      requestData.put("configured_rules", configuredRules);
    }
    if (customInstructions != null) {
      requestData.put("custom_instructions", customInstructions);
    }
    String jsonBody = jsonParser.getGson().toJson(requestData);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff("/v3/style_rules", jsonBody);
    checkResponse(response, false, false);
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
    validateParameter("styleId", styleId);
    String relativeUrl = String.format("/v3/style_rules/%s", styleId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
    return jsonParser.parseStyleRuleInfo(response.getBody());
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
    validateParameter("styleId", styleId);
    validateParameter("name", name);
    String relativeUrl = String.format("/v3/style_rules/%s", styleId);
    Map<String, Object> requestData = new HashMap<>();
    requestData.put("name", name);
    String jsonBody = jsonParser.getGson().toJson(requestData);
    HttpResponse response =
        httpClientWrapper.sendJsonPatchRequestWithBackoff(relativeUrl, jsonBody);
    checkResponse(response, false, false);
    return jsonParser.parseStyleRuleInfo(response.getBody());
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
    validateParameter("styleId", styleId);
    String relativeUrl = String.format("/v3/style_rules/%s", styleId);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
  }

  /**
   * Replaces the configured rules of the style rule with the specified ID and returns the updated
   * {@link StyleRuleInfo} object.
   *
   * @param styleId ID of the style rule to update.
   * @param configuredRules The new configured rules to set for the style rule.
   * @return {@link StyleRuleInfo} object with updated details about the style rule.
   * @throws NotFoundException If no style rule with the given ID is found.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public StyleRuleInfo updateStyleRuleConfiguredRules(
      String styleId, ConfiguredRules configuredRules)
      throws DeepLException, InterruptedException, NotFoundException {
    validateParameter("styleId", styleId);
    if (configuredRules == null) {
      throw new IllegalArgumentException("configuredRules must not be null");
    }
    String relativeUrl = String.format("/v3/style_rules/%s/configured_rules", styleId);
    String jsonBody = jsonParser.getGson().toJson(configuredRules);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff("PUT", relativeUrl, jsonBody);
    checkResponse(response, false, false);
    return jsonParser.parseStyleRuleInfo(response.getBody());
  }

  /**
   * Creates a new custom instruction for the style rule with the specified ID and returns the
   * created {@link CustomInstruction} object.
   *
   * @param styleId ID of the style rule to add the custom instruction to.
   * @param label Label for the custom instruction.
   * @param prompt Prompt text for the custom instruction.
   * @param sourceLanguage Optional source language code for the custom instruction.
   * @return {@link CustomInstruction} object with details about the newly created custom
   *     instruction.
   * @throws InterruptedException If the thread is interrupted during execution of this function.
   * @throws DeepLException If any error occurs while communicating with the DeepL API.
   */
  public CustomInstruction createStyleRuleCustomInstruction(
      String styleId, String label, String prompt, @Nullable String sourceLanguage)
      throws DeepLException, InterruptedException {
    validateParameter("styleId", styleId);
    validateParameter("label", label);
    validateParameter("prompt", prompt);
    String relativeUrl = String.format("/v3/style_rules/%s/custom_instructions", styleId);
    Map<String, Object> requestData = new HashMap<>();
    requestData.put("label", label);
    requestData.put("prompt", prompt);
    if (sourceLanguage != null) {
      requestData.put("source_language", sourceLanguage);
    }
    String jsonBody = jsonParser.getGson().toJson(requestData);
    HttpResponse response = httpClientWrapper.sendJsonRequestWithBackoff(relativeUrl, jsonBody);
    checkResponse(response, false, false);
    return jsonParser.parseCustomInstruction(response.getBody());
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
    validateParameter("styleId", styleId);
    validateParameter("instructionId", instructionId);
    String relativeUrl =
        String.format("/v3/style_rules/%s/custom_instructions/%s", styleId, instructionId);
    HttpResponse response = httpClientWrapper.sendGetRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
    return jsonParser.parseCustomInstruction(response.getBody());
  }

  /**
   * Updates the custom instruction with the specified ID within the style rule with the specified
   * ID and returns the updated {@link CustomInstruction} object.
   *
   * @param styleId ID of the style rule containing the custom instruction.
   * @param instructionId ID of the custom instruction to update.
   * @param label New label for the custom instruction.
   * @param prompt New prompt text for the custom instruction.
   * @param sourceLanguage Optional new source language code for the custom instruction.
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
    validateParameter("styleId", styleId);
    validateParameter("instructionId", instructionId);
    validateParameter("label", label);
    validateParameter("prompt", prompt);
    String relativeUrl =
        String.format("/v3/style_rules/%s/custom_instructions/%s", styleId, instructionId);
    Map<String, Object> requestData = new HashMap<>();
    requestData.put("label", label);
    requestData.put("prompt", prompt);
    if (sourceLanguage != null) {
      requestData.put("source_language", sourceLanguage);
    }
    String jsonBody = jsonParser.getGson().toJson(requestData);
    HttpResponse response =
        httpClientWrapper.sendJsonRequestWithBackoff("PUT", relativeUrl, jsonBody);
    checkResponse(response, false, false);
    return jsonParser.parseCustomInstruction(response.getBody());
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
    validateParameter("styleId", styleId);
    validateParameter("instructionId", instructionId);
    String relativeUrl =
        String.format("/v3/style_rules/%s/custom_instructions/%s", styleId, instructionId);
    HttpResponse response = httpClientWrapper.sendDeleteRequestWithBackoff(relativeUrl);
    checkResponse(response, false, false);
  }

  /** Creates a glossary with given details. */
  private MultilingualGlossaryInfo createGlossaryFromCsvInternal(
      String name, String sourceLanguageCode, String targetLanguageCode, String entries)
      throws DeepLException, InterruptedException {
    ArrayList<KeyValuePair<String, String>> params =
        createGlossaryDictionariesHttpParams(
            sourceLanguageCode, targetLanguageCode, entries, "csv");
    params.add(new KeyValuePair<>("name", name));
    HttpResponse response = httpClientWrapper.sendRequestWithBackoff("/v3/glossaries", params);
    checkResponse(response, false, false);
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
      String entriesFormat)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    validateParameter("glossaryId", glossaryId);
    validateParameter("sourceLanguageCode", sourceLanguageCode);
    validateParameter("targetLanguageCode", targetLanguageCode);
    validateParameter("entries", entries);

    ArrayList<KeyValuePair<String, String>> bodyParams = new ArrayList<>();
    bodyParams.add(new KeyValuePair<>("source_lang", sourceLanguageCode));
    bodyParams.add(new KeyValuePair<>("target_lang", targetLanguageCode));
    bodyParams.add(new KeyValuePair<>("entries", entries));
    bodyParams.add(new KeyValuePair<>("entries_format", entriesFormat));

    String relativeUrl = String.format("/v3/glossaries/%s/dictionaries", glossaryId);
    HttpResponse response = httpClientWrapper.sendPutRequestWithBackoff(relativeUrl, bodyParams);
    checkResponse(response, false, true);
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
      String entriesFormat)
      throws DeepLException, IllegalArgumentException, InterruptedException {
    validateParameter("glossaryId", glossaryId);
    validateParameter("sourceLanguageCode", sourceLanguageCode);
    validateParameter("targetLanguageCode", targetLanguageCode);
    validateParameter("entries", entries);

    ArrayList<KeyValuePair<String, String>> bodyParams =
        createGlossaryDictionariesHttpParams(
            sourceLanguageCode, targetLanguageCode, entries, entriesFormat);
    String relativeUrl = String.format("/v3/glossaries/%s", glossaryId);
    HttpResponse response = httpClientWrapper.sendPatchRequestWithBackoff(relativeUrl, bodyParams);
    checkResponse(response, false, true);
    return jsonParser.parseMultilingualGlossaryInfo(response.getBody());
  }

  /** Creates query string for the source and target languages */
  private String createLanguageQueryParams(String sourceLanguageCode, String targetLanguageCode)
      throws IllegalArgumentException, DeepLException {
    validateParameter("sourceLanguageCode", sourceLanguageCode);
    validateParameter("targetLanguageCode", targetLanguageCode);
    try {
      return "?"
          + String.join(
              "&",
              String.format(
                  "source_lang=%s",
                  URLEncoder.encode(sourceLanguageCode, StandardCharsets.UTF_8.name())),
              String.format(
                  "target_lang=%s",
                  URLEncoder.encode(targetLanguageCode, StandardCharsets.UTF_8.name())));
    } catch (UnsupportedEncodingException exception) {
      throw new DeepLException("Error while URL-encoding request", exception);
    }
  }

  private void validateParameter(String paramName, String value) throws IllegalArgumentException {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Parameter %s must not be empty", paramName));
    }
  }

  protected static ArrayList<KeyValuePair<String, String>> createWriteHttpParams(
      List<String> texts, @Nullable String targetLang, @Nullable TextRephraseOptions options) {
    if (targetLang != null) {
      targetLang = LanguageCode.standardize(targetLang);
      checkValidLanguages(null, targetLang);
    }

    ArrayList<KeyValuePair<String, String>> params = new ArrayList<>();
    if (targetLang != null) {
      params.add(new KeyValuePair<>("target_lang", targetLang));
    }
    if (options != null && options.getWritingStyle() != null) {
      params.add(new KeyValuePair<>("writing_style", options.getWritingStyle()));
    }
    if (options != null && options.getTone() != null) {
      params.add(new KeyValuePair<>("tone", options.getTone()));
    }

    texts.forEach(
        (text) -> {
          if (text.isEmpty()) throw new IllegalArgumentException("text must not be empty");
          params.add(new KeyValuePair<>("text", text));
        });

    return params;
  }

  protected static ArrayList<KeyValuePair<String, String>> createGlossaryHttpParams(
      String name, List<MultilingualGlossaryDictionaryEntries> glossaryDicts) {
    ArrayList<KeyValuePair<String, String>> bodyParams = new ArrayList<>();
    bodyParams.add(new KeyValuePair<>("name", name));
    for (int i = 0; i < glossaryDicts.size(); i++) {
      bodyParams.add(
          new KeyValuePair<>(
              String.format("dictionaries[%d].source_lang", i),
              glossaryDicts.get(i).getSourceLanguageCode()));
      bodyParams.add(
          new KeyValuePair<>(
              String.format("dictionaries[%d].target_lang", i),
              glossaryDicts.get(i).getTargetLanguageCode()));
      bodyParams.add(
          new KeyValuePair<>(
              String.format("dictionaries[%d].entries", i),
              glossaryDicts.get(i).getEntries().toTsv()));
      bodyParams.add(
          new KeyValuePair<>(String.format("dictionaries[%d].entries_format", i), "tsv"));
    }
    return bodyParams;
  }

  protected static ArrayList<KeyValuePair<String, String>> createGlossaryDictionariesHttpParams(
      String sourceLanguageCode, String targetLanguageCode, String entries, String entriesFormat) {
    ArrayList<KeyValuePair<String, String>> bodyParams = new ArrayList<>();
    bodyParams.add(new KeyValuePair<>("dictionaries[0].source_lang", sourceLanguageCode));
    bodyParams.add(new KeyValuePair<>("dictionaries[0].target_lang", targetLanguageCode));
    bodyParams.add(new KeyValuePair<>("dictionaries[0].entries", entries));
    bodyParams.add(new KeyValuePair<>("dictionaries[0].entries_format", entriesFormat));

    return bodyParams;
  }
}
