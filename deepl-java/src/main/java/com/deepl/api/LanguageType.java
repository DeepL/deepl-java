// Copyright 2022 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.api;

/**
 * Enum specifying a source or target language type.
 *
 * @deprecated Only used by the deprecated {@link Translator#getLanguages(LanguageType)}; use {@link
 *     Translator#getSourceLanguages()} or {@link Translator#getTargetLanguages()} instead.
 */
@Deprecated
public enum LanguageType {
  Source,
  Target,
}
