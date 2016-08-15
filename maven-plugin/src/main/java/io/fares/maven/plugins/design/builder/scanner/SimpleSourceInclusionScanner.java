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

package io.fares.maven.plugins.design.builder.scanner;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.util.DirectoryScanner;

public class SimpleSourceInclusionScanner {
    private Set<String> sourceIncludes;

    private Set<String> sourceExcludes;

    public SimpleSourceInclusionScanner(Set<String> sourceIncludes,
	    Set<String> sourceExcludes) {
	this.sourceIncludes = sourceIncludes;

	this.sourceExcludes = sourceExcludes;
    }

    public Set<File> getIncludedSources(File sourceDir)
	    throws InclusionScanException {

	String[] potentialSources = scanForSources(sourceDir, sourceIncludes,
		sourceExcludes);

	Set<File> matchingSources = new HashSet<File>(
		potentialSources != null ? potentialSources.length : 0);

	if (potentialSources != null) {
	    for (String potentialSource : potentialSources) {
		matchingSources.add(new File(sourceDir, potentialSource));
	    }
	}

	return matchingSources;
    }

    protected String[] scanForSources(File sourceDir,
	    Set<String> sourceIncludes, Set<String> sourceExcludes) {
	DirectoryScanner ds = new DirectoryScanner();
	ds.setFollowSymlinks(true);
	ds.setBasedir(sourceDir);

	String[] includes;
	if (sourceIncludes.isEmpty()) {
	    includes = new String[0];
	} else {
	    includes = sourceIncludes
		    .toArray(new String[sourceIncludes.size()]);
	}

	ds.setIncludes(includes);

	String[] excludes;
	if (sourceExcludes.isEmpty()) {
	    excludes = new String[0];
	} else {
	    excludes = sourceExcludes
		    .toArray(new String[sourceExcludes.size()]);
	}

	ds.setExcludes(excludes);
	ds.addDefaultExcludes();

	ds.scan();

	return ds.getIncludedFiles();
    }
}
