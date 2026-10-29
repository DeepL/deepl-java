# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Added `DeepLException.getHttpStatusCode()`, the HTTP status code of the
  failed response (`null` if there was none), and
  `DeepLException.getShouldRetry()`, which is `true` for HTTP 429, HTTP 5xx and
  connection, DNS and timeout failures.
- Added `setProxyUrl(String)` to `DeepLClientOptions` (`TranslatorOptions`) and
  `DeepLAdminClientOptions`, which takes an HTTP proxy URL such as
  `http://proxy.example.com:3128`.
- `TextTranslationOptions.setExtraBodyParameters()` and
  `DocumentTranslationOptions.setExtraBodyParameters()` now return the options
  class, so further setters can be chained after them.
- Added the `setStyleProfile()` option to `TextTranslationOptions` and
  `DocumentTranslationOptions`, sent as `style_profile_id` on text and
  document translation. It accepts a style profile ID or a `StyleProfile`
  object.
- Added the `TagHandlingMode`, `TagHandlingVersion` and `ModelType` enums and
  `TextTranslationOptions.setTagHandling()`, `setTagHandlingVersion()` and
  `setModelType()` overloads that take them; the string setters remain.
- Added `TextTranslationOptions.setFormality(String)` and
  `setSentenceSplittingMode(String)`, which send the string as given, for
  values not yet in `Formality` or `SentenceSplittingMode`.
- Added `TextTranslationOptions.setSplittingTags(String)`,
  `setNonSplittingTags(String)` and `setIgnoreTags(String)`, which send a
  comma-separated tag list as given.
- Added `TextTranslationOptions.setStyleRule(String)` and
  `setTranslationMemory(String)`, taking the style rule or translation memory
  ID.
- Added `TextTranslationOptions.setGlossaryIds(IGlossary, IGlossary...)`,
  taking glossary objects.
- Added `Translator.translateDocumentGetStatus()`, which returns the status of
  a document translation.
- Added a `translateDocumentWaitUntilDone(DocumentHandle, Duration)` overload
  that throws a `DeepLException` when the timeout expires, and
  `DocumentTranslationOptions.setTimeout(Duration)`, which limits how long
  `translateDocument()` waits for the translation; on expiry it throws a
  `DocumentTranslationException` with the document handle.
- Added `DocumentTranslationOptions.setFilename()`, the file name sent with the
  upload in place of the input file's name.
- Added `DocumentTranslationOptions.setFormality(String)`, which sends the
  string as given.
- Added `DocumentTranslationOptions.setStyleRule(String)`,
  `setTranslationMemory(String)` and
  `setGlossaryIds(IGlossary, IGlossary...)`.
- Added `DocumentStatus.getStatusString()`, the status as the API sent it,
  including values not in `DocumentStatus.StatusCode`.
- Added `TextResult.getTagHandlingVersion()`, the tag handling version the API
  used (`tag_handling_version`); `null` when the response does not contain it.
- Added style profile support to `DeepLClient` (`/v3/style_profiles`):
  `createStyleProfile()`, `listStyleProfiles()`, `getStyleProfile()`,
  `updateStyleProfile()` and `deleteStyleProfile()`, with the new classes
  `StyleProfile` and `StyleProfileOptions`. Options left `null` are not sent;
  an empty string clears the description or industry and an empty list
  unlinks all components of that kind.
- Added Spoken Terms support to `DeepLClient` (`/v3/spoken-terms`):
  `createSpokenTerms()`, `listSpokenTerms()`, `getSpokenTerms()`,
  `updateSpokenTerms()`, `getSpokenTermsEntries()`, `replaceSpokenTermsList()`,
  `deleteSpokenTerms()` and `deleteSpokenTermsList()`, with the new classes
  `SpokenTerms`, `SpokenTermsList` and `SpokenTermsEntries`. Terms are passed
  and returned as a list of strings and are checked like glossary terms, so a
  term containing a newline throws an `IllegalArgumentException`.
- Added the `DeepLAdminClient` class for the Admin API. It must be created with
  an admin API key and supports managing developer keys
  (`createDeveloperKey()`, `getDeveloperKeys()`, `deactivateDeveloperKey()`,
  `renameDeveloperKey()`, `setDeveloperKeyUsageLimits()`) and retrieving usage
  analytics (`getUsageAnalytics()`, `getCustomTagUsageAnalytics()`, with
  `AdminUsageAnalyticsOptions` and `CustomTagUsageAnalyticsOptions`, which take
  the `AdminUsageGroupBy` and `CustomTagUsageAggregateBy` enums; the client is
  configured with `DeepLAdminClientOptions`). Responses
  are `DeveloperKey`, `DeveloperKeyUsageLimits`, `AdminUsageReport`,
  `AdminUsageBreakdown` (including `getSpeechToSpeechMinutes()`), `KeyUsage`,
  `CustomTagUsageReport`, `CustomTagUsage` and `CustomTagUsageBreakdown`.
  The analytics functions take `LocalDate` or `"YYYY-MM-DD"` strings, which are
  UTC calendar days; both the start and the end day are included.
  `setDeveloperKeyUsageLimits()` takes a `DeveloperKeyUsageLimitsUpdate` and
  changes only the limits set in it, including the speech-to-speech limit.
- Added the API Pro fields of `GET /v2/usage` to `Usage`:
  `getApiKeyCharacter()` (per-key character usage), `getSpeechToTextMinutes()`,
  `getSpeechToSpeechMinutes()`, `getStartTime()` and `getEndTime()` (billing
  period) and `getProducts()` (per-product usage as `Usage.Product`). They are
  `null` when the response does not contain them, for example for Free accounts.
- Added `DocumentTranslationOptions.setEnableWatermark()` to add a
  "Translated by DeepL" watermark to translated `docx` and `pdf` documents.
  By default the option is not sent.
- Added `DocumentTranslationOptions.setOutputFormat()` to request the file
  format of the translated document, for example `docx`. By default the
  translated document has the same format as the input document.
- Added `LanguageCode` constants for the target language variants `de-CH`,
  `de-DE`, `es-ES`, `es-419`, `fr-CA`, `fr-FR`, `zh-Hans` and `zh-Hant`.
- Added `DeepLClient.getLanguagesForResource()`, which calls
  `GET /v3/languages` for a given resource (`translate_text`, `glossary`,
  `write`, ...) with optional `include` values (`beta`, `external`) and
  returns `LanguageSupport` objects with per-language feature support.
- Added `DeepLClient.getLanguageResources()`, which calls
  `GET /v3/languages/resources` and returns `LanguageResource` objects
  describing each resource's features and whether they need source and/or
  target language support.
- Added `correctText()` to the `DeepLClient` class, which corrects grammar, spelling
  and punctuation using the `/v2/write/correct` endpoint. It accepts a single
  text or a list of texts and an optional target language.

### Changed
- The bare target languages `en` and `pt` are no longer rejected
  client-side in text translation, document translation and rephrasing. The
  API translates them to `en-US` and `pt-PT` respectively.
- `getSourceLanguages()`, `getTargetLanguages()`, `getLanguages()` and
  `getGlossaryLanguages()` now use `GET /v3/languages` instead of the
  deprecated v2 endpoints. The returned lists may contain additional
  languages, including the bare `en` and `pt` as target languages.

### Deprecated
- Deprecated `GlossaryNotFoundException`. Catch its superclass
  `NotFoundException` instead, which is thrown for HTTP 404 on every endpoint.
  It will be removed in the next major version.
- Deprecated `DeepLClientOptions.setApiVersion()` and `getApiVersion()`; the
  API version will no longer be configurable in the next major version.
- Deprecated the language code constants in `LanguageCode` (for example
  `LanguageCode.German`). Pass the language code as a string instead, for
  example `"de"`, or use `getSourceLanguages()` / `getTargetLanguages()`.
  The constants will be removed in the next major version. The class itself
  and its helper methods are not deprecated.
- Deprecated `TextTranslationOptions.setGlossaryId()`, `setGlossaries()`,
  `setStyleId()` and `setTranslationMemoryId()`. Use `setGlossary()`,
  `setGlossaryIds()`, `setStyleRule()` and `setTranslationMemory()` instead.
  They will be removed in the next major version.
- Deprecated `Translator.translateDocumentStatus()`; use
  `translateDocumentGetStatus()` instead. Deprecated
  `DocumentTranslationOptions.setGlossaryId()`, `setGlossaries()`,
  `setStyleId()` and `setTranslationMemoryId()`; use `setGlossary()`,
  `setGlossaryIds()`, `setStyleRule()` and `setTranslationMemory()` instead.
  They will be removed in the next major version.

### Fixed
- A document status the library does not know yet no longer stops
  `translateDocumentWaitUntilDone()` and `translateDocument()` with
  "Unknown error": it counts as in progress and polling continues.
  `DocumentStatus.getStatus()` returns `null` for it.
- `translateDocumentGetStatus()` and `translateDocumentDownload()` now
  URL-encode the document ID in the request path.
- `translateDocumentUpload()` and `translateDocument()` with an input stream
  now post to `/v2/document` instead of `/v2/document/`.
- Document translation now sends the source and target language codes with
  only the language part lower-cased, for example `zh-Hans`; it sent
  `zh-HANS` before.
- `translateText()` no longer fails when a translation in the response has no
  `billed_characters` (`getBilledCharacters()` returns 0) or a `null`
  `model_type_used` (`getModelTypeUsed()` returns `null`).
- HTTP 401 now throws `AuthorizationException`, like HTTP 403, instead of a
  `DeepLException` with "Unknown error".
- Connection failures (refused connections, unreachable hosts) and failed DNS
  lookups are now retried like timeouts, in all request types including
  `PATCH`.
- Developer key creation is now also retried after a refused connection or a
  connect timeout, in addition to HTTP 429 and DNS failures, as the request was
  not sent in these cases. A timeout or connection reset after the request was
  sent is still not retried.
- A failure while reading the response body, for example a read timeout, now
  throws a `ConnectionException` and is retried. It threw a non-retried
  `DeepLException` before.
- A whitespace-only auth key is now rejected by the `DeepLClient` and
  `Translator` constructors with an `IllegalArgumentException`.
- An `Authorization` header passed with `setHeaders()` no longer replaces the
  `DeepL-Auth-Key` authorization built from the auth key, in all clients.
- A malformed or empty API response now throws a `DeepLException` instead of
  a `JsonSyntaxException` or `NullPointerException`.
- Fixed the doc comments of `LanguageCode.English` and
  `LanguageCode.Portuguese`, which claimed they may only be used as source
  languages.
- `rephraseText()` no longer throws a `NullPointerException` when the target
  language is `null`.
- `getLanguages()` no longer sends a `POST` request to `/v2/languages`; it
  now sends `GET`.
- `translateDocument()` with file arguments and `translateDocumentDownload()`
  with an output file no longer delete an existing file at the output path
  when they refuse to overwrite it and throw an `IOException`.
- `PATCH` requests (`updateMultilingualGlossaryName()`,
  `updateMultilingualGlossaryDictionary()` and `updateStyleRuleName()`) now use
  the JVM proxy settings (`http.proxyHost`, `https.proxyHost` and others) like
  all other requests. They ignored them before.

## [1.17.0] - 2026-08-11
### Added
- Added support for using multiple glossaries in text and document translation
  via `setGlossaryIds()`/`setGlossaries()` (up to 5 glossaries) in
  `TextTranslationOptions` and `DocumentTranslationOptions`.
- Added support for style rules in document translation via
  `setStyleRule()`/`setStyleId()` in `DocumentTranslationOptions`.
- Added support for translation memories in document translation via
  `setTranslationMemory()`/`setTranslationMemoryId()`/`setTranslationMemoryThreshold()`
  in `DocumentTranslationOptions`.
- Added support for the translation memory management endpoints in the
  `DeepLClient` class: `getTranslationMemory()`,
  `listTranslationMemorySegments()`, `deleteTranslationMemory()`,
  `createTranslationMemoryImport()`, `uploadTranslationMemoryFile()`,
  `createTranslationMemoryExport()`, `getTranslationMemoryJob()`,
  `waitUntilTranslationMemoryJobDone()`, `downloadTranslationMemoryExport()`,
  `importTranslationMemoryFromFilepath()` and
  `exportTranslationMemoryToFilepath()`.
- Added `creationTime` and `updatedTime` to `TranslationMemoryInfo`.
- Added optional timeouts to `waitUntilTranslationMemoryJobDone()`,
  `importTranslationMemoryFromFilepath()` and
  `exportTranslationMemoryToFilepath()`.

### Fixed
- Fixed `waitUntilTranslationMemoryJobDone()` throwing when a translation memory
  job reports `AwaitingInput`. An import job keeps reporting that status for a
  while after its file has been uploaded, because the API detects the upload
  asynchronously, so it is now polled through like any other non-terminal
  status.
- Fixed incorrect example texts in the test suite for several languages (Danish,
  Indonesian, Japanese, Portuguese, and Russian) to match the mock server.

## [1.16.0] - 2026-04-09
### Added
- Added support for translation memories in text translation via
  `setTranslationMemoryId()` and `setTranslationMemoryThreshold()` in
  `TextTranslationOptions`.
- Added `listTranslationMemories()` method to the `DeepLClient` class for
  listing available translation memories.

### Fixed
- Fixed GitLab release job using wrong image by explicitly setting `image: registry.gitlab.com/gitlab-org/release-cli:latest`.

## [1.15.0] - 2026-03-26
### Added
- Added JSON request body support to `HttpContent` and `HttpClientWrapper`, enabling
  JSON-encoded API calls via `sendJsonRequestWithBackoff()` and `sendJsonPatchRequestWithBackoff()`.
- Added support for style rules CRUD endpoints in the `DeepLClient` class:
  `createStyleRule()`, `getStyleRule()`, `updateStyleRuleName()`,
  `updateStyleRuleConfiguredRules()`, and `deleteStyleRule()`.
- Added support for style rule custom instruction CRUD endpoints in the
  `DeepLClient` class: `createStyleRuleCustomInstruction()`,
  `getStyleRuleCustomInstruction()`, `updateStyleRuleCustomInstruction()`,
  and `deleteStyleRuleCustomInstruction()`.
  Please refer to the README for usage instructions.

### Changed
- Updated formality tests to accept either formal or informal output when using default formality,
  since the default formality is automatic.
- Updated `testModelType()` to just check if the `model_type_used` is non-null if the `model_type` is specified in the request
- Updated `testMixedDirectionText()` to add a missing `</p>` tag
- Improved `NotFoundException` error message by removing the misleading "check server_url" suggestion.

### Fixed
- Fixed PATCH requests (`sendPatchRequestWithBackoff`) not retrying on 429/5xx errors despite
  the method name promising backoff behavior. PATCH requests now use the same retry logic as all
  other HTTP methods.

## [1.14.0] - 2025-12-10
### Added
- Added `setTagHandlingVersion()` method to `TextTranslationOptions` to specify which version of the tag handling algorithm to use. Options are `v1` and `v2`.

## [1.13.0] - 2025-12-03
### Added
- Added `setCustomInstructions()` method to `TextTranslationOptions` to
  customize translation behavior with up to 10 instructions (max 300 characters
  each). Only supported for target languages: `de`, `en`, `es`, `fr`, `it`,
  `ja`, `ko`, `zh` and their variants.

  Note: using the custom instructions parameter will use `quality_optimized`
  model type as the default. Requests combining custom instructions and the
  `latency_optimized` model type will be rejected.

## [1.12.0] - 2025-11-12
### Added
- Added support for the `GET /v3/style_rules` endpoint in the client library, the
  implementation can be found in the `DeepLClient` class. Please refer to the
  README for usage instructions
- Added `styleId` option to `translateText()` which allows text translation with
  style rules.

## [1.11.0] - 2025-11-04
### Added
- Added `extraRequestParameters` option to text and document translation methods to pass arbitrary parameters in the request body. This can be used to access beta features or override built-in parameters (such as `target_lang`, `source_lang`, etc.).

## [1.10.3] - 2025-08-22
### Security
- Updated `org.apache.httpcomponents:httpclient` to 4.5.14 due to [CVE-2020-13956](https://nvd.nist.gov/vuln/detail/CVE-2020-13956).
   * Thanks to [warm-tune](https://github.com/warm-tune) for reporting in [#72](https://github.com/DeepLcom/deepl-java/issues/72).

## [1.10.2] - 2025-07-11
### Changed
- Migrate to Sonatype Portal OSSRH Staging API due to legacy OSSRH being sunsetted.
- Whitespace surrounding auth key is now stripped.
  * Thanks to [timazhum](https://github.com/timazhum) for the fix in [#64](https://github.com/DeepLcom/deepl-java/pull/69).

## [1.10.1] - 2025-06-18
### Fixed
- Fixed `DeepLClient::deleteMultilingualGlossary(String glossaryId)` being package private, made it public instead.
  * Thanks to [MTSxoff](https://github.com/MTSxoff) for the report in [#68](https://github.com/DeepLcom/deepl-java/issues/68) and the fix in [#69](https://github.com/DeepLcom/deepl-java/pull/69).

## [1.10.0] - 2025-04-30
### Added
- Added support for the /v3 Multilingual Glossary APIs in the client library
  while providing backwards compatability for the previous /v2 Glossary
  endpoints. Please refer to the README or
  [upgrading_to_multilingual_glossaries.md](upgrading_to_multilingual_glossaries.md)
  for usage instructions.
- Added Ukrainian language code

## [1.9.0] - 2025-02-21
### Added
- Allow specifying the API version to use. This is mostly for users who have an
  API subscription that includes an API key for CAT tool usage, who need to use
  the v1 API.

## [1.8.1] - 2025-02-07
### Fixed
- Added a constructor for `DeepLClient` that only takes an `authKey`, to fix the
  README example and be in line with `Translator`.
- Un-deprecated the `Translator` and `TranslatorOptions` class and moved it to
  their constructors. The functionality in them continues to work and be supported,
  user code should just use `DeepLClient` and `DeepLClientOptions`.

## [1.8.0] - 2025-01-17
### Added
- Added support for the Write API in the client library, the implementation
  can be found in the `DeepLClient` class. Please refer to the README for usage
  instructions.

### Changed
- The main functionality of the library is now also exposed via the `DeepLClient`
  class. Please change your code to use this over the `Translator` class whenever
  convenient.

## [1.7.0] - 2024-11-15
### Added
- Added `modelType` option to `translateText()` to use models with higher
  translation quality (available for some language pairs), or better latency.
  Options are `'quality_optimized'`, `'latency_optimized'`, and  `'prefer_quality_optimized'`
- Added the `modelTypeUsed` field to `translateText()` response, that
  indicates the translation model used when the `modelType` option is
  specified.

## [1.6.0] - 2024-09-17
### Added
- Added `getBilledCharacters()` to text translation response.

## [1.5.1] - 2024-09-05
### Fixed
- Fixed parsing for usage count and limit for large values.
  * Thanks to [lubo-dev](https://github.com/lubo-dev) in [#45](https://github.com/DeepLcom/deepl-java/pull/45).

## [1.5.0] - 2024-04-10
### Added
- New language available: Arabic (MSA) (`'ar'`). Add language code constants and tests.

  Note: older library versions also support the new language, this update only
  adds new code constants.

### Fixed
- Change document upload to use the path `/v2/document` instead of `/v2/document/` (no trailing `/`).
  Both paths will continue to work in the v2 version of the API, but `/v2/document` is the intended one.

## [1.4.0] - 2023-11-03
### Added
- Add optional `context` parameter for text translation, that specifies
  additional context to influence translations, that is not translated itself.

### Fixed
- Remove unused `commons-math` dependency

## [1.3.0] - 2023-06-09
### Fixed
- Changed document translation to poll the server every 5 seconds. This should greatly reduce observed document translation processing time.
- Fix getUsage request to be a HTTP GET request, not POST.

## [1.2.0] - 2023-03-22
### Added
- Script to check our source code for license headers and a step for them in the CI.
- Added system and java version information to the user-agent string that is sent with API calls, along with an opt-out.
- Added method for applications that use this library to identify themselves in API requests they make.

## [1.1.0] - 2023-01-26
### Added
- Add example maven project using this library.
- New languages available: Korean (`'ko'`) and Norwegian (bokmål) (`'nb'`). Add
  language code constants and tests.

  Note: older library versions also support the new languages, this update only
  adds new code constants.

### Fixed
- Send Formality options in API requests even if it is default.

## [1.0.1] - 2023-01-02
### Fixed
- Always send SentenceSplittingMode option in requests.
  * [#3](https://github.com/DeepLcom/deepl-java/issues/3) thanks to
    [nicStuff](https://github.com/nicStuff)

## [1.0.0] - 2022-12-15
### Added
- Add support for glossary management functions.

### Changed
- `parsing.ErrorResponse` fields `message` and `detail` are now private,
  encapsulated with getters.

## [0.2.1] - 2022-10-19
### Fixed
- Handle case where HTTP response is not valid JSON.

## [0.2.0] - 2022-09-26
### Added
- Add new `Formality` options: `PreferLess` and `PreferMore`.

### Changed
- Requests resulting in `503 Service Unavailable` errors are now retried.
  Attempting to download a document before translation is completed will now
  wait and retry (up to 5 times by default), rather than throwing an exception.

### Fixed
- Use `Locale.ENGLISH` when changing string case.
  * Thanks to [seratch](https://github.com/seratch).
- Avoid cases in `HttpContent` and `StreamUtils` where temporary objects might
  not be closed.
  * Thanks to [seratch](https://github.com/seratch).

## [0.1.3] - 2022-09-09
### Fixed
- Fixed examples in readme.
- `Usage.Detail` `count` and `limit` properties type changed from `int` to `long`.

## [0.1.2] - 2022-09-08
### Fixed
- Fix publishing to Maven Central by including sourcesJar and javadocJar.

## [0.1.1] - 2022-09-08
### Fixed
- Fix CI publishing step.

## [0.1.0] - 2022-09-08
Initial version.

[Unreleased]: https://github.com/DeepL/deepl-java/compare/v1.17.0...HEAD
[1.17.0]: https://github.com/DeepL/deepl-java/compare/v1.16.0...v1.17.0
[1.16.0]: https://github.com/DeepL/deepl-java/compare/v1.15.0...v1.16.0
[1.15.0]: https://github.com/DeepL/deepl-java/compare/v1.14.0...v1.15.0
[1.14.0]: https://github.com/DeepL/deepl-java/compare/v1.13.0...v1.14.0
[1.13.0]: https://github.com/DeepL/deepl-java/compare/v1.12.0...v1.13.0
[1.12.0]: https://github.com/DeepL/deepl-java/compare/v1.11.0...v1.12.0
[1.11.0]: https://github.com/DeepL/deepl-java/compare/v1.10.3...v1.11.0
[1.10.3]: https://github.com/DeepL/deepl-java/compare/v1.10.2...v1.10.3
[1.10.2]: https://github.com/DeepL/deepl-java/compare/v1.10.1...v1.10.2
[1.10.1]: https://github.com/DeepL/deepl-java/compare/v1.10.0...v1.10.1
[1.10.0]: https://github.com/DeepL/deepl-java/compare/v1.9.0...v1.10.0
[1.9.0]: https://github.com/DeepL/deepl-java/compare/v1.8.1...v1.9.0
[1.8.1]: https://github.com/DeepL/deepl-java/compare/v1.8.0...v1.8.1
[1.8.0]: https://github.com/DeepL/deepl-java/compare/v1.7.0...v1.8.0
[1.7.0]: https://github.com/DeepL/deepl-java/compare/v1.6.0...v1.7.0
[1.6.0]: https://github.com/DeepL/deepl-java/compare/v1.5.1...v1.6.0
[1.5.1]: https://github.com/DeepL/deepl-java/compare/v1.5.0...v1.5.1
[1.5.0]: https://github.com/DeepL/deepl-java/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/DeepL/deepl-java/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/DeepL/deepl-java/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/DeepL/deepl-java/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/DeepL/deepl-java/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/DeepL/deepl-java/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/DeepL/deepl-java/compare/v0.2.1...v1.0.0
[0.2.1]: https://github.com/DeepL/deepl-java/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/DeepL/deepl-java/compare/v0.1.3...v0.2.0
[0.1.3]: https://github.com/DeepL/deepl-java/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/DeepL/deepl-java/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/DeepL/deepl-java/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/DeepL/deepl-java/releases/tag/v0.1.0
