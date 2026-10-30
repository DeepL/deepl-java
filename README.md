# DeepL Java Library

[![Maven Central](https://img.shields.io/maven-central/v/com.deepl.api/deepl-java.svg)](https://mvnrepository.com/artifact/com.deepl.api/deepl-java)
[![License: MIT](https://img.shields.io/badge/license-MIT-blueviolet.svg)](https://github.com/DeepLcom/deepl-java/blob/main/LICENSE)

The [DeepL API][api-docs] is a language AI API that allows other computer programs
to send texts and documents to DeepL's servers and receive high-quality
translations and improvements to the text. This opens a whole universe of
opportunities for developers: any translation product you can imagine can now
be built on top of DeepL's best-in-class translation technology.

The DeepL Java library offers a convenient way for applications written in Java
to interact with the DeepL API. We intend to support all API functions with the
library, though support for new features may be added to the library after
they’re added to the API.

## Getting an authentication key

To use the DeepL Java Library, you'll need an API authentication key. To get a
key, [please create an account here][create-account]. With a DeepL API Free
account you can consume up to 500,000 characters/month for free.

## Requirements

Java 1.8 or later.

## Installation

### Gradle users

Add this dependency to your project's build file:

```
implementation "com.deepl.api:deepl-java:1.17.0"
```

### Maven users

Add this dependency to your project's POM:

```
<dependency>
  <groupId>com.deepl.api</groupId>
  <artifactId>deepl-java</artifactId>
  <version>1.17.0</version>
</dependency>
```

## Usage

Import the package and construct a `DeepLClient`. The first argument is a string
containing your API authentication key as found in your
[DeepL Pro Account][pro-account].

Be careful not to expose your key, for example when sharing source code.

```java
import com.deepl.api.*;

class Example {
    DeepLClient client;

    public Example() throws Exception {
        String authKey = "f63c02c5-f056-...";  // Replace with your key
        client = new DeepLClient(authKey);
        TextResult result =
                client.translateText("Hello, world!", null, "fr");
        System.out.println(result.getText()); // "Bonjour, le monde !"
    }
}
```

This example is for demonstration purposes only. In production code, the
authentication key should not be hard-coded, but instead fetched from a
configuration file or environment variable.

`DeepLClient` accepts additional options, see [Configuration](#configuration)
for more information.

### Translating text

To translate text, call `translateText()`. The first argument is a string
containing the text you want to translate, or an iterable of strings if you want
to translate multiple texts.

`sourceLang` and `targetLang` specify the source and target language codes
respectively. The `sourceLang` is optional, if it is `null` the source language
will be auto-detected.

Language codes are **case-insensitive** strings according to ISO 639-1, for
example `'de'`, `'fr'`, `'ja''`. Some target languages also include the regional
variant according to ISO 3166-1, for example `'en-US'`, or `'pt-BR'`. The bare
codes `'en'` and `'pt'` are also accepted as target languages and translate to
the default variants `'en-US'` and `'pt-PT'`; use a regional variant to choose
explicitly. The full list of supported languages is in the
[API documentation][api-docs-lang-list]. The codes are sent with the language
part lower-cased and the rest as given, for example `zh-Hans`. Language codes are
plain strings; the `LanguageCode` constants are deprecated and will be removed
in the next major version.

There are additional optional arguments to control translation, see
[Text translation options](#text-translation-options) below.

`translateText()` returns a `TextResult`, or a List of `TextResult`s
corresponding to your input text(s). `TextResult` has the following accessors:
- `getText()` returns the translated text,
- `getDetectedSourceLanguage()` returns the detected source language code,
- `getBilledCharacters()` returns the number of characters billed for the text
  (0 if the response does not contain it),
- `getModelTypeUsed()` returns the model type used for the translation, as a
  string such as `"quality_optimized"` (see `ModelType`), or `null`, and
- `getTagHandlingVersion()` returns the tag handling version used, as a string
  such as `"v2"` (see `TagHandlingVersion`); it is `null` unless tag handling
  was requested.

```java
class Example {  // Continuing class Example from above
    public void textTranslationExamples() throws Exception {
        // Translate text into a target language, in this case, French:
        TextResult result =
                client.translateText("Hello, world!", null, "fr");
        System.out.println(result.getText()); // "Bonjour, le monde !"

        // Translate multiple texts into British English
        List<TextResult> results =
                client.translateText(List.of("お元気ですか？", "¿Cómo estás?"),
                                         null,
                                         "en-GB");
        System.out.println(results.get(0).getText()); // "How are you?"
        System.out.println(results.get(0).getDetectedSourceLanguage()); // "ja" the language code for Japanese
        System.out.println(results.get(0).getBilledCharacters()); // 7 - the number of characters in the source text "お元気ですか？"
        System.out.println(results.get(1).getText()); // "How are you?"
        System.out.println(results.get(1).getDetectedSourceLanguage()); // "es" the language code for Spanish
        System.out.println(results.get(1).getBilledCharacters()); // 12 - the number of characters in the source text "¿Cómo estás?"

        // Translate into German with less and more Formality:
        System.out.println(client.translateText("How are you?",
                                                    null,
                                                    "de",
                                                    new TextTranslationOptions().setFormality(
                                                            Formality.Less)).getText());  // 'Wie geht es dir?'
        System.out.println(client.translateText("How are you?",
                                                    null,
                                                    "de",
                                                    new TextTranslationOptions().setFormality(
                                                            Formality.More)).getText());  // 'Wie geht es Ihnen?'
    }
}
```

#### Text translation options

In addition to the input text(s) argument, a `translateText()` overload accepts
a `TextTranslationOptions`, with the following setters:

- `setSentenceSplittingMode()`: specify how input text should be split into
  sentences, default: `'on'`.
    - `SentenceSplittingMode.All`: input text will be split into sentences using
      both newlines and punctuation.
    - `SentenceSplittingMode.Off`: input text will not be split into sentences.
      Use this for applications where each input text contains only one
      sentence.
    - `SentenceSplittingMode.NoNewlines`: input text will be split into
      sentences using punctuation but not newlines.
    - A string is also accepted and sent as given in `split_sentences`, for
      example `"nonewlines"`.
- `setPreserveFormatting()`: controls automatic-formatting-correction. Set to
  `True` to prevent automatic-correction of formatting, default: `false`.
- `setFormality()`: controls whether translations should lean toward informal or
  formal language. This option is only available for some target languages, see
  [Listing available languages](#listing-available-languages).
    - `Formality.Less`: use informal language.
    - `Formality.More`: use formal, more polite language.
    - A string is also accepted and sent as given, for example `"prefer_less"`,
      for values not yet in `Formality`.
- `setGlossary()`: specifies a glossary to use with translation, as a string
  containing the glossary ID, or a `GlossaryInfo`/`MultilingualGlossaryInfo` 
  object (this object is returned by glossary lookup functions, for example 
  `listGlossaries()` or `listMultilingualGlossaries()`).
    - `setGlossaryId()` is deprecated; use `setGlossary()` instead.
- `setGlossaryIds()`: specifies multiple glossaries to use with translation
  (up to 5), as a list or varargs of glossary ID strings, or as one or more
  `IGlossary` objects (`GlossaryInfo`/`MultilingualGlossaryInfo`; for a list of
  glossary objects, pass their IDs as a list of strings).
  Glossaries are applied in the order provided (first match wins). This option
  cannot be combined with `setGlossary()`.
    - `setGlossaries()` is deprecated; use `setGlossaryIds()` instead.
- `setStyleRule()`: specifies a style rule to use with translation, as a string
  containing the ID of the style rule, or a `StyleRuleInfo` object.
    - `setStyleId()` is deprecated; use `setStyleRule()` instead.
- `setStyleProfile()`: specifies a style profile to use with translation, as a
  string containing the style profile ID, or a `StyleProfile` object. The profile's glossaries, style rules and translation
  memories are applied per text once the source language is known; a component
  that does not fit the language pair is left out. No source language is
  required. Cannot be combined with the glossary, style rule, translation memory
  or custom instruction options; the API rejects such a request.
- `setTranslationMemory()`: specifies a translation memory to use with translation,
  as a string containing the translation memory ID, or a `TranslationMemoryInfo`
  object.
    - `setTranslationMemoryId()` is deprecated; use `setTranslationMemory()` instead.
    - `setTranslationMemoryThreshold()` is also available, accepting an integer
      from 0 to 100 to control the minimum matching percentage for translation
      memory matches. We recommend a minimum threshold of 75%.
- `setContext()`: specifies additional context to influence translations, that is not
  translated itself. Characters in the `context` parameter are not counted toward billing.
  See the [API documentation][api-docs-context-param] for more information and
  example usage.
- `setCustomInstructions()`: an array of instructions to customize the translation behavior.
  Up to 10 custom instructions can be specified, each with a maximum of 300 characters.
  Important: The target language must be `de`, `en`, `es`, `fr`, `it`, `ja`, `ko`, `zh`
  or any variants of these languages.
  Note: Any request with the custom instructions parameter enabled will use the
  `quality_optimized` model type as the default. Requests combining custom instructions
  and `model_type: latency_optimized` will be rejected.
- `setModelType()`: specifies the type of translation model to use, as a
  `ModelType` or a string, options are:
  - `ModelType.QualityOptimized` (`'quality_optimized'`): use a translation model that
    maximizes translation quality, at the cost of response time. This option may be
    unavailable for some language pairs.
  - `ModelType.PreferQualityOptimized` (`'prefer_quality_optimized'`): use the
    highest-quality translation model for the given language pair.
  - `ModelType.LatencyOptimized` (`'latency_optimized'`): use a translation model that
    minimizes response time, at the cost of translation quality.
- `setTagHandling()`: type of tags to parse before translation, as a
  `TagHandlingMode` (`TagHandlingMode.Html`, `TagHandlingMode.Xml`) or a string
  (`"html"`, `"xml"`).
- `setTagHandlingVersion()`: specifies which version of the tag handling algorithm
  to use, as a `TagHandlingVersion` (`TagHandlingVersion.V1`, `TagHandlingVersion.V2`)
  or a string (`"v1"`, `"v2"`).

The following options are only used if `setTagHandling()` is set to `'xml'`:

- `setOutlineDetection()`: specify `false` to disable automatic tag detection,
  default is `true`.
- `setSplittingTags()`: list of XML tags that should be used to split text into
  sentences. Tags may be specified as an `Iterable<String>`
  (`Arrays.asList("tag1", "tag2")`), or a comma-separated string (`"tag1,tag2"`)
  that is sent as given. The default is an empty
  list.
- `setNonSplittingTags()`: list of XML tags that should not be used to split
  text into sentences. Format and default are the same as for splitting tags.
- `setIgnoreTags()`: list of XML tags that containing content that should not be
  translated. Format and default are the same as for splitting tags.
- `setExtraBodyParameters()`: a map of additional parameters added last to the
  request body, for example to access beta features. They replace built-in
  parameters with the same name. Further setters can be chained after it.

For a detailed explanation of the XML handling options, see the
[API documentation][api-docs-xml-handling].

### Improving text (Write API)

You can use the Write API to improve or rephrase text. This is implemented in
the `rephraseText()` method. The first argument is a string containing the text
you want to translate, or a list of strings if you want to translate multiple texts.

`targetLang` optionally specifies the target language, e.g. when you want to change
the variant of a text (for example, you can send an english text to the write API and
use `targetLang` to turn it into British or American English). Please note that the
Write API itself does NOT translate. If you wish to translate and improve a text, you
will need to make multiple calls in a chain.

Language codes are the same as for translating text.

Example call:

```java
WriteResult result = client.rephraseText("A rainbouw has seven colours.", "EN-US", null);
System.out.println(result.getText);
```

Additionally, you can optionally specify a style OR a tone (not both at once) that the
improvement should be in. The following styles are supported (`default` will be used if
nothing is selected):

- `academic`
- `business`
- `casual`
- `default`
- `simple`

The following tones are supported (`default` will be used if nothing is selected):

- `confident`
- `default`
- `diplomatic`
- `enthusiastic`
- `friendly`

You can also prefix any non-default style or tone with `prefer_` (`prefer_academic`, etc.),
in which case the style/tone will only be applied if the language supports it. If you do not
use `prefer_`, requests with `targetLang`s or detected languages that do not support
styles and tones will fail. The current list of supported languages can be found in our
[API documentation][api-docs]. We plan to also expose this information via an API endpoint
in the future.

`setWritingStyle()` and `setTone()` take the `WritingStyle` and `WritingTone`
enums or a string, which is sent as given:

```java
TextRephraseOptions options = (new TextRephraseOptions()).setWritingStyle(WritingStyle.Business);
WriteResult result = client.rephraseText("A rainbouw has seven colours.", "EN-US", options);
System.out.println(result.getText);
```

`TextRephraseOptions` also supports `setExtraBodyParameters()`: a map of
additional parameters added last to the request body. They replace built-in
parameters with the same name.

To correct grammar, spelling and punctuation without rewriting the text, use
`correctText()`. It takes a string or a list of strings, an optional target
language and an optional `CorrectTextOptions`, which supports
`setExtraBodyParameters()`:

```java
WriteResult result = client.correctText("A rainbouw has seven colours.", "EN-US", null);
System.out.println(result.getText());
```

Both functions return one `WriteResult` per input text. A response that does
not contain one complete result per input text raises a `DeepLException`.

### Translating documents

To translate documents, call `translateDocument()` File objects. The first and
second arguments correspond to the input and output files respectively.

Just as for the `translateText()` function, the `sourceLang` and
`targetLang` arguments specify the source and target language codes.

There are additional optional arguments to control translation, see
[Document translation options](#document-translation-options) below.

```java
class Example {  // Continuing class Example from above
    public void documentTranslationExamples() throws Exception {
        // Translate a formal document from English to German
        File inputFile = new File("/path/to/Instruction Manual.docx");
        File outputFile = new File("/path/to/Bedienungsanleitung.docx");
        try {
            client.translateDocument(inputFile, outputFile, "en", "de");
        } catch (DocumentTranslationException exception) {
            // If an error occurs during document translation after the document was
            // already uploaded, a DocumentTranslationException is thrown. The
            // document_handle property contains the document handle that may be used to
            // later retrieve the document from the server, or contact DeepL support.
            DocumentHandle handle = exception.getHandle();
            System.out.printf(
                    "Error after uploading %s, document handle: id: %s key: %s",
                    exception.getMessage(),
                    handle.getDocumentId(),
                    handle.getDocumentKey());
        }
    }
}
```

If the input file does not exist, `translateDocument()` throws an
`IllegalArgumentException` before sending any request. Every other failure,
including an existing output file or a file that cannot be opened, is thrown as
a `DocumentTranslationException`; `getHttpStatusCode()` and `getShouldRetry()`
report those of the underlying `DeepLException`, if any.

`translateDocument()` is a convenience function that wraps multiple API calls:
uploading, polling status until the translation is complete, and downloading. If
your application needs to execute these steps individually, you can instead use
the following functions directly:

- `translateDocumentUpload()`,
- `translateDocumentGetStatus()` (or
  `translateDocumentWaitUntilDone()`), and
- `translateDocumentDownload()`

`translateDocumentStatus()` is deprecated; use `translateDocumentGetStatus()`
instead. `translateDocumentWaitUntilDone()` polls the status every 5 seconds
until the document is done; an overload taking a `Duration` timeout throws a
`DeepLException` when the timeout expires. The returned `DocumentStatus` has
`getStatus()` (a `DocumentStatus.StatusCode`, or `null` for a status the
library does not know yet), `getStatusString()` (the status as the API sent
it), `ok()` (not failed) and `done()` (succeeded). An unknown status counts as
in progress, so waiting continues.

The source and target language codes are sent with the language part
lower-cased and the rest as given, for example `zh-Hans`.

#### Document translation options

In addition to the input file, output file, `sourceLang` and `targetLang`
arguments, `translateDocument()` accepts an optional
`DocumentTranslationOptions`, with the following setters:

- `setFormality()`: same as
  in [Text translation options](#text-translation-options).
- `setGlossary()`: same as
  in [Text translation options](#text-translation-options), but requires a
  source language.
    - `setGlossaryId()` is deprecated; use `setGlossary()` instead.
- `setGlossaryIds()`: same as
  in [Text translation options](#text-translation-options). Specifies multiple
  glossaries (up to 5) to use with document translation, as glossary IDs or
  `IGlossary` objects. Requires a source language.
    - `setGlossaries()` is deprecated; use `setGlossaryIds()` instead.
- `setStyleRule()`: same as
  in [Text translation options](#text-translation-options), taking a style rule
  ID or a `StyleRuleInfo` object.
    - `setStyleId()` is deprecated; use `setStyleRule()` instead.
- `setStyleProfile()`: same as
  in [Text translation options](#text-translation-options). An unknown style
  profile is not rejected at upload; the document status reports an error and
  a `DocumentTranslationException` is thrown with the message
  `Style profile not found.`
- `setTranslationMemory()`/`setTranslationMemoryThreshold()`:
  same as in [Text translation options](#text-translation-options), taking a
  translation memory ID or a `TranslationMemoryInfo` object.
    - `setTranslationMemoryId()` is deprecated; use `setTranslationMemory()` instead.
- `setFilename()`: the file name sent with the upload, for example
  `report.docx`; its extension determines the document type. Used in place of
  the input file's name and of the `fileName` argument of the stream overloads.
- `setTimeout()`: a `Duration` limiting how long `translateDocument()` waits
  for the translation after the upload. When it expires, a
  `DocumentTranslationException` with the document handle is thrown. Not sent
  to the API; by default there is no limit, and a duration longer than about
  292 years also means no limit.
- `setOutputFormat()`: the file format of the translated document, as a file
  extension, for example `docx`. By default the translated document has the
  same format as the input document.
- `setEnableWatermark()`: set to `true` to add a "Translated by DeepL"
  watermark to the translated document. Only supported for `docx` and `pdf`
  output; for other output formats the API ignores it. By default the option
  is not sent and no watermark is added.
- `setExtraBodyParameters()`: a map of additional parameters added last to the
  upload request, as for text translation.

### Glossaries

Glossaries allow you to customize your translations using user-defined terms.
Multiple glossaries can be stored with your account, each with a user-specified
name and a uniquely-assigned ID.

### v2 versus v3 glossary APIs

The newest version of the glossary APIs are the `/v3` endpoints, allowing both
editing functionality plus support for multilingual glossaries. New methods and
objects have been created to support interacting with these new glossaries.
Due to this new functionality, users are recommended to utilize these
multilingual glossary methods. However, to continue using the `v2` glossary API
endpoints, please continue to use the existing endpoints in the `translator.java`
(e.g. `createGlossary()`, `getGlossary()`, etc).

To migrate to use the new multilingual glossary methods from the current
monolingual glossary methods, please refer to
[this migration guide](upgrading_to_multilingual_glossaries.md).

The following sections describe how to interact with multilingual glossaries
using the new functionality:

#### Creating a glossary

You can create a glossary with `createMultilingualGlossary()` by passing your
desired glossary name, and a `GlossaryEntries` object specifying the terms to
store in the glossary.

Each glossary contains a list of dictionaries, where each dictionary applies to a single source-target language pair. Note: Glossaries
are only supported for some language pairs, see
[Listing available glossary languages](#listing-available-glossary-languages)
for more information.

If successful, the glossary is created and stored with your DeepL account, and
a `MultilingualGlossaryInfo` object is returned including the ID, name, 
languages and entry count.

```java
class Example {  // Continuing class Example from above
    public void createGlossaryExample() throws Exception {
        // Create an English to German glossary with two terms:
        GlossaryEntries entries = new GlossaryEntries() {{
            put("artist", "Maler");
            put("prize", "Gewinn");
        }};
        MultilingualGlossaryDictionaryEntries myGlossaryDicts = 
                Arrays.asList(new MultilingualGlossaryDictionaryEntries(
                                "en",
                                "de",
                                entries
                              ));
        MultilingualGlossaryInfo myGlossary =
                client.createGlossary("My glossary", myGlossaryDicts);

        System.out.printf("Created '%s' (%s) containing %d dictionary from %s->%s with %d entries\n",
                          myGlossary.getName(),
                          myGlossary.getGlossaryId(),
                          myGlossary.getDictionaries().length,
                          myGlossary.getSourceLang(),
                          myGlossary.getTargetLang(),
                          myGlossary.getEntryCount());
        // Example: Created 'My glossary' (559192ed-8e23-...) containing 1 
        // dictionary from en->de containing 2 entries
    }
}
```

To construct the GlossaryEntries, you can insert entries using typical Map 
functions like `put()`. The `fromTsv()` function allows creating GlossaryEntries
from TSV data. Each term is trimmed of leading and trailing whitespace, then
checked: a term that is empty or contains a control character throws an
`IllegalArgumentException`. The trimmed terms are stored and sent. Entries can
also be changed with `replaceAll()` or `Map.Entry.setValue()`, which check the
new terms the same way.

You can also create a glossary using a glossary downloaded from the DeepL
website by using `createMultilingualGlossaryFromCsv()` with either a CSV file,
or a string containing the CSV data:

```java
class Example {  // Continuing class Example from above
    public createGlossaryFromCsvExample() throws Exception {
        File csvFile = new File("/path/to/glossary_file.csv");
        MultilingualGlossaryInfo myGlossary =
                client.createMultilingualGlossaryFromCsv("My glossary",
                                                         "en",
                                                         "de",
                                                         csvFile);
    }
}
```

The [API documentation][api-docs-csv-format] explains the expected CSV format in
detail.


#### Getting, listing and deleting stored glossaries

Functions to get, list, and delete stored glossaries are also provided:

- `getMultilingualGlossary()` takes a glossary ID or `MultilingualGlossaryInfo`
  object and returns a `MultilingualGlossaryInfo` object for a stored glossary,
  or raises an exception if no such glossary is found.
- `listMultilingualGlossaries()` returns a list of `MultilingualGlossaryInfo`
  objects corresponding to all of your stored glossaries.
- `deleteMultilingualGlossary()` takes a glossary ID or
  `MultilingualGlossaryInfo` object and deletes the stored glossary from the
  server, or raises an exception if no such glossary is found.
- `deleteMultilingualGlossaryDictionary()` takes a glossary ID or
  `MultilingualGlossaryInfo` object to identify the glossary. Additionally
  takes in a source and target language or a
  `MultilingualGlossaryDictionaryInfo` object and deletes the stored dictionary
  from the server, or raises an exception if no such glossary dictionary is
  found.

```java
class Example {  // Continuing class Example from above
    public getListDeleteGlossaryExamples() throws Exception {
        // Retrieve a stored glossary using the ID
        String glossaryId = "559192ed-8e23-...";
        MultilingualGlossaryInfo myGlossary = 
          client.getMultilingualGlossary(glossaryId);

        client.deleteMultilingualGlossaryDictionary(glossaryId, 
                                                    myGlossary.getDictionaries()[0]);

        // Find and delete glossaries named 'Old glossary'
        List<MultilingualGlossaryInfo> glossaries = 
          client.listMultilingualGlossaries();
        for (MultilingualGlossaryInfo glossary : glossaries) {
            if (glossary.getName() == "Old glossary") {
                client.deleteMultilingualGlossary(glossary);
            }
        }
    }
}
```

#### Listing entries in a stored glossary

The `MultilingualGlossaryDictionaryInfo` object does not contain the glossary
entries, but instead only the number of entries in the `entry_count` property.

To list the entries contained within a stored glossary, use
`getMultilingualGlossaryDictionaryEntries()` providing either the 
`MultilingualGlossaryInfo` object or glossary ID and either a 
`MultilingualGlossaryDictionaryInfo` or source and target language pair. If the
glossary has no dictionary for that language pair, a `NotFoundException` is
thrown:

```java
class Example {  // Continuing class Example from above
  public getGlossaryEntriesExample() throws Exception {
      List<MultilingualGlossaryDictionaryInfo> glossaryDicts = 
        client.getMultilingualGlossaryDictionaryEntries(myGlossary, "en", "de");
      
      for (Map.Entry<String, String> entry : glossaryDicts.getDictionaries()[0].getEntries().entrySet()) {
        System.out.println(entry.getKey() + ":" + entry.getValue());
      }
      // prints:
      //   artist:Maler
      //   prize:Gewinn
  }
}
```

#### Editing a glossary

Functions to edit stored glossaries are also provided:

- `updateMultilingualGlossaryDictionary()` takes a glossary ID or `MultilingualGlossaryInfo`
  object, plus a source language, target language, and a dictionary of entries.
  It will then either update the list of entries for that dictionary (either
  inserting new entires or replacing the target phrase for any existing
  entries) or will insert a new glossary dictionary if that language pair is
  not currently in the stored glossary.
- `replaceMultilingualGlossaryDictionary()` takes a glossary ID or `MultilingualGlossaryInfo`
  object, plus a source language, target language, and a dictionary of entries.
  It will then either set the entries to the parameter value, completely
  replacing any pre-existing entries for that language pair.
- `updateMultilingualGlossaryName()` takes a glossary ID or `MultilingualGlossaryInfo`
  object, plus the new name of the glossary.
- `updateMultilingualGlossary()` takes a glossary ID or `MultilingualGlossaryInfo`
  object and an `UpdateMultilingualGlossaryOptions` object, and changes the
  name and merges the dictionaries set in it in one request; fields that are not
  set are not sent.
- `updateMultilingualGlossaryDictionaryFromCsv()` and
  `replaceMultilingualGlossaryDictionaryFromCsv()` take a glossary ID or
  `MultilingualGlossaryInfo` object, a source and target language and the CSV
  content as a `String` or `File`.

The create, update and replace functions also have overloads taking an options
object as the last argument (`CreateMultilingualGlossaryOptions`,
`UpdateMultilingualGlossaryOptions` and
`ReplaceMultilingualGlossaryDictionaryOptions`; the `updateMultilingualGlossaryName()`
and `updateMultilingualGlossaryDictionary*()` overloads accept any options object
and read only its extra body parameters), whose `setExtraBodyParameters()`
values are added to the request body last.

```java
class Example {  // Continuing class Example from above
  public void updateGlossaryExample() throws Exception {
    GlossaryEntries entries = new GlossaryEntries(Collections.singletonMap("goodbye", "Tschüss"));
    MultilingualGlossaryInfo updatedGlossary =
      client.updateMultilingualGlossary(myGlossary,
        new UpdateMultilingualGlossaryOptions()
          .setName("My renamed glossary")
          .setDictionaries(Arrays.asList(
            new MultilingualGlossaryDictionaryEntries("en", "de", entries))));
  }
}
```

```java
// Update glossary dictionary
class Example {  // Continuing class Example from above
  public updateGlossaryEntriesExample() throws Exception {
    GlossaryEntries entries = new GlossaryEntries() {{
              put("artist", "Maler");
              put("hello", "guten tag");
          }};
    List<MultilingualGlossaryDictionaryEntries> dictionaries = 
      Arrays.asList(new MultilingualGlossaryDictionaryEntries("EN", "DE", entries));
    MultilingualGlossaryInfo myGlossary = client.createMultilingualGlossary(
        "My glossary",
        dictionaries
    );

    GlossaryEntries newEntries = new GlossaryEntries() {{
              put("hello", "hallo");
              put("prize", "Gewinn");
          }};

    MultilingualGlossaryDictionaryEntries glossaryDict = 
      new MultilingualGlossaryDictionaryEntries("EN", "DE", newEntries);

    MultilingualGlossaryInfo updatedGlossary = 
      client.updateMultilingualGlossaryDictionary(myGlossary, glossaryDict);

    MultilingualGlossaryInfo entriesResponse = 
      client.getMultilingualGlossaryDictionaryEntries(myGlossary, "EN", "DE");

    for (Map.Entry<String, String> entry : glossaryDicts.getDictionaries()[0].getEntries().entrySet()) {
      System.out.println(entry.getKey() + ":" + entry.getValue());
    }
    
    // prints:
    //   artist:Maler
    //   hello:hallo
    //   prize:Gewinn
  }

  // Update a glossary dictionary from CSV
  public updateGlossaryEntriesFromCsvExample() throws Exception {
    File csvFile = new File("/path/to/glossary_file.csv");
    String glossaryId = "559192ed-8e23-...";
    MultilingualGlossaryInfo myGlossary =
      client.createMultilingualGlossaryDictionaryFromCsv(glossaryId,
                                                        "en",
                                                        "de",
                                                        csvFile);
  }


  // Update a glossary name
  public void updateGlossaryNameExample() throws Exception {
    String glossaryId = "559192ed-8e23-...";
    MultilingualGlossaryInfo myGlossary =
      client.updateMultilingualName(glossaryId, "My new glossary name");
    System.out.println(myGlossary.getName()); // 'My new glossary name'
  }

  // Replace a glossary dictionary
  public void replaceGlossaryEntriesExample() throws Exception {
    GlossaryEntries entries = new GlossaryEntries() {{
              put("artist", "Maler");
              put("hello", "guten tag");
          }};
    List<MultilingualGlossaryDictionaryEntries> dictionaries = 
      Arrays.asList(new MultilingualGlossaryDictionaryEntries("EN", "DE", entries));
    MultilingualGlossaryInfo myGlossary = client.createMultilingualGlossary(
        "My glossary",
        dictionaries
    );

    GlossaryEntries newEntries = new GlossaryEntries() {{
              put("goodbye", "Auf Weidersehen");
          }};

    MultilingualGlossaryDictionaryEntries glossaryDict = 
      new MultilingualGlossaryDictionaryEntries("EN", "DE", newEntries);

    MultilingualGlossaryInfo updatedGlossary = 
      client.replaceMultilingualGlossaryDictionary(myGlossary, glossaryDict);

    MultilingualGlossaryInfo entriesResponse = 
      client.getMultilingualGlossaryDictionaryEntries(myGlossary, "EN", "DE");

    for (Map.Entry<String, String> entry : glossaryDicts.getDictionaries()[0].getEntries().entrySet()) {
      System.out.println(entry.getKey() + ":" + entry.getValue());
    }
    
    // prints:
    //   goodbye:Auf Weidersehen
  }

  // Replace a glossary dictionary from CSV
  public void replaceGlossaryEntriesFromCsvExample() throws Exception {
    File csvFile = new File("/path/to/glossary_file.csv");
    String glossaryId = "559192ed-8e23-...";
    MultilingualGlossaryInfo myGlossary =
      client.replaceMultilingualGlossaryDictionaryFromCsv(glossaryId,
                                                          "en",
                                                          "de",
                                                          csvFile);
    MultilingualGlossaryInfo entriesResponse = 
      client.getMultilingualGlossaryDictionaryEntries(myGlossary, "EN", "DE");
  }
}
```

#### Using a stored glossary

You can use a stored glossary for text translation by setting the `glossary`
argument to either the glossary ID or `MultilingualGlossaryInfo` object. The
`source_lang` argument is optional: if it is `null`, DeepL detects the source
language and uses the glossary's dictionary for the detected language pair:

```java
class Example {  // Continuing class Example from above
    public boid usingGlossaryExample() throws Exception {
        String text = "The artist was awarded a prize.";
        TextTranslationOptions options =
                new TextTranslationOptions().setGlossary(my_glossary);
        TextResult resultWithGlossary =
                client.translateText(text, "en", "de", options);
        System.out.println(resultWithGlossary.getText()); // "Der Maler wurde mit einem Gewinn ausgezeichnet."

        // For comparison, the result without a glossary:
        TextResult resultWithoutGlossary =
                client.translateText(text, "en", "de");
        System.out.println(resultWithoutGlossary.getText()); // "Der Künstler wurde mit einem Preis ausgezeichnet."
    }
}
```

Using a stored glossary for document translation is the same, except that the
`source_lang` argument is required: set the `glossary` argument and specify the
`source_lang` argument:

```java
class Example {  // Continuing class Example from above
    public boid getListDeleteGlossaryExamples() throws Exception {
        String glossaryId = "559192ed-8e23-...";
        DocumentTranslationOptions options =
                new DocumentTranslationOptions().setGlossary(glossaryId);

        File inputFile = new File("/path/to/Instruction Manual.docx");
        File outputFile = new File("/path/to/Bedienungsanleitung.docx");
        client.translateDocument(inputFile,
                                     outputFile,
                                     "en",
                                     "de",
                                     options);
    }
}
```

The `translateDocument()` and `translateDocumentUpload()` functions both
support the `glossary` argument.

### Spoken Terms

Spoken Terms collections improve the transcription of terms such as product
names and technical vocabulary. A collection holds one term list per language
(ISO 639-1 code, for example `en`); each term list is a list of terms. Multiple
collections can be stored with your account, each with a user-specified name
and a uniquely-assigned ID.

Each method that takes a collection accepts either its ID or a `SpokenTerms`
object. Each term is trimmed of leading and trailing whitespace and then
checked like a glossary term before any request is sent: a term that is empty
or contains a control character such as a newline throws an
`IllegalArgumentException`. Other invalid term lists are rejected by the API
with a `DeepLException`.

#### Creating a Spoken Terms collection

Use `createSpokenTerms()` with a name and a list of `SpokenTermsEntries`, one
per language. It returns a `SpokenTerms` object with the ID, name, creation
time and the term lists (`SpokenTermsList`: language and entry count). An
optional third argument, `CreateSpokenTermsOptions`, supports
`setExtraBodyParameters()`: a map of additional parameters added last to the
JSON request body, replacing any parameter with the same key.

```java
class Example {  // Continuing class Example from above
    public void createSpokenTermsExample() throws Exception {
        SpokenTerms spokenTerms = client.createSpokenTerms(
            "Technical Terms",
            Arrays.asList(new SpokenTermsEntries("en", Arrays.asList("DeepL", "API", "webhook"))));
        System.out.println("Created " + spokenTerms.getSpokenTermsId());
        for (SpokenTermsList termList : spokenTerms.getTermLists()) {
            System.out.println(termList.getLang() + ": " + termList.getEntryCount() + " terms");
        }
    }
}
```

#### Getting and listing Spoken Terms collections

`getSpokenTerms()` retrieves one collection, `listSpokenTerms()` all of them.
Neither returns the terms themselves; use `getSpokenTermsEntries()` with a
language code for those:

```java
class Example {  // Continuing class Example from above
    public void getSpokenTermsExample() throws Exception {
        SpokenTerms spokenTerms = client.getSpokenTerms("def3a26b-3e84-45b3-84ae-0c0aaf3525f7");
        System.out.println(spokenTerms.getName() + " " + spokenTerms.getCreationTime());

        for (SpokenTerms collection : client.listSpokenTerms()) {
            System.out.println(collection.getName() + " (" + collection.getSpokenTermsId() + ")");
        }

        SpokenTermsEntries entries = client.getSpokenTermsEntries(spokenTerms, "en");
        System.out.println(entries.getEntries()); // [API, DeepL, webhook]
    }
}
```

#### Editing a Spoken Terms collection

`updateSpokenTerms()` changes the name and/or merges terms into the existing
term lists. It takes an `UpdateSpokenTermsOptions` and sends only the fields
that are set:

- `setName()`: the new name of the collection.
- `setTermLists()`: term lists to merge, one `SpokenTermsEntries` per language.
- `setExtraBodyParameters()`: a map of additional parameters added last to the
  JSON request body, replacing any parameter with the same key.

`replaceSpokenTermsList()` replaces the term list of one language, or adds it
if the collection has none for that language; an optional third argument,
`ReplaceSpokenTermsListOptions`, supports `setExtraBodyParameters()`.
`deleteSpokenTermsList()` removes the term list of one language:

```java
class Example {  // Continuing class Example from above
    public void editSpokenTermsExample() throws Exception {
        String spokenTermsId = "def3a26b-3e84-45b3-84ae-0c0aaf3525f7";

        // Rename the collection
        client.updateSpokenTerms(spokenTermsId,
            new UpdateSpokenTermsOptions().setName("Product Terms"));

        // Merge a term into the "en" term list
        SpokenTerms updated = client.updateSpokenTerms(spokenTermsId,
            new UpdateSpokenTermsOptions().setTermLists(
                Arrays.asList(new SpokenTermsEntries("en", Arrays.asList("authentication")))));

        // Replace the "en" term list, then add and remove a "de" term list
        SpokenTermsList replaced = client.replaceSpokenTermsList(spokenTermsId,
            new SpokenTermsEntries("en", Arrays.asList("API")));
        System.out.println(replaced.getEntryCount()); // 1
        client.replaceSpokenTermsList(spokenTermsId,
            new SpokenTermsEntries("de", Arrays.asList("Schnittstelle")));
        client.deleteSpokenTermsList(spokenTermsId, "de");
    }
}
```

#### Deleting a Spoken Terms collection

```java
class Example {  // Continuing class Example from above
    public void deleteSpokenTermsExample() throws Exception {
        client.deleteSpokenTerms("def3a26b-3e84-45b3-84ae-0c0aaf3525f7");
    }
}
```

### Style Rules

Style rules allow you to customize your translations using a managed, shared list
of rules for style, formatting, and more. Multiple style rules can be stored with
your account, each with a user-specified name and a uniquely-assigned ID.

#### Creating a style rule

Use `createStyleRule()` to create a new style rule. You must specify a name and
language code. You may optionally provide configured rules and custom
instructions. Configured rules are a `Map<String, Map<String, String>>` from
category to rule to value, in the API's snake_case form (for example
`"dates_and_times"` → `"calendar_era"` → `"use_bc_and_ad"`), and are sent as
given, so new categories and rules can be used as soon as the API supports
them. The last argument is a `CreateStyleRuleOptions` object or `null`; its
`setExtraBodyParameters()` adds further body parameters, sent last. The
overload taking a `ConfiguredRules` object is deprecated; use the `Map`
overload instead.

```java
class Example {  // Continuing class Example from above
    public void createStyleRuleExample() throws Exception {
        // Create a simple style rule
        StyleRuleInfo rule = client.createStyleRule("My Style", "en", null, null, null);
        System.out.println("Created: " + rule.getStyleId());

        // Create with configured rules and custom instructions
        Map<String, Map<String, String>> configuredRules = new HashMap<>();
        configuredRules.put("dates_and_times",
            Collections.singletonMap("calendar_era", "use_bc_and_ad"));
        List<CustomInstruction> instructions = new ArrayList<>();
        instructions.add(new CustomInstruction("Formal tone", "Use formal language", null));
        StyleRuleInfo ruleWithInstructions = client.createStyleRule(
            "Formal Style", "en", configuredRules, instructions, null);
    }
}
```

#### Retrieving and listing style rules

`getStyleRule()` retrieves a single style rule by its ID or `StyleRuleInfo`
object. `getAllStyleRules()`
returns a list of `StyleRuleInfo` objects corresponding to all of your stored
style rules. The method accepts optional parameters: `page` (page number for
pagination, 0-indexed), `pageSize` (number of items per page), and `detailed`.
When `true`, the response includes `configuredRules` and `customInstructions`
for each style rule. When `false` (default), these fields are omitted for faster
responses.

```java
class Example {  // Continuing class Example from above
    public void listStyleRulesExample() throws Exception {
        // Get a single style rule by ID
        StyleRuleInfo rule = client.getStyleRule("dca2e053-8ae5-45e6-a0d2-881156e7f4e4");
        System.out.println(rule.getName());

        // Get all style rules
        List<StyleRuleInfo> styleRules = client.getAllStyleRules();
        for (StyleRuleInfo r : styleRules) {
            System.out.println(String.format("%s (%s)", r.getName(), r.getStyleId()));
        }

        // Get style rules with detailed configuration
        List<StyleRuleInfo> styleRulesDetailed = client.getAllStyleRules(null, null, true);
        for (StyleRuleInfo r : styleRulesDetailed) {
            if (r.getConfiguredRules() != null && r.getConfiguredRules().getNumbers() != null) {
                System.out.println(String.format("Number formatting rules: %s",
                    String.join(", ", r.getConfiguredRules().getNumbers().keySet())));
            }
        }
    }
}
```

#### Updating a style rule

Use `updateStyleRuleName()` to rename a style rule, and
`updateStyleRuleConfiguredRules()` to replace its configured rules. The
configured rules are a `Map<String, Map<String, String>>` sent as given as the
request body; the overload taking a `ConfiguredRules` object is deprecated.
Both methods have overloads that take an options object
(`UpdateStyleRuleNameOptions`, `UpdateStyleRuleConfiguredRulesOptions`) whose
extra body parameters are sent last; for configured rules they are added to the
body as further top-level keys.

Every method that takes a style rule ID (`getStyleRule()`,
`updateStyleRuleName()`, `updateStyleRuleConfiguredRules()`, `deleteStyleRule()`
and the custom instruction methods) also accepts a `StyleRuleInfo` object. IDs
are URL-encoded in the request path.

```java
class Example {  // Continuing class Example from above
    public void updateStyleRuleExample() throws Exception {
        String styleId = "dca2e053-8ae5-45e6-a0d2-881156e7f4e4";

        // Update the name
        StyleRuleInfo updated = client.updateStyleRuleName(styleId, "New Name");
        System.out.println("Updated name: " + updated.getName());

        // Rename, passing the StyleRuleInfo object and extra body parameters
        client.updateStyleRuleName(updated, "Newer Name",
            new UpdateStyleRuleNameOptions().setExtraBodyParameters(
                Collections.singletonMap("param", "value")));

        // Replace the configured rules
        Map<String, Map<String, String>> configuredRules = new HashMap<>();
        configuredRules.put("punctuation",
            Collections.singletonMap("periods_in_academic_degrees", "do_not_use"));
        StyleRuleInfo updatedRules = client.updateStyleRuleConfiguredRules(
            styleId, configuredRules, null);
    }
}
```

#### Managing custom instructions

Custom instructions can be created, retrieved, updated, and deleted individually
within a style rule. The style rule can be given as an ID or a `StyleRuleInfo`
object, and the custom instruction as an ID or a `CustomInstruction` object.
`createStyleRuleCustomInstruction()` and `updateStyleRuleCustomInstruction()`
have overloads that take an options object
(`CreateStyleRuleCustomInstructionOptions`,
`UpdateStyleRuleCustomInstructionOptions`) whose extra body parameters are sent
last:

```java
class Example {  // Continuing class Example from above
    public void customInstructionsExample() throws Exception {
        String styleId = "dca2e053-8ae5-45e6-a0d2-881156e7f4e4";

        // Create a custom instruction
        CustomInstruction instruction = client.createStyleRuleCustomInstruction(
            styleId, "Formal tone", "Always use formal language", null);
        String instructionId = instruction.getId();

        // Retrieve the custom instruction
        CustomInstruction retrieved = client.getStyleRuleCustomInstruction(
            styleId, instructionId);
        System.out.println(retrieved.getLabel());

        // Update the custom instruction
        CustomInstruction updated = client.updateStyleRuleCustomInstruction(
            styleId, instructionId, "Updated label", "Updated prompt", null);

        // Delete the custom instruction, passing the object instead of its ID
        client.deleteStyleRuleCustomInstruction(styleId, updated);
    }
}
```

#### Deleting a style rule

Use `deleteStyleRule()` to permanently remove a style rule from your account:

```java
class Example {  // Continuing class Example from above
    public void deleteStyleRuleExample() throws Exception {
        client.deleteStyleRule("dca2e053-8ae5-45e6-a0d2-881156e7f4e4");
    }
}
```

#### Using a stored style rule in translations

You can use a stored style rule for text translation by setting the `styleRule`
argument to either the style rule ID or `StyleRuleInfo` object:

```java
class Example {  // Continuing class Example from above
    public void usingStyleRuleExample() throws Exception {
        String styleId = "dca2e053-8ae5-45e6-a0d2-881156e7f4e4";
        TextResult result = client.translateText(
            "Hallo, Welt!",
            "de",
            "en-US",
            new TextTranslationOptions().setStyleRule(styleId));
        System.out.println(result.getText());
    }
}
```

### Style Profiles

A style profile bundles glossaries, style rules and translation memories under
one name, with an optional description and industry. Style profiles are managed
with `DeepLClient`; each has a user-specified name and a uniquely-assigned ID.

#### Creating a style profile

Use `createStyleProfile()` to create a style profile. Only the name is required.
Pass a `CreateStyleProfileOptions` to set a description, an industry, the
components to link, and whether the glossary order sets priority.
`setGlossaryIds()`, `setStyleRuleIds()` and `setTranslationMemoryIds()` take a
list of IDs or the objects themselves (`GlossaryInfo` or
`MultilingualGlossaryInfo`, `StyleRuleInfo`, `TranslationMemoryInfo`).
`setExtraBodyParameters()` adds fields to the JSON request body, replacing a
field with the same key. If any of the component IDs cannot be used, the
request fails and no style profile is created.

```java
class Example {  // Continuing class Example from above
    public void createStyleProfileExample() throws Exception {
        // Create a style profile with only a name
        StyleProfile profile = client.createStyleProfile("Legal, German");
        System.out.println("Created: " + profile.getStyleProfileId());

        // Create a style profile with a description and linked components
        StyleProfile linked = client.createStyleProfile(
            "Legal, German",
            new CreateStyleProfileOptions()
                .setDescription("Contract wording for the German market.")
                .setIndustry("Legal")
                .setGlossaryIds(Arrays.asList(
                    "4d0a2e1c-6b93-4f7a-8d15-2c6e9a3b7f81",
                    "7c1b3d2e-0f4a-4e6b-9a8c-5d2f1e3b4a60"))
                .setGlossaryPriorityOrderEnabled(true)
                .setStyleRuleIds(Arrays.asList("dca2e053-8ae5-45e6-a0d2-881156e7f4e4")));
        System.out.println(linked.getStyleRuleIds());

        // Link components by passing the objects instead of their IDs
        StyleRuleInfo rule = client.getStyleRule("dca2e053-8ae5-45e6-a0d2-881156e7f4e4");
        client.createStyleProfile(
            "Legal, German (rules)", new CreateStyleProfileOptions().setStyleRuleIds(rule));
    }
}
```

#### Getting and listing style profiles

`getStyleProfile()` retrieves a style profile by its ID or `StyleProfile` object.
`listStyleProfiles()` returns one page of your style profiles. It accepts the
optional parameters `page` (page number, 0-indexed, default 0) and `pageSize`
(items per page, default 10; the API reduces values above 100 to 100). Optional
fields that a style profile does not have, such as the description and industry,
are `null`; the creation and update times are always set.

```java
class Example {  // Continuing class Example from above
    public void listStyleProfilesExample() throws Exception {
        StyleProfile profile = client.getStyleProfile("3f1c9d0e-7a24-4b58-9c31-8f0e5d2a1b47");
        System.out.println(profile.getName() + ": " + profile.getDescription());

        for (int page = 0; ; page++) {
            List<StyleProfile> profiles = client.listStyleProfiles(page, 100);
            if (profiles.isEmpty()) {
                break;
            }
            for (StyleProfile p : profiles) {
                System.out.println(String.format("%s (%s)", p.getName(), p.getStyleProfileId()));
            }
        }
    }
}
```

#### Updating a style profile

`updateStyleProfile()` takes the ID or `StyleProfile` object and an optional
`UpdateStyleProfileOptions`, which has the setters of `CreateStyleProfileOptions`
plus `setName()`. Fields left `null` are not sent and stay unchanged. An empty string clears the description or industry. A component
list you send replaces the linked list in full; an empty list unlinks all
components of that kind.

```java
class Example {  // Continuing class Example from above
    public void updateStyleProfileExample() throws Exception {
        String profileId = "3f1c9d0e-7a24-4b58-9c31-8f0e5d2a1b47";

        // Rename and set the industry; other fields are unchanged
        client.updateStyleProfile(
            profileId,
            new UpdateStyleProfileOptions().setName("Legal, German (2026)").setIndustry("Legal"));

        // Clear the description and unlink all style rules
        StyleProfile updated = client.updateStyleProfile(
            profileId,
            new UpdateStyleProfileOptions()
                .setDescription("")
                .setStyleRuleIds(Collections.emptyList()));
        System.out.println(updated.getDescription());  // null
    }
}
```

#### Deleting a style profile

Use `deleteStyleProfile()` with the ID or `StyleProfile` object:

```java
class Example {  // Continuing class Example from above
    public void deleteStyleProfileExample() throws Exception {
        client.deleteStyleProfile("3f1c9d0e-7a24-4b58-9c31-8f0e5d2a1b47");
    }
}
```

#### Using a style profile in translations

Pass a `StyleProfile` or an ID to `setStyleProfile()` in `TextTranslationOptions`
(the same in `DocumentTranslationOptions`) to
translate with all of the profile's components through one ID:

```java
class Example {  // Continuing class Example from above
    public void usingStyleProfileExample() throws Exception {
        TextResult result = client.translateText(
            "The artist is here.",
            null,
            "de",
            new TextTranslationOptions()
                .setStyleProfile("3f1c9d0e-7a24-4b58-9c31-8f0e5d2a1b47"));
        System.out.println(result.getText());
    }
}
```

### Translation Memories

Translation memories store and reuse previously created translations, helping to
ensure consistency and reduce effort when translating similar or repeated content.

#### Listing translation memories

Use `listTranslationMemories()` to retrieve translation memories associated
with your account. The number of translation memories returned is controlled by
`pageSize` (max 25). The method accepts optional parameters: `page` (page number
for pagination, 0-indexed) and `pageSize` (number of items per page).

```java
class Example {  // Continuing class Example from above
    public void listTranslationMemoriesExample() throws Exception {
        List<TranslationMemoryInfo> translationMemories =
                client.listTranslationMemories();
        for (TranslationMemoryInfo tm : translationMemories) {
            System.out.println(String.format("%s (%s)",
                tm.getName(), tm.getTranslationMemoryId()));
        }
    }
}
```

#### Retrieving a translation memory

Use `getTranslationMemory()` to retrieve a single translation memory. It accepts
either a translation memory ID or a `TranslationMemoryInfo` object:

```java
class Example {  // Continuing class Example from above
    public void getTranslationMemoryExample() throws Exception {
        TranslationMemoryInfo tm = client.getTranslationMemory("YOUR_TM_ID");
        System.out.println(String.format("%s: %d segments, updated %s",
            tm.getName(), tm.getSegmentCount(), tm.getUpdatedTime()));
    }
}
```

#### Listing the segments of a translation memory

`listTranslationMemorySegments()` returns one page of segments as a
`TranslationMemorySegments` object. Pagination is cursor-based: omit the page
cursor on the first call, then pass the previous response's
`getNextPageCursor()` until it is `null`. Optionally filter with `setFilterText()`
(at least 2 characters, matched against both source and target text) and
`setFilterCaseSensitive()`. Note that `getSegmentCount()` is the
translation-memory total and is not reduced by the filter. The API may omit the
segment timestamps, in which case `getCreationTime()`, `getUpdatedTime()` and
`getLastUsedTime()` are `null` on both segments and their targets.

```java
class Example {  // Continuing class Example from above
    public void listTranslationMemorySegmentsExample() throws Exception {
        String pageCursor = null;
        do {
            TranslationMemorySegments page = client.listTranslationMemorySegments(
                "YOUR_TM_ID",
                new TranslationMemorySegmentsOptions()
                    .setPageSize(50)
                    .setPageCursor(pageCursor));
            for (TranslationMemorySegment segment : page.getSegments()) {
                System.out.println(segment.getSourceText());
                for (TranslationMemoryTargetSegment target : segment.getTargets()) {
                    System.out.println(String.format("  %s: %s",
                        target.getTargetLanguage(), target.getTargetText()));
                }
            }
            pageCursor = page.getNextPageCursor();
        } while (pageCursor != null);
    }
}
```

#### Importing a translation memory

`importTranslationMemoryFromFilepath()` imports a TMX file as a new translation
memory: it creates the import job, uploads the file, and waits for processing to
finish. The returned `TranslationMemoryJob` carries the ID of the new translation
memory:

```java
class Example {  // Continuing class Example from above
    public void importTranslationMemoryExample() throws Exception {
        TranslationMemoryJob job = client.importTranslationMemoryFromFilepath(
            new File("/path/to/legal.tmx"), "Legal TM", Duration.ofSeconds(300));
        System.out.println(String.format("Created translation memory %s",
            job.getResult().getTranslationMemoryId()));
        System.out.println(String.format("Skipped segments: %s",
            job.getResult().getSkippedSegmentCount()));
    }
}
```

The optional timeout is the maximum time to wait for the import to finish; omit
it to wait indefinitely. The job status is polled every 5 seconds, and the last
wait is cut short at the timeout.

A missing input file throws `IllegalArgumentException` before any request is
sent. Any other local I/O failure, such as an unreadable file, throws
`DeepLException` with the I/O error as its cause.

`ImportTranslationMemoryOptions` groups the import options: `setDisplayName()`,
`setContentType()` (declared when the job is created and sent with the upload;
the file is uploaded as `application/xml` if unset), `setTimeout()` and
`setExtraBodyParameters()`, whose parameters are added to the import request
body last. Instead of a `File`, you can pass the file content as a `byte[]`
together with a file name:

```java
class Example {  // Continuing class Example from above
    public void importTranslationMemoryOptionsExample() throws Exception {
        ImportTranslationMemoryOptions options = new ImportTranslationMemoryOptions()
            .setDisplayName("Legal TM")
            .setContentType("application/xml")
            .setTimeout(Duration.ofSeconds(300));
        TranslationMemoryJob job = client.importTranslationMemoryFromFilepath(
            new File("/path/to/legal.tmx"), options);
        byte[] fileContent = Files.readAllBytes(Paths.get("/path/to/other.tmx"));
        TranslationMemoryJob other = client.importTranslationMemoryFromFilepath(
            fileContent, "other.tmx", options);
    }
}
```

The three steps are also available separately, for example to upload the file
yourself or to poll for progress. `createTranslationMemoryImport()` returns an
upload URL that the file must be uploaded to before processing starts, then
`getTranslationMemoryJob()` reports the status:

```java
class Example {  // Continuing class Example from above
    public void importTranslationMemoryStepsExample() throws Exception {
        File inputFile = new File("/path/to/legal.tmx");
        byte[] fileContent = Files.readAllBytes(inputFile.toPath());

        TranslationMemoryImport created = client.createTranslationMemoryImport(
            inputFile.getName(), fileContent.length,
            new CreateTranslationMemoryImportOptions().setDisplayName("Legal TM"));
        // Until the file is uploaded, the job status is AwaitingInput
        client.uploadTranslationMemoryFile(created, fileContent);

        TranslationMemoryJob job = client.waitUntilTranslationMemoryJobDone(
            created.getJobId(), Duration.ofSeconds(300));
    }
}
```

`CreateTranslationMemoryImportOptions` takes the optional `setContentType()`,
`setDisplayName()` and `setExtraBodyParameters()`; the overload taking only the
file name and size uses none of them. `uploadTranslationMemoryFile()` accepts the
`TranslationMemoryImport` or its upload URL, and an optional content type that
defaults to `application/xml`. `getTranslationMemoryJob()` and
`waitUntilTranslationMemoryJobDone()` accept a job ID or a `TranslationMemoryJob`
object.

`TranslationMemoryJob.getStatus()` returns a `TranslationMemoryJobResult.Status`,
or `null` for a status the library does not know yet; `getStatusString()` returns
the status as the API sent it. Both are also available on
`TranslationMemoryJobResult`.

Note that an import job keeps reporting `AwaitingInput` for a while after its
file has been uploaded, because the API detects the upload asynchronously.
`waitUntilTranslationMemoryJobDone()` polls through that status like any other
non-terminal one. A job whose file is never uploaded does not finish on its own,
so pass a timeout when that is a possibility.

#### Exporting a translation memory

`exportTranslationMemoryToFilepath()` exports a translation memory to a TMX file:
it creates the export job, waits for it to finish, and writes the result. It
accepts either a translation memory ID or a `TranslationMemoryInfo` object:

```java
class Example {  // Continuing class Example from above
    public void exportTranslationMemoryExample() throws Exception {
        TranslationMemoryJob job = client.exportTranslationMemoryToFilepath(
            "YOUR_TM_ID", new File("/path/to/exported.tmx"), Duration.ofSeconds(300));
    }
}
```

As with import, the timeout is optional; omit it to wait indefinitely. The output
path is checked before the export job is created: if a file already exists there
or its directory does not exist, `DeepLException` is thrown. A failure while
writing the file also throws `DeepLException`, with the I/O error as its cause.
Instead of a `File`, you can pass an `OutputStream`, which is not closed:

```java
class Example {  // Continuing class Example from above
    public void exportTranslationMemoryToStreamExample() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TranslationMemoryJob job = client.exportTranslationMemoryToFilepath(
            "YOUR_TM_ID", outputStream, Duration.ofSeconds(300));
    }
}
```

As with import, the individual steps are available separately. Note that the API
may reuse a previously completed export of an unchanged translation memory,
indicated by `isReusedExisting()`:

```java
class Example {  // Continuing class Example from above
    public void exportTranslationMemoryStepsExample() throws Exception {
        TranslationMemoryExport created =
            client.createTranslationMemoryExport("YOUR_TM_ID");
        TranslationMemoryJob job =
            client.waitUntilTranslationMemoryJobDone(created.getJobId());
        client.downloadTranslationMemoryExport(
            job, new File("/path/to/exported.tmx"));
    }
}
```

#### Deleting a translation memory

Use `deleteTranslationMemory()` to permanently remove a translation memory from
your account. It accepts either a translation memory ID or a
`TranslationMemoryInfo` object:

```java
class Example {  // Continuing class Example from above
    public void deleteTranslationMemoryExample() throws Exception {
        client.deleteTranslationMemory("YOUR_TM_ID");
    }
}
```

#### Using a translation memory in translations

You can use a translation memory for text translation by setting the translation
memory options in `TextTranslationOptions`:

```java
class Example {  // Continuing class Example from above
    public void usingTranslationMemoryExample() throws Exception {
        // Using the translation memory ID directly
        TextTranslationOptions options = new TextTranslationOptions()
                .setTranslationMemory("tm-123abc")
                .setTranslationMemoryThreshold(80);
        TextResult result = client.translateText(
            "Hello, world!", null, "de", options);
        System.out.println(result.getText());

        // Or using a TranslationMemoryInfo object from listTranslationMemories()
        List<TranslationMemoryInfo> memories = client.listTranslationMemories();
        if (!memories.isEmpty()) {
            TextTranslationOptions optionsFromInfo = new TextTranslationOptions()
                    .setTranslationMemory(memories.get(0));
            TextResult resultFromInfo = client.translateText(
                "Hello, world!", null, "de", optionsFromInfo);
            System.out.println(resultFromInfo.getText());
        }
    }
}
```

### Checking account usage

To check account usage, use the `getUsage()` function.

The returned `Usage` object contains three usage subtypes: `character`,
`document` and `teamDocument`. Depending on your account type, some usage
subtypes may be `null`. For API accounts:

- `usage.character` is non-`null`,
- `usage.document` and `usage.teamDocument` are `null`.

Each usage subtype (if valid) has `count` and `limit` properties giving the
amount used and maximum amount respectively, and the `limitReached()` method
that checks if the usage has reached the limit. The top level `Usage` object has
the `anyLimitReached()` method to check the character, document and team
document subtypes. It does not consider the API Pro subtypes described below;
call `limitReached()` on those directly, e.g. `getApiKeyCharacter().limitReached()`.

API Pro accounts get further usage information; for other accounts these
properties are `null`:

- `getApiKeyCharacter()`: the characters used by the API key making the
  request, as opposed to `getCharacter()`, which covers the whole account.
- `getSpeechToTextMinutes()` and `getSpeechToSpeechMinutes()`: speech usage in
  minutes, with `count` and `limit` like the other subtypes.
- `getStartTime()` and `getEndTime()`: the current billing period as `Date`s.
- `getProducts()`: a list of `Usage.Product`, one per product, with
  `getProductType()` (for example `"translate"`, `"write"`, `"speechToText"`;
  new values may be added), `getBillingUnit()` (`"characters"` or `"minutes"`),
  `getApiKeyUnitCount()` (units used by this API key) and
  `getAccountUnitCount()` (units used by the whole account).

```java
class Example {  // Continuing class Example from above
    public void getUsageExample() throws Exception {
        Usage usage = client.getUsage();
        if (usage.anyLimitReached()) {
            System.out.println("Translation limit reached.");
        }
        if (usage.getCharacter() != null) {
            System.out.printf("Character usage (account): %d of %d%n",
                              usage.getCharacter().getCount(),
                              usage.getCharacter().getLimit());
        }
        if (usage.getApiKeyCharacter() != null) {
            System.out.printf("Character usage (this API key): %d of %d%n",
                              usage.getApiKeyCharacter().getCount(),
                              usage.getApiKeyCharacter().getLimit());
        }
        if (usage.getDocument() != null) {
            System.out.printf("Document usage: %d of %d%n",
                              usage.getDocument().getCount(),
                              usage.getDocument().getLimit());
        }
        if (usage.getStartTime() != null && usage.getEndTime() != null) {
            System.out.printf("Billing period: %s to %s%n",
                              usage.getStartTime().toInstant(),
                              usage.getEndTime().toInstant());
        }
        if (usage.getProducts() != null) {
            for (Usage.Product product : usage.getProducts()) {
                System.out.printf("%s: %d %s (this API key), %d %s (account)%n",
                                  product.getProductType(),
                                  product.getApiKeyUnitCount(),
                                  product.getBillingUnit(),
                                  product.getAccountUnitCount(),
                                  product.getBillingUnit());
            }
        }
    }
}
```

### Listing available languages

You can request the list of languages supported by DeepL for text and documents
using the `getSourceLanguages()` and `getTargetLanguages()` functions. They both
return a list of `Language` objects.

The `name` property gives the name of the language in English, and the `code`
property gives the language code. The `supportsFormality` property only appears
for target languages, and indicates whether the target language supports the
optional `formality` parameter.

```java
class Example {  // Continuing class Example from above
    public void getLanguagesExample() throws Exception {
        List<Language> sourceLanguages = client.getSourceLanguages();
        List<Language> targetLanguages = client.getTargetLanguages();
        System.out.println("Source languages:");
        for (Language language : sourceLanguages) {
            System.out.printf("%s (%s)%n",
                              language.getName(),
                              language.getCode()); // Example: "German (de)"
        }

        System.out.println("Target languages:");
        for (Language language : targetLanguages) {
            if (language.getSupportsFormality()) {
                System.out.printf("%s (%s) supports formality%n",
                                  language.getName(),
                                  language.getCode()); // Example: "Italian (it) supports formality"

            } else {
                System.out.printf("%s (%s)%n",
                                  language.getName(),
                                  language.getCode()); // Example: "Lithuanian (lt)"
            }
        }
    }
}
```

#### Listing languages and features per resource

`DeepLClient.getLanguagesForResource()` returns the languages supported by one
API resource, as a list of `LanguageSupport` objects. Pass the resource as a
`LanguageResourceName` (`TranslateText`, `TranslateDocument`, `Glossary`,
`Voice`, `Write`, `StyleRules`, `TranslationMemory`) or as a string. By
default only stable languages and features are returned; to also return beta
languages or features relying on third-party providers, pass
`GetLanguagesForResourceOptions` with `setInclude(LanguageInclude.Beta,
LanguageInclude.External)` (or `setInclude(List<String>)`).
`getCode()` returns the language code exactly as the API sends it.

`getLanguageResources()` returns one `LanguageResource` per resource:
`getName()` gives the resource name, for example `"translate_text"`, and
`getFeatures()` maps each feature name to a `ResourceFeature`, whose
`getNeedsSourceSupport()` and `getNeedsTargetSupport()` say whether the source
or target language must support the feature (`false` if the API omits them).

```java
class Example {  // Continuing class Example from above
    public void getLanguagesForResourceExample() throws Exception {
        List<LanguageSupport> languages = client.getLanguagesForResource(
                LanguageResourceName.Write,
                new GetLanguagesForResourceOptions().setInclude(LanguageInclude.Beta));
        for (LanguageSupport language : languages) {
            System.out.printf("%s (%s) %s%n",
                              language.getName(),
                              language.getCode(),
                              language.getStatus()); // Example: "German (de) stable"
        }

        for (LanguageResource resource : client.getLanguageResources()) {
            System.out.printf("%s: %s%n",
                              resource.getName(),
                              resource.getFeatures().keySet()); // Example: "glossary: [...]"
        }
    }
}
```

#### Listing available glossary languages

Glossaries are supported for a subset of language pairs. To retrieve those
languages use the `getGlossaryLanguages()` function, which returns an array
of `GlossaryLanguagePair` objects. Use the `getSourceLanguage()` and
`getTargetLanguage()` functions to check the pair of language codes supported.

```java
class Example {  // Continuing class Example from above
    public void getGlossaryLanguagesExample() throws Exception {
        List<GlossaryLanguagePair> glossaryLanguages =
                client.getGlossaryLanguages();
        for (GlossaryLanguagePair glossaryLanguage : glossaryLanguages) {
            System.out.printf("%s to %s\n",
                              glossaryLanguage.getSourceLanguage(),
                              glossaryLanguage.getTargetLanguage());
            // Example: "en to de", "de to en", etc.
        }
    }
}
```

You can also find the list of supported glossary language pairs in the
[API documentation][api-docs-glossary-lang-list].

Note that glossaries work for all target regional-variants: a glossary for the
target language English (`"en"`) supports translations to both American English
(`"en-US"`) and British English (`"en-GB"`).

### Admin API

The Admin API lets you manage the developer keys of your organization and
retrieve usage analytics. It requires an admin API key, which you can create in
the "Admin API Keys" tab of your DeepL account. Admin keys cannot be used for
translation; developer keys cannot access the Admin API.

Create a `DeepLAdminClient` with your admin key. It uses the
`https://api.deepl.com` server by default. You can pass a
`DeepLAdminClientOptions` object to configure the timeout, retries, proxy,
headers, server URL and app info, as for `DeepLClient`, see
[Configuration](#configuration). Developer key creation is retried only after
HTTP 429, a DNS resolution failure, a refused connection or a connect timeout,
when the API cannot have processed the request, so that a lost response cannot create several keys. For the same
reason, this request is sent in streaming mode, so it does not follow HTTP
redirects: a redirect fails with a `ConnectionException`.

```java
import com.deepl.api.*;

class AdminExample {
    DeepLAdminClient adminClient;

    public AdminExample() {
        adminClient = new DeepLAdminClient("YOUR_ADMIN_KEY");
    }
}
```

#### Managing developer keys

```java
class AdminExample {  // Continuing class AdminExample from above
    public void developerKeysExample() throws Exception {
        DeveloperKey key = adminClient.createDeveloperKey(
                new CreateDeveloperKeyOptions().setLabel("Staging Team Key"));
        System.out.printf("Created %s (%s)%n", key.getLabel(), key.getKeyId());

        for (DeveloperKey developerKey : adminClient.listDeveloperKeys()) {
            System.out.printf("%s: deactivated=%b%n",
                              developerKey.getLabel(),
                              developerKey.isDeactivated());
        }

        key = adminClient.renameDeveloperKey(key, "Production Team Key");

        // Changes only the limits you set; the other limits keep their value.
        // null removes a limit (unlimited) and 0 blocks that usage.
        key = adminClient.setDeveloperKeyUsageLimits(
                key,
                new DeveloperKeyUsageLimitsUpdate()
                        .setCharacters(500000L)
                        .setSpeechToSpeechMilliseconds(null));

        // Deactivation is permanent.
        adminClient.deactivateDeveloperKey(key);
    }
}
```

`deactivateDeveloperKey()`, `renameDeveloperKey()` and
`setDeveloperKeyUsageLimits()` accept either a key ID or a `DeveloperKey`
object and return the updated `DeveloperKey`. The API validates labels and
limit values. `DeveloperKey.getLabel()` and `getUsageLimits()` return `null`
if the API sent no label or no usage limits; within `DeveloperKeyUsageLimits`
a `null` limit means unlimited.

`createDeveloperKey()` takes a `CreateDeveloperKeyOptions` (`setLabel()`), and
`deactivateDeveloperKey()`, `renameDeveloperKey()` and
`setDeveloperKeyUsageLimits()` have overloads taking a
`DeactivateDeveloperKeyOptions`, `RenameDeveloperKeyOptions` or
`SetDeveloperKeyUsageLimitsOptions`. All four options classes support
`setExtraBodyParameters()`; these parameters are added to the JSON body last
and replace a field with the same key.

#### Retrieving usage analytics

`getUsageAnalytics()` returns an `AdminUsageReport` for a date range. Dates are
UTC calendar days, and both the start and the end day are included. Pass them
as `java.time.LocalDate` objects, which are used as they are, as
`"YYYY-MM-DD"` strings (an invalid string throws an `IllegalArgumentException`),
or as `java.util.Date` or `java.time.Instant` values, of which the UTC day is
sent: `2026-10-20T00:00+02:00` is the UTC day `2026-10-19`. The API requires the end date to be after the start date
(so a report covers at least two days) and at most 366 days after it;
otherwise both analytics functions throw a `DeepLException`. Dates in the
reports (`getStartDate()`, `getEndDate()`, `getUsageDate()`) are
`java.util.Date` values at midnight UTC of the day.
Pass a `groupBy` argument to break the usage down by key
(`AdminUsageGroupBy.Key`, see `getKeyUsages()`) or by key and day
(`AdminUsageGroupBy.KeyAndDay`, see `getKeyAndDayUsages()`); a string is sent
as given, for values not yet in the enum. These lists are
empty if the report uses a different grouping. `KeyUsage.getApiKeyLabel()` is
`null` if the API sent no label.
`getGroupBy()` returns the grouping as the API sends it, for example `"key"` or
`"keyAndDay"`. Each usage breakdown has the characters used in total and per
product, `getSpeechToTextMinutes()` and `getSpeechToSpeechMinutes()`.
`getTotalUsage()` and `getSpeechToSpeechMinutes()` (a `Double`) are `null` if
the report does not include them.

```java
import com.deepl.api.*;
import java.time.LocalDate;

class AdminExample {  // Continuing class AdminExample from above
    public void usageAnalyticsExample() throws Exception {
        AdminUsageReport report = adminClient.getUsageAnalytics(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1),
                AdminUsageGroupBy.Key);
        if (report.getTotalUsage() != null) {
            System.out.printf("Total characters: %d%n",
                              report.getTotalUsage().getTotalCharacters());
        }
        for (KeyUsage keyUsage : report.getKeyUsages()) {
            System.out.printf("%s: %d%n",
                              keyUsage.getApiKeyLabel(),
                              keyUsage.getUsage().getTotalCharacters());
        }
    }
}
```

`getCustomTagUsageAnalytics()` returns a `CustomTagUsageReport` with the
usage per custom tag. It takes the same date types. Its `aggregateBy` argument
chooses between the whole period (`CustomTagUsageAggregateBy.Period`, the
default) and per-day usage (`CustomTagUsageAggregateBy.Day`); a string is sent
as given. The results are paginated: pass the `getNextPage()` value of a report
as the `page` argument to get the next page.
`CustomTagUsageBreakdown.getSpeechToSpeechMinutes()` (a `Double`) is `null` if
the report does not include it.

```java
import com.deepl.api.*;
import java.time.LocalDate;

class AdminExample {  // Continuing class AdminExample from above
    public void customTagUsageExample() throws Exception {
        Integer page = null;
        do {
            CustomTagUsageReport report = adminClient.getCustomTagUsageAnalytics(
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1),
                    CustomTagUsageAggregateBy.Period, page);
            for (CustomTagUsage tagUsage : report.getUsage()) {
                System.out.printf("%s: %d%n",
                                  tagUsage.getCustomTag(),
                                  tagUsage.getBreakdown().getTotalCharacters());
            }
            page = report.getNextPage();
        } while (page != null);
    }
}
```

### Exceptions

All module functions may raise `DeepLException` or one of its subclasses. If
invalid arguments are provided, they may raise the standard exceptions
`IllegalArgumentException`.

- `AuthorizationException`: the key is invalid or not authorized (HTTP 401 or
  403).
- `NotFoundException`: the resource does not exist (HTTP 404). The glossary
  functions throw its subclass `GlossaryNotFoundException`, which is
  deprecated: catch `NotFoundException` instead.
- `TooManyRequestsException`: HTTP 429 after all retries.
- `QuotaExceededException`: the quota is exceeded (HTTP 456).
- `ConnectionException`: no HTTP response was received, for example after a
  connection, DNS or timeout failure or a connection reset, or the connection
  failed while a downloaded document or translation memory export was being
  read.
- A malformed API response raises a `DeepLException`.

`DeepLException.getHttpStatusCode()` returns the HTTP status code of the failed
response, or `null` if there was none. `DeepLException.getShouldRetry()`
returns `true` for transient errors (HTTP 429, HTTP 5xx, connection, DNS and
timeout failures, connection resets and broken pipes); the client has already retried these up to the configured
maximum number of retries.

### Writing a Plugin

If you use this library in an application, please identify the application with
`DeepLClientOptions.setAppInfo()`, which takes the name and version of the app:

```java
class Example {  // Continuing class Example from above
    public void configurationExample() throws Exception {
        DeepLClientOptions options =
                new DeepLClientOptions().setAppInfo("my-java-translation-plugin", "1.2.3");
        DeepLClient client = new DeepLClient(authKey, options);
    }
}
```

This information is passed along when the library makes calls to the DeepL API.
Both name and version are required. Please note that setting the `User-Agent` header
via `DeepLClientOptions.setHeaders()` will override this setting, if you need to use this,
please manually identify your Application in the `User-Agent` header.

### Configuration

The `DeepLClient` constructor accepts `DeepLClientOptions` as a second argument,
for example:

```java
class Example {  // Continuing class Example from above
    public void configurationExample() throws Exception {
        DeepLClientOptions options =
                new DeepLClientOptions().setMaxRetries(1).setTimeout(Duration.ofSeconds(
                        1));
        DeepLClient client = new DeepLClient(authKey, options);
    }
}
```

The available options setters are:

- `setMaxRetries()`: maximum number of failed HTTP requests to retry, the
  default is 5. Note: only failures due to transient conditions are retried:
  HTTP 429, HTTP 5xx, and connection, DNS and timeout failures, connection
  resets and broken pipes, including a timeout while reading the response.
  Every request, including `PATCH` requests, is sent at most `maxRetries + 1`
  times.
- `setTimeout()`: connection timeout for each HTTP request.
- `setProxy()`: provide details about a proxy to use for all HTTP requests to
  DeepL, as a `java.net.Proxy`.
- `setProxyUrl()`: the same as an HTTP proxy URL such as
  `"http://proxy.example.com:3128"` (port 80 if omitted; credentials in the
  URL are not supported).
- `setHeaders()`: additional HTTP headers to attach to all requests. They may
  replace the `User-Agent` header (matched in any casing), but not the `Authorization` header, which is
  always built from the auth key.
- `setServerUrl()`: base URL for DeepL API, may be overridden for testing
  purposes. By default, the correct DeepL API (Free or Pro) is automatically
  selected.
- `setApiVersion()` (deprecated, removed in the next major version): Version
  of the DeepL API, may be overridden to use e.g. the v1 API. By default, the
  most recent API version is automatically selected. Please note: The v1 API
  does not support all features of the API, e.g. document translation or
  rephrase.

#### Anonymous platform information

By default, we send some basic information about the platform the client library is running on with each request, see [here for an explanation](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent). This data is completely anonymous and only used to improve our product, not track any individual users. If you do not wish to send this data, you can opt-out when creating your `DeepLClient` object by calling the `setSendPlatformInfo()` setter on the `DeepLClientOptions` like so:

```java
class Example {  // Continuing class Example from above
    public void configurationExample() throws Exception {
        DeepLClientOptions options =
                new DeepLClientOptions().setSendPlatformInfo(false);
        DeepLClient client = new DeepLClient(authKey, options);
    }
}
```

You can also customize the `User-Agent` header by setting its value explicitly in the `DeepLClientOptions` object via the header field. Example:

```java
class Example {  // Continuing class Example from above
    public void configurationExample() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "my custom user agent");
        DeepLClientOptions options =
                new DeepLClientOptions().setHeaders(headers);
        DeepLClient client = new DeepLClient(authKey, options);
    }
}
```

## Issues

If you experience problems using the library, or would like to request a new
feature, please open an [issue][issues].

## Development

We welcome Pull Requests, please read the
[contributing guidelines](CONTRIBUTING.md).

### Tests

There are two test suites.

The offline tests need no auth key and no network. They test the options,
request encoding, error handling, retries and polling against a local
[WireMock][wiremock] server and check each outgoing request exactly. Execute
them using `./gradlew offlineTest`. They run on Java 8 and newer.

The real-API tests send each request the library supports once to the DeepL
API, using the auth key defined by the `DEEPL_AUTH_KEY` environment variable.
Set `DEEPL_SERVER_URL` to test against another server, for example the test
API. Execute them using `./gradlew realApiTest`, with Java 11 or newer. They
create glossaries, style rules and translation memories named
`sdktest-java-...` and delete them again. The real-API suite also requires the
admin key defined by the `DEEPL_ADMIN_KEY` environment variable for the Admin
API tests, which create a developer key named `sdktest-java-...`, rename it,
set its usage limits and deactivate it again.

Be aware that the real-API tests make DeepL API requests that contribute
toward your API usage.

Each run writes a coverage report of the library to
`deepl-java/build/reports/jacoco/offlineTestCoverage` or
`deepl-java/build/reports/jacoco/realApiTestCoverage`.

[wiremock]: https://wiremock.org/

[api-docs]: https://www.deepl.com/docs-api?utm_source=github&utm_medium=github-java-readme

[api-docs-context-param]: https://www.deepl.com/docs-api/translating-text/?utm_source=github&utm_medium=github-java-readme

[api-docs-csv-format]: https://www.deepl.com/docs-api/managing-glossaries/supported-glossary-formats/?utm_source=github&utm_medium=github-java-readme

[api-docs-xml-handling]: https://www.deepl.com/docs-api/handling-xml/?utm_source=github&utm_medium=github-java-readme

[api-docs-lang-list]: https://www.deepl.com/docs-api/translating-text/?utm_source=github&utm_medium=github-java-readme

[api-docs-glossary-lang-list]: https://www.deepl.com/docs-api/managing-glossaries/?utm_source=github&utm_medium=github-java-readme

[create-account]: https://www.deepl.com/pro?utm_source=github&utm_medium=github-java-readme#developer

[issues]: https://www.github.com/DeepLcom/deepl-java/issues

[pro-account]: https://www.deepl.com/pro-account/?utm_source=github&utm_medium=github-java-readme
