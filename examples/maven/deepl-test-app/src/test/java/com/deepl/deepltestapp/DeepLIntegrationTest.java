// Copyright 2023 DeepL SE (https://www.deepl.com)
// Use of this source code is governed by an MIT
// license that can be found in the LICENSE file.
package com.deepl.deepltestapp;

import com.deepl.api.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Internal DeepL Integration Test
 */
public class DeepLIntegrationTest {

    /**
     * Runs the hello world example. Requires a DeepL auth key via the DEEPL_AUTH_KEY
     * environment variable.
     */
    @Test
    void testApp() throws InterruptedException, DeepLException {
        String result = App.translateHelloWorld();
        String[] wordsToCheck = {"Hello", "World"};
        for (String wordToCheck : wordsToCheck) {
            assertFalse(result.contains(wordToCheck),
                    String.format("Expected translation to no longer contain the english %s, received %s", wordToCheck, result)
            );
        }
    }
}
