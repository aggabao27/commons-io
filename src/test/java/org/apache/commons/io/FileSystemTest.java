/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Tests {@link FileSystem}.
 */
public class FileSystemTest {

    @Test
    public void testGetBlockSize() {
        assertTrue(FileSystem.getCurrent().getBlockSize() >= 0);
    }

    @Test
    public void testGetCurrent() {
        if (SystemUtils.IS_OS_WINDOWS) {
            assertEquals(FileSystem.WINDOWS, FileSystem.getCurrent());
        }
        if (SystemUtils.IS_OS_LINUX) {
            assertEquals(FileSystem.LINUX, FileSystem.getCurrent());
        }
        if (SystemUtils.IS_OS_MAC_OSX) {
            assertEquals(FileSystem.MAC_OSX, FileSystem.getCurrent());
        }
    }

    @Test
    public void testIsLegalName() {
        for (final FileSystem fs : FileSystem.values()) {
            testEmptyNameIsIllegal(fs);
            testNullNameIsIllegal(fs);
            testNulCharIsIllegal(fs);
            testSimpleNameIsLegal(fs);
            testReservedNamesAreIllegal(fs);
        }
    }

    private void testEmptyNameIsIllegal(FileSystem fs) {
        assertFalse(fs.isLegalFileName(""), fs.name());
    }

    private void testNullNameIsIllegal(FileSystem fs) {
        assertFalse(fs.isLegalFileName(null), fs.name());
    }

    private void testNulCharIsIllegal(FileSystem fs) {
        assertFalse(fs.isLegalFileName("\0"), fs.name());
    }

    private void testSimpleNameIsLegal(FileSystem fs) {
        assertTrue(fs.isLegalFileName("0"), fs.name());
    }

    private void testReservedNamesAreIllegal(FileSystem fs) {
        for (final String candidate : fs.getReservedFileNames()) {
            assertFalse(fs.isLegalFileName(candidate));
        }
    }


    @Test
    public void testIsReservedFileName() {
        for (final FileSystem fs : FileSystem.values()) {
            for (final String candidate : fs.getReservedFileNames()) {
                assertTrue(fs.isReservedFileName(candidate));
            }
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testIsReservedFileNameOnWindows() {
        final FileSystem fs = FileSystem.WINDOWS;
        for (final String candidate : fs.getReservedFileNames()) {
            // System.out.printf("Reserved %s exists: %s%n", candidate, Files.exists(Paths.get(candidate)));
            assertTrue(fs.isReservedFileName(candidate));
            assertTrue(fs.isReservedFileName(candidate + ".txt"), candidate);
        }

// This can hang when trying to create files for some reserved names, but it is interesting to keep
//
//        for (final String candidate : fs.getReservedFileNames()) {
//            System.out.printf("Testing %s%n", candidate);
//            assertTrue(fs.isReservedFileName(candidate));
//            final Path path = Paths.get(candidate);
//            final boolean exists = Files.exists(path);
//            try {
//                PathUtils.writeString(path, "Hello World!", StandardCharsets.UTF_8);
//            } catch (IOException ignored) {
//                // Asking to create a reserved file either:
//                // - Throws an exception, for example "AUX"
//                // - Is a NOOP, for example "COM3"
//            }
//            assertEquals(exists, Files.exists(path), path.toString());
//        }
    }

    @Test
    public void testReplacementWithNUL() {
        for (final FileSystem fs : FileSystem.values()) {
            try {
                fs.toLegalFileName("Test", '\0'); // Assume NUL is always illegal
            } catch (final IllegalArgumentException iae) {
                assertTrue(iae.getMessage().startsWith("The replacement character '\\0'"), iae.getMessage());
            }
        }
    }

    @Test
    public void testSorted() {
        for (final FileSystem fs : FileSystem.values()) {
            final char[] chars = fs.getIllegalFileNameChars();
            for (int i = 0; i < chars.length - 1; i++) {
                assertTrue(chars[i] < chars[i + 1], fs.name());
            }
        }
    }

    @Test
    public void testSupportsDriveLetter() {
        assertTrue(FileSystem.WINDOWS.supportsDriveLetter());
        assertFalse(FileSystem.GENERIC.supportsDriveLetter());
        assertFalse(FileSystem.LINUX.supportsDriveLetter());
        assertFalse(FileSystem.MAC_OSX.supportsDriveLetter());
    }

    @Test
    public void testToLegalFileNameWindows() {
        final FileSystem fs = FileSystem.WINDOWS;
        final char replacement = '-';
        testIllegalCharactersAreReplaced(fs, replacement);
        testAllowedCharactersAreUnchanged(fs, replacement);
    }

    private void testIllegalCharactersAreReplaced(FileSystem fs, char replacement) {
        for (char i = 0; i < 32; i++) {
            assertEquals(replacement, fs.toLegalFileName(String.valueOf(i), replacement).charAt(0));
        }
        final char[] illegal = { '<', '>', ':', '"', '/', '\\', '|', '?', '*' };
        for (char c : illegal) {
            assertEquals(replacement, fs.toLegalFileName(String.valueOf(c), replacement).charAt(0));
        }
    }

    private void testAllowedCharactersAreUnchanged(FileSystem fs, char replacement) {
        for (char c = 'a'; c <= 'z'; c++) {
            assertEquals(c, fs.toLegalFileName(String.valueOf(c), replacement).charAt(0));
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            assertEquals(c, fs.toLegalFileName(String.valueOf(c), replacement).charAt(0));
        }
        for (char c = '0'; c <= '9'; c++) {
            assertEquals(c, fs.toLegalFileName(String.valueOf(c), replacement).charAt(0));
        }
    }

}
