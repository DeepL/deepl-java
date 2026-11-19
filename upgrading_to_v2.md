# Upgrading to 2.0.0

Version 2.0.0 removes everything that 1.18.0 marked as deprecated, and it changes the
error handling of several functions. You must act if your code uses the `Translator` class,
the `LanguageCode` constants, the removed option setters, the style rule `ConfiguredRules`
class or `GlossaryNotFoundException`, or if it calls a function whose checked exceptions
changed. Java 8 is still the minimum runtime. The 1.18.0 deprecation warnings point to the
replacement of each removed item.

To migrate step by step, first upgrade to 1.18.0 and fix every deprecation warning, then
upgrade to 2.0.0 and fix the remaining compile errors.

## Requirements

- Java 8 or later, as before. The library is tested on the LTS releases 8, 11, 17, 21 and 25.
- The runtime dependencies are unchanged (Apache HttpClient, Gson and the JetBrains
  annotations), and so are the Maven group and artifact (`com.deepl.api:deepl-java`).
- Recompile your code against 2.0.0. Some changed signatures are binary-incompatible, for
  example the return type of `TextResult.getBilledCharacters()` and the `Usage.Product`
  counts (see [Types and signatures](#types-and-signatures)).
- Building the library from source needs JDK 17 or later to run Gradle (it still compiles for
  Java 8). This does not affect users of the published jar.

## Quick checklist

1. Replace `Translator` and `TranslatorOptions` with `DeepLClient` and `DeepLClientOptions`:
   [Translator removed](#translator-removed).
2. Replace `LanguageCode.German` and the other constants with strings:
   [Language code constants](#language-code-constants).
3. Rename the option setters `setGlossaryId`, `setGlossaries`, `setStyleId` and
   `setTranslationMemoryId`: [Option setters](#option-setters).
4. Replace `translateDocumentStatus()` with `translateDocumentGetStatus()` and
   `getLanguages(LanguageType)` with `getSourceLanguages()` / `getTargetLanguages()`:
   [Renamed methods](#renamed-methods).
5. Catch `NotFoundException` instead of `GlossaryNotFoundException`:
   [Glossaries](#glossaries).
6. Pass style rule configurations as maps: [Style rules](#style-rules).
7. Remove calls to `setApiVersion()` and `getApiVersion()`:
   [API version](#api-version).
8. Handle the changed checked exceptions of `translateDocument()`,
   `updateMultilingualGlossaryName()` and the translation memory file functions:
   [Errors, validation and retries](#errors-validation-and-retries).
9. Use `long` for `TextResult.getBilledCharacters()` and `Long` for the other widened
   counts: [Types and signatures](#types-and-signatures).
10. Replace `Usage.anyLimitReached()` with `anyCharacterLimitReached()` and drop the document
    usage getters: [Usage](#usage).
11. Do not pass `null` or empty strings for required arguments, and catch
    `NullPointerException` where you caught `IllegalArgumentException` for `null`:
    [Null and empty arguments](#null-and-empty-arguments).

## Client and options

### Translator removed

The `Translator` class is removed. `DeepLClient` no longer extends it and declares all of its
methods with the same signatures, so only the type and the constructor change.
`TranslatorOptions` remains as the base class of `DeepLClientOptions`, but its constructor is
no longer public. The `DeepLClient(String, TranslatorOptions)` constructor is removed.

```java
// Before
Translator translator = new Translator(authKey, new TranslatorOptions().setTimeout(Duration.ofSeconds(30)));
// After
DeepLClient client = new DeepLClient(authKey, new DeepLClientOptions().setTimeout(Duration.ofSeconds(30)));
```

Subclasses of `DeepLClient` that used the protected members `jsonParser` and
`httpClientWrapper` still find them on `DeepLClient`.

### Null options and keys

`new DeepLClient(authKey, null)` and `new DeepLAdminClient(adminKey, null)` use the default
options; before they threw `NullPointerException` and `IllegalArgumentException`.
`setProxyUrl(null)` of `DeepLClientOptions` and `DeepLAdminClientOptions` clears the proxy, so
the JVM proxy settings apply; before it threw `IllegalArgumentException`. A `null` key throws
`NullPointerException` instead of `IllegalArgumentException`; an empty or blank key still
throws `IllegalArgumentException`. `DeepLClient.isFreeAccountAuthKey(null)` throws
`NullPointerException` instead of returning `false`.

`setAppInfo()` and the `AppInfo` constructor throw `NullPointerException` for a `null` app name
or version. Before, the `User-Agent` header ended in `null/null`. Pass both values.

### API version

`DeepLApiVersion`, `DeepLClientOptions.setApiVersion()`, `getApiVersion()` and the protected
`apiVersion` fields are removed. The client always uses API version 2. Delete the calls.

### Server URL

The `DeepLClient` and `DeepLAdminClient` constructors throw `IllegalArgumentException` when
the server URL is not an absolute `http` or `https` URL with a host. Before, the first request
failed with a `ConnectionException`. An empty string is rejected too: to use the default
server, do not call `setServerUrl()`.

```java
// Before: an environment variable set to an empty string ended up as an empty server URL
options.setServerUrl(System.getenv("DEEPL_SERVER_URL"));
// After
String serverUrl = System.getenv("DEEPL_SERVER_URL");
if (serverUrl != null && !serverUrl.isEmpty()) {
  options.setServerUrl(serverUrl);
}
```

A server URL that ends in `/` no longer produces request paths with a double slash.

### Extra body parameters

`BaseRequestOptions.setExtraBodyParameters()` and `getExtraBodyParameters()`, and the
overrides in every options class, use `Map<String, ?>` instead of `Map<String, String>`.
Code that passes a `Map<String, String>` compiles unchanged. Code
that assigns the result of `getExtraBodyParameters()` to a `Map<String, String>` no longer
compiles.

Request bodies of text translation, rephrasing, text correction, glossaries, spoken terms,
style profiles and rules, translation memories and the `DeepLAdminClient` are JSON (see
[JSON request bodies](#json-request-bodies)). In JSON bodies each value is sent as its JSON
type, so pass the type the API expects:

```java
// Before: strings were sent as strings and the API converted them
options.setExtraBodyParameters(Collections.singletonMap("preserve_formatting", "0"));
// After: the API rejects "0" for a boolean parameter
options.setExtraBodyParameters(Collections.singletonMap("preserve_formatting", false));
```

A `null` value is sent as JSON `null` in JSON bodies and replaces a built-in field with the
same key. To leave a parameter out of a JSON body, do not put its key into the map. In
multipart bodies (document upload) a `null` value still removes the key.

### JSON request bodies

Text translation, rephrasing, text correction, glossary creation (v2 and v3) and renaming or
updating a multilingual glossary now send JSON instead of form-encoded bodies. Texts, tag
lists, custom instructions and glossary IDs are JSON arrays; a comma-separated tag string
such as `"a,b"` is sent as the list of its tags. Document upload, status and download are
unchanged. This only matters if you inspect outgoing requests, for example in a proxy or a
test double.

## Text translation

### Option setters

The setters that 1.18.0 deprecated are removed from `TextTranslationOptions` and
`DocumentTranslationOptions`:

| Before | After |
|---|---|
| `setGlossaryId(String)` | `setGlossary(String)` or `setGlossary(IGlossary)` |
| `setGlossaries(IGlossary...)` | `setGlossaryIds(...)` |
| `setStyleId(String)` | `setStyleRule(String)` or `setStyleRule(StyleRuleInfo)` |
| `setTranslationMemoryId(String)` | `setTranslationMemory(String)` or `setTranslationMemory(TranslationMemoryInfo)` |

```java
// Before
new TextTranslationOptions().setGlossaryId(glossaryId).setStyleId(styleId);
// After
new TextTranslationOptions().setGlossary(glossaryId).setStyleRule(styleId);
```

### Option values

The setters of `TextTranslationOptions` and `DocumentTranslationOptions` that take a glossary,
style rule, style profile or translation memory object leave the option unset for `null`, like
the setters that take an ID. Before they threw `IllegalArgumentException` (`setGlossary()`:
`NullPointerException`).

`DocumentTranslationOptions.setGlossary()`, `setStyleRule()`, `setStyleProfile()` and
`setTranslationMemory()` throw `IllegalArgumentException` for an empty ID and for an object
without ID. Before, an empty ID was sent and an object without ID left the option unset.
`setGlossaryIds()` of both classes throws it for an empty entry or a glossary object without
ID, which were sent before.

A `null` entry of `setGlossaryIds()` in both classes, and of `setGlossaryIds()`,
`setStyleRuleIds()` and `setTranslationMemoryIds()` of `CreateStyleProfileOptions` and
`UpdateStyleProfileOptions`, throws `NullPointerException`. So does a `null` entry of the
lists given to `setCustomInstructions()`, `setSplittingTags()`, `setNonSplittingTags()` and
`setIgnoreTags()`; before, a `null` tag was sent as the tag `null`. Leave `null` out of the
lists.

```java
// Before: an empty ID was sent to the API
new DocumentTranslationOptions().setGlossary(glossaryIdOrEmpty);
// After: pass null to leave the option unset
new DocumentTranslationOptions().setGlossary(glossaryIdOrEmpty.isEmpty() ? null : glossaryIdOrEmpty);
```

### Language code constants

The 42 string constants of `LanguageCode` (`LanguageCode.German`, `LanguageCode.EnglishAmerican`
and so on) are removed. Pass the code as a string, or take it from `getSourceLanguages()` and
`getTargetLanguages()`. `LanguageCode.removeRegionalVariant()` and `LanguageCode.standardize()`
remain.

```java
// Before
client.translateText("Hello", LanguageCode.English, LanguageCode.German);
// After
client.translateText("Hello", "en", "de");
```

### Renamed methods

```java
// Before
List<Language> languages = translator.getLanguages(LanguageType.Target);
DocumentStatus status = translator.translateDocumentStatus(handle);
// After
List<Language> languages = client.getTargetLanguages();
DocumentStatus status = client.translateDocumentGetStatus(handle);
```

`LanguageType` is removed with `getLanguages(LanguageType)`.

## Documents

`translateDocument()` has new checked exceptions and error types. The `File` overloads no
longer declare `IOException`. All overloads, including the `InputStream` ones, now declare
`InterruptedException` and throw it unwrapped on interrupt; before, an interrupt became a
`DocumentTranslationException`.

| Situation | Before | After |
|---|---|---|
| Input file does not exist (`translateDocument(File, ...)`, `translateDocumentUpload(File, ...)`) | `FileNotFoundException` | `IllegalArgumentException`, before any request |
| Output path is occupied, input is unreadable, output cannot be created | `IOException` | `DocumentTranslationException` with the `IOException` as cause |
| Thread interrupted | `DocumentTranslationException` | `InterruptedException` |

```java
// Before
try {
  client.translateDocument(input, output, null, "de");
} catch (IOException | DocumentTranslationException e) { ... }
// After
try {
  client.translateDocument(input, output, null, "de");
} catch (DocumentTranslationException | InterruptedException e) { ... }
```

## Glossaries

`GlossaryNotFoundException` is removed. Every glossary function throws `NotFoundException`
for HTTP 404, and `getMultilingualGlossaryDictionaryEntries()` throws it when the glossary
has no dictionary for the language pair.

```java
// Before
try { client.getGlossary(id); } catch (GlossaryNotFoundException e) { ... }
// After
try { client.getGlossary(id); } catch (NotFoundException e) { ... }
```

`updateMultilingualGlossaryName()` (all overloads) now declares `InterruptedException` and
throws it unwrapped instead of a `DeepLException`.

The functions that take a `MultilingualGlossaryInfo` throw `NullPointerException` for `null`,
now with the parameter name as message. `createGlossary()`, `createGlossaryFromCsv()`,
`createMultilingualGlossaryFromCsv()` and `updateMultilingualGlossaryName()` throw
`IllegalArgumentException` for an empty name, and `createGlossary()` and
`createMultilingualGlossary()` for empty entries, before sending a request. Before, the
API rejected them with a `DeepLException`.

## Style rules

`ConfiguredRules` and the two overloads that take it are removed. Configured rules are a
`Map<String, Map<String, String>>` from category to rule to value, in the snake_case form of
the API. `StyleRuleInfo.getConfiguredRules()` returns the same map, so categories that
`ConfiguredRules` did not have are no longer dropped.

```java
// Before
String dates = rule.getConfiguredRules().getDatesAndTimes().get("calendar_era");
client.createStyleRule("Name", "en", configuredRules, instructions);
client.updateStyleRuleConfiguredRules(id, configuredRules);
// After
String dates = rule.getConfiguredRules().get("dates_and_times").get("calendar_era");
client.createStyleRule("Name", "en", configuredRulesMap, instructions, null);
client.updateStyleRuleConfiguredRules(id, configuredRulesMap, null);
```

The last argument is the options object (`CreateStyleRuleOptions` or
`UpdateStyleRuleConfiguredRulesOptions`); pass `null` for none. The `StyleRuleInfo`
constructor takes the map as well.

## Translation memories

`importTranslationMemoryFromFilepath()` and `exportTranslationMemoryToFilepath()` (all
overloads) declare `throws TranslationMemoryJobException, InterruptedException`. They no
longer declare `IOException` or the base `DeepLException`, and a `catch (IOException)` around
them does not compile.

`TranslationMemoryJobException` extends `DeepLException`. It wraps every failure: API errors,
a failed or expired job, a timeout and local file errors. The underlying error is the cause.
`getJobId()` returns the job ID once the job was created, and `getJob()` returns the last
fetched `TranslationMemoryJob`, or `null` before the first status request.
`getHttpStatusCode()` and `getShouldRetry()` come from the cause. An empty ID, or a
`TranslationMemoryInfo` without ID, passed to `exportTranslationMemoryToFilepath()` is wrapped
as well, without job ID. Not wrapped: a missing input file still throws
`IllegalArgumentException`, and a `null` translation memory ID, `TranslationMemoryInfo`, file
name, input file or output file throws `NullPointerException` before any request.

```java
// Before
try {
  client.importTranslationMemoryFromFilepath(file, "My TM");
} catch (AuthorizationException e) { ... } catch (IOException e) { ... }
// After
try {
  client.importTranslationMemoryFromFilepath(file, "My TM");
} catch (TranslationMemoryJobException e) {
  Throwable cause = e.getCause(); // for example AuthorizationException or IOException
}
```

Other changes:

- `TranslationMemoryJob.done()` and `TranslationMemoryJobResult.done()` are `true` only for a
  completed job, like `DocumentStatus.done()`. Before they were also `true` for a failed or
  expired job. Check `ok()` for failure.
- `TranslationMemoryJobResult.Status.Downloaded` is removed; the status of a finished export
  is `Completed`.
- `TranslationMemoryJobResult.getSkippedSegmentCount()` returns `Long` instead of `Integer`
  (see [Types and signatures](#types-and-signatures)).

## Usage

- `Usage.anyLimitReached()` is replaced by `anyCharacterLimitReached()`. It checks the
  account and API key character limits only. Call `limitReached()` on the speech details
  to check the speech limits.
- `Usage.getDocument()`, `Usage.getTeamDocument()` and the matching constructor parameters
  are removed, and `toString()` no longer prints document lines. Responses that still contain
  these fields parse and the fields are ignored.
- The public `Usage` constructors are now `Usage(Detail character)` and a 7-argument
  constructor `Usage(character, apiKeyCharacter, speechToTextMinutes, speechToSpeechMinutes,
  products, startTime, endTime)`. Delete the `document` and `teamDocument` arguments.
- `Usage.Product.getApiKeyUnitCount()` and `getAccountUnitCount()` return `Long`, which is
  `null` when the API omits the count, instead of a `long` that was `0`.

```java
// Before
if (usage.anyLimitReached()) { ... }
// After
if (usage.anyCharacterLimitReached()) { ... }
```

## Null and empty arguments

Every `DeepLClient` and `DeepLAdminClient` function checks its arguments before any request. A
`null` required argument, or a `null` entry of a list argument, throws `NullPointerException`
with the parameter name as message. An empty string where a value is required throws
`IllegalArgumentException`. Changes that need a code change:

| Situation | Before | After |
|---|---|---|
| `null` resource ID, name, language code or other required string of the multilingual glossary, Spoken Terms, style rule, style profile, translation memory and `getLanguagesForResource()` functions | `IllegalArgumentException` | `NullPointerException` |
| `null` translation memory, translation memory job or import, Spoken Terms, style profile, style rule or custom instruction object (an object without ID still throws `IllegalArgumentException`) | `IllegalArgumentException` | `NullPointerException` |
| `null` glossary ID of `getGlossary()`, `getGlossaryEntries()`, `deleteGlossary()` | request to `/v2/glossaries/null` | `NullPointerException` |
| Empty dictionary language code of `createMultilingualGlossary()`, `createMultilingualGlossaryFromCsv()`, `updateMultilingualGlossary()` | sent to the API | `IllegalArgumentException` |
| Empty language code of a Spoken Terms term list, `getSpokenTermsEntries()` or `deleteSpokenTermsList()` | sent to the API | `IllegalArgumentException` |
| Empty source language of a style rule custom instruction (`createStyleRule()`, `createStyleRuleCustomInstruction()`, `updateStyleRuleCustomInstruction()`; `null` leaves it unset) | sent to the API | `IllegalArgumentException` |
| `null` name of `createSpokenTerms()` or `createStyleProfile()`; `null` custom instruction, label or prompt of `createStyleRule()` | left out of the request | `NullPointerException` |
| `null` configured rules of `updateStyleRuleConfiguredRules()` | `IllegalArgumentException` | `NullPointerException` |
| `null` file content, input file, output file or output stream of `uploadTranslationMemoryFile()`, `importTranslationMemoryFromFilepath()`, `exportTranslationMemoryToFilepath()`, `downloadTranslationMemoryExport()` | `IllegalArgumentException` | `NullPointerException` |
| `null` target language, input stream, file name or output stream of `translateDocument()` | `DocumentTranslationException` | `NullPointerException` |
| `null` key ID, `DeveloperKey` or date of `DeepLAdminClient` | `IllegalArgumentException` | `NullPointerException` |
| `null` label of `renameDeveloperKey()`, `null` limits of `setDeveloperKeyUsageLimits()` | sent as JSON `null`, or sent without limits | `NullPointerException` |
| `null` document ID or key of the `DocumentHandle` constructor | the document functions threw `NullPointerException` | the constructor throws `NullPointerException` |
| `null` entries of the `SpokenTermsEntries` constructor | sent as an empty term list | `NullPointerException`; pass an empty list |

`GetLanguagesForResourceOptions.setInclude()` throws `NullPointerException` for a `null`
include value, and `setInclude((LanguageInclude[]) null)` leaves the option unset.

```java
// Before
try {
  client.getStyleProfile(styleProfileId);
} catch (IllegalArgumentException e) { ... } // also thrown for a null ID
// After
Objects.requireNonNull(styleProfileId, "styleProfileId"); // or check before the call
client.getStyleProfile(styleProfileId);
```

## Errors, validation and retries

- Requests that create a resource are retried only when they were not sent: after HTTP 429
  or a failure while connecting, such as a DNS failure, a refused connection, an unreachable
  host or network or a connect timeout; not after a TLS or proxy failure. These are `createGlossary()`,
  `createGlossaryFromCsv()`, `createMultilingualGlossary()`,
  `createMultilingualGlossaryFromCsv()`, `createSpokenTerms()`, `createStyleProfile()`,
  `createStyleRule()`, `createStyleRuleCustomInstruction()`,
  `createTranslationMemoryImport()` (and so `importTranslationMemoryFromFilepath()`),
  `createTranslationMemoryExport()`, `DeepLAdminClient.createDeveloperKey()` and the document
  upload. After HTTP 5xx, a read timeout or a connection reset, the error is raised without a
  retry and `DeepLException.getShouldRetry()` is `false`, because the resource may exist.
  Check before you retry such a request yourself. Except for
  `createTranslationMemoryExport()`, which has no body, these requests are streamed and do
  not follow HTTP redirects: a redirect fails with a `ConnectionException`.
- A failure while connecting, before the request is written, is retried for every request
  whatever its exception class, for example `Network is unreachable` or a reset during the TLS
  handshake. This includes `DeepLAdminClient.createDeveloperKey()`, which before raised such a
  failure at once with `getShouldRetry()` `false`. TLS failures and a proxy that refuses the
  tunnel are not retried.
- A non-2xx answer from an HTTP proxy to `CONNECT` for a `PATCH` request (the update
  functions) or a `POST` without a body (`createTranslationMemoryExport()` and the other
  bodiless creates) throws a `ConnectionException` with `getShouldRetry()` `false` and is not
  retried. Before, it was treated as the API's response: retried for 429 and 5xx and mapped by
  its status, for example to `AuthorizationException` for 403.
- The `checkResponse` method that subclasses of `DeepLClient` can override lost its
  `usingGlossary` parameter: `checkResponse(HttpResponse, boolean)`.
- `updateMultilingualGlossaryName()`, `translateDocument()` and the translation memory file
  functions: see [Glossaries](#glossaries), [Documents](#documents) and
  [Translation memories](#translation-memories).

## Types and signatures

| Member | Before | After |
|---|---|---|
| `TextResult.getBilledCharacters()` and constructors | `int` | `long` |
| `TranslationMemoryJobResult.getSkippedSegmentCount()` and constructors | `Integer` | `Long` |
| `Usage.Product.getApiKeyUnitCount()`, `getAccountUnitCount()` and constructor | `long` | `Long`, `null` when the API omits it |
| `StyleRuleInfo.getConfiguredRules()` and constructor | `ConfiguredRules` | `Map<String, Map<String, String>>` |
| `setExtraBodyParameters()`, `getExtraBodyParameters()` | `Map<String, String>` | `Map<String, ?>` |

```java
// Before
int billed = result.getBilledCharacters();
// After
long billed = result.getBilledCharacters();
```

## Internal classes

These members were public or protected without being meant for callers. They changed or are
removed.

- `DeepLClient.createWriteHttpParams()`, `createGlossaryHttpParams()` and
  `createGlossaryDictionariesHttpParams()` (protected, static): removed, because the bodies
  they built are JSON now.
- `DeepLClient.checkResponse(HttpResponse, boolean, boolean)`: now
  `checkResponse(HttpResponse, boolean)`.
- `TranslatorOptions()`: constructor is protected.
- `com.deepl.api.http.HttpResponse` and `HttpResponseStream` gained the public
  `HttpResponse(int, String, boolean)` constructor, `isRetriedOnlyIfUnsent()` and
  `toStringResponse(boolean)`; they are internal and not meant for callers.
- `Usage` constructors and `Usage.Product` constructor: see [Usage](#usage).
