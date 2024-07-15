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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.function.Uncheck;
import org.apache.commons.lang3.function.Consumers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests FileUtils.listFiles() methods.
 */
public class FileUtilsListFilesTest {

    @TempDir
    public File temporaryFolder;

    private Collection<String> filesToFilenames(final Collection<File> files) {
        return files.stream().map(File::getName).collect(Collectors.toList());
    }

    /**
     * Consumes and closes the underlying stream.
     *
     * @param files The iterator to consume.
     * @return a new collection.
     */
    private Collection<String> filesToFilenames(final Iterator<File> files) {
        final Collection<String> fileNames = new ArrayList<>();
        // Iterator.forEachRemaining() closes the underlying stream.
        files.forEachRemaining(f -> fileNames.add(f.getName()));
        return fileNames;
    }

    @BeforeEach
    public void setUp() throws Exception {
        createFile(temporaryFolder, "dummy-build.xml");
        createFile(temporaryFolder, "README");

        File subDir1 = createDirectory(temporaryFolder, "subdir1");
        createFile(subDir1, "dummy-build.xml");
        createFile(subDir1, "dummy-readme.txt");

        File subSubDir1 = createDirectory(subDir1, "subsubdir1");
        createFile(subSubDir1, "dummy-file.txt");
        createFile(subSubDir1, "dummy-index.html");

        File cvsDir = createDirectory(subDir1.getParentFile(), "CVS");
        createFile(cvsDir, "Entries");
        createFile(cvsDir, "Repository");
    }

    private void createFile(File parentDir, String fileName) throws IOException {
        File file = new File(parentDir, fileName);
        FileUtils.touch(file);
    }

    private File createDirectory(File parentDir, String dirName) {
        File dir = new File(parentDir, dirName);
        dir.mkdirs();
        return dir;
    }


    @Test
    public void testIterateFilesByExtension() {
        final String[] extensions = { "xml", "txt" };

        Iterator<File> files = FileUtils.iterateFiles(temporaryFolder, extensions, false);
        try {
            final Collection<String> fileNames = filesToFilenames(files);
            assertEquals(1, fileNames.size());
            assertTrue(fileNames.contains("dummy-build.xml"));
            assertFalse(fileNames.contains("README"));
            assertFalse(fileNames.contains("dummy-file.txt"));
        } finally {
            // Backstop in case filesToFilenames() failure.
            files.forEachRemaining(Consumers.nop());
        }

        try {
            files = FileUtils.iterateFiles(temporaryFolder, extensions, true);
            final Collection<String> fileNames = filesToFilenames(files);
            assertEquals(4, fileNames.size());
            assertTrue(fileNames.contains("dummy-file.txt"));
            assertFalse(fileNames.contains("dummy-index.html"));
        } finally {
            // Backstop in case filesToFilenames() failure.
            files.forEachRemaining(Consumers.nop());
        }

        files = FileUtils.iterateFiles(temporaryFolder, null, false);
        try {
            final Collection<String> fileNames = filesToFilenames(files);
            assertEquals(2, fileNames.size());
            assertTrue(fileNames.contains("dummy-build.xml"));
            assertTrue(fileNames.contains("README"));
            assertFalse(fileNames.contains("dummy-file.txt"));
        } finally {
            // Backstop in case filesToFilenames() failure.
            files.forEachRemaining(Consumers.nop());
        }
    }

    @Test
    public void testListFiles() {
        Collection<File> files;
        Collection<String> fileNames;
        IOFileFilter fileFilter;
        IOFileFilter dirFilter;

        // First, find non-recursively
        fileFilter = FileFilterUtils.trueFileFilter();
        files = FileUtils.listFiles(temporaryFolder, fileFilter, null);
        fileNames = filesToFilenames(files);
        assertTrue(fileNames.contains("dummy-build.xml"), "'dummy-build.xml' is missing");
        assertFalse(fileNames.contains("dummy-index.html"), "'dummy-index.html' shouldn't be found");
        assertFalse(fileNames.contains("Entries"), "'Entries' shouldn't be found");

        // Second, find recursively
        fileFilter = FileFilterUtils.trueFileFilter();
        dirFilter = FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter("CVS"));
        files = FileUtils.listFiles(temporaryFolder, fileFilter, dirFilter);
        fileNames = filesToFilenames(files);
        assertTrue(fileNames.contains("dummy-build.xml"), "'dummy-build.xml' is missing");
        assertTrue(fileNames.contains("dummy-index.html"), "'dummy-index.html' is missing");
        assertFalse(fileNames.contains("Entries"), "'Entries' shouldn't be found");

        // Do the same as above but now with the filter coming from FileFilterUtils
        fileFilter = FileFilterUtils.trueFileFilter();
        dirFilter = FileFilterUtils.makeCVSAware(null);
        files = FileUtils.listFiles(temporaryFolder, fileFilter, dirFilter);
        fileNames = filesToFilenames(files);
        assertTrue(fileNames.contains("dummy-build.xml"), "'dummy-build.xml' is missing");
        assertTrue(fileNames.contains("dummy-index.html"), "'dummy-index.html' is missing");
        assertFalse(fileNames.contains("Entries"), "'Entries' shouldn't be found");

        // Again with the CVS filter but now with a non-null parameter
        fileFilter = FileFilterUtils.trueFileFilter();
        dirFilter = FileFilterUtils.prefixFileFilter("sub");
        dirFilter = FileFilterUtils.makeCVSAware(dirFilter);
        files = FileUtils.listFiles(temporaryFolder, fileFilter, dirFilter);
        fileNames = filesToFilenames(files);
        assertTrue(fileNames.contains("dummy-build.xml"), "'dummy-build.xml' is missing");
        assertTrue(fileNames.contains("dummy-index.html"), "'dummy-index.html' is missing");
        assertFalse(fileNames.contains("Entries"), "'Entries' shouldn't be found");

        assertThrows(NullPointerException.class, () -> FileUtils.listFiles(temporaryFolder, null, null));
    }

    @Test
    public void testListFilesByExtension() {
        final String[] extensions = {"xml", "txt"};

        Collection<File> files = FileUtils.listFiles(temporaryFolder, extensions, false);
        assertEquals(1, files.size());
        Collection<String> fileNames = filesToFilenames(files);
        assertTrue(fileNames.contains("dummy-build.xml"));
        assertFalse(fileNames.contains("README"));
        assertFalse(fileNames.contains("dummy-file.txt"));

        files = FileUtils.listFiles(temporaryFolder, extensions, true);
        fileNames = filesToFilenames(files);
        assertEquals(4, fileNames.size());
        assertTrue(fileNames.contains("dummy-file.txt"));
        assertFalse(fileNames.contains("dummy-index.html"));

        files = FileUtils.listFiles(temporaryFolder, null, false);
        assertEquals(2, files.size());
        fileNames = filesToFilenames(files);
        assertTrue(fileNames.contains("dummy-build.xml"));
        assertTrue(fileNames.contains("README"));
        assertFalse(fileNames.contains("dummy-file.txt"));
    }

    @Test
    public void testListFilesWithDeletion() throws IOException {
        final String[] extensions = {"xml", "txt"};
        final List<File> list;
        final File xFile = new File(temporaryFolder, "x.xml");
        xFile.createNewFile();
        final Collection<File> files = FileUtils.listFiles(temporaryFolder, extensions, true);
        assertEquals(5, files.size());
        try (Stream<File> stream = Uncheck.get(() -> FileUtils.streamFiles(temporaryFolder, true, extensions))) {
            assertTrue(xFile.delete());
            list = stream.collect(Collectors.toList());
            assertFalse(list.contains(xFile), list::toString);
        }
        assertEquals(4, list.size());
    }

}
