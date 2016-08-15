/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.fares.maven.plugins.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.Scanner;
import org.xml.sax.InputSource;

import io.fares.maven.plugins.utils.CollectionUtils.Function;

public class IOUtils {

    public static final Function<File, URL> GET_URL = new Function<File, URL>() {
        public URL eval(File file) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException muex) {
                throw new RuntimeException(muex);
            }
        }
    };
    public static final Function<File, Long> LAST_MODIFIED = new Function<File, Long>() {
        public Long eval(File file) {
            return lastModified(file);
        }
    };

    /**
     * Creates an input source for the given file
     *
     * @param file file to create input source for
     * @return Created input source object
     */
    public static InputSource getInputSource(File file) {
        try {
            final URL url = file.toURI().toURL();
            return getInputSource(url);
        } catch (MalformedURLException e) {
            return new InputSource(file.getPath());
        }
    }

    public static InputSource getInputSource(final URL url) {
        return new InputSource(StringUtils.escapeSpace(url.toExternalForm()));
    }

    public static long lastModified(File file) {
        if (file == null || !file.exists()) {
            return 0;
        } else {
            return file.lastModified();
        }
    }

    /**
     * Scans given directory for files satisfying given inclusion/exclusion
     * patterns.
     *
     * @param directory       Directory to scan.
     * @param includes        inclusion pattern.
     * @param excludes        exclusion pattern.
     * @param defaultExcludes default exclusion flag.
     * @return Files from the given directory which satisfy given patterns. The
     * files are {@link File#getCanonicalFile() canonical}.
     * @throws IOException if an error was encountered scanning the directories
     */
    public static List<File> scanDirectoryForFiles(final File directory,
                                                   final String[] includes, final String[] excludes,
                                                   boolean defaultExcludes) throws IOException {
        if (!directory.exists()) {
            return Collections.emptyList();
        }

        final DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir(directory.getAbsoluteFile());
        final Scanner scanner = directoryScanner;

        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        if (defaultExcludes) {
            scanner.addDefaultExcludes();
        }

        scanner.scan();

        final List<File> files = new ArrayList<File>();
        for (final String name : scanner.getIncludedFiles()) {
            files.add(new File(directory, name).getCanonicalFile());
        }

        return files;
    }

}
