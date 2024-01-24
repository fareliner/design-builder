/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.fares.maven.plugins.design.builder.scanner;

import io.fares.maven.plugins.design.builder.flattener.ResourceEntry;
import io.fares.maven.plugins.design.builder.flattener.ResourceEntryDependencyResolver;
import io.fares.maven.plugins.utils.CollectionUtils;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static java.text.MessageFormat.format;

public final class CatalogFileScanner {

  private static final Logger log = LoggerFactory.getLogger(CatalogFileScanner.class);

  private List<String> compileClasspathElements;

  private ResourceEntryDependencyResolver resourceEntryResolver;

  private RepositorySystemSession repositorySystemSession;

  private List<RemoteRepository> remoteRepositories;

  private ResourceEntry[] catalogs;

  private List<Resource> resources;

  private File sourceDirectory;

  private Set<String> includes;

  private Set<String> excludes;


  public List<String> getCompileClasspathElements() {
    return compileClasspathElements;
  }

  public void setCompileClasspathElements(List<String> compileClasspathElements) {
    this.compileClasspathElements = compileClasspathElements;
  }

  public RepositorySystemSession getRepositorySystemSession() {
    return repositorySystemSession;
  }

  public void setRepositorySystemSession(RepositorySystemSession repositorySystemSession) {
    this.repositorySystemSession = repositorySystemSession;
  }

  public List<RemoteRepository> getRemoteRepositories() {
    return remoteRepositories;
  }

  public void setRemoteRepositories(List<RemoteRepository> remoteRepositories) {
    this.remoteRepositories = remoteRepositories;
  }

  public ResourceEntryDependencyResolver getResourceEntryResolver() {
    return resourceEntryResolver;
  }

  public void setResourceEntryResolver(ResourceEntryDependencyResolver resourceEntryResolver) {
    this.resourceEntryResolver = resourceEntryResolver;
  }

  public List<Resource> getResources() {
    return resources;
  }

  public void setResources(List<Resource> resources) {
    this.resources = resources;
  }

  public ResourceEntry[] getCatalogs() {
    return catalogs;
  }

  public void setCatalogs(ResourceEntry[] catalogs) {
    this.catalogs = catalogs;
  }

  public File getSourceDirectory() {
    return sourceDirectory;
  }

  public void setSourceDirectory(File sourceDirectory) {
    this.sourceDirectory = sourceDirectory;
  }

  public Set<String> getIncludes() {
    return includes;
  }

  public void setIncludes(Set<String> includes) {
    this.includes = includes;
  }

  public Set<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(Set<String> excludes) {
    this.excludes = excludes;
  }

  public List<URL> scan(Pattern catalogFilter) throws IOException, ArtifactResolutionException {

    Set<URL> cp = new HashSet<>();

    // 1. add all classpath resources
    cp.addAll(getClassPathElementURLs(compileClasspathElements));

    // 2. add all resource folders
    // TODO filter proper excludes
    for (Resource resource : resources) {
      File resourceDir = new File(resource.getDirectory());
      cp.add(resourceDir.toURI().toURL());
    }

    // 3. also add all plugin catalog resources
    cp.addAll(getCatalogUrls());

    StringBuilder classpath = new StringBuilder();

    // build a classpath
    for (URL el : cp) {
      classpath.append(File.pathSeparatorChar);
      classpath.append(el.toExternalForm());
    }

    final List<URL> catalogFiles = new LinkedList<>();

    if (log.isInfoEnabled()) {
      log.info("Scanner Classpath:" + '\n' + classpath);
    }

    if (log.isDebugEnabled()) {
      log.debug("Scanner uses catalogFilter: {}", catalogFilter);
    }


    Map<String, String> pathToFileContent = new HashMap<>();

    ClassGraph scanner = new ClassGraph()
      .overrideClasspath(classpath.toString())
      .acceptPaths("*");

    try (ScanResult scanResult = scanner.scan()) {
      // filter the results for a regex match of the filter
      scanResult.getResourcesMatchingPattern(catalogFilter)
        .forEachByteArrayThrowingIOException(
          (io.github.classgraph.Resource r, byte[] fileContent) -> {
            pathToFileContent.put(r.getPath(), new String(fileContent, StandardCharsets.UTF_8));

            if (log.isDebugEnabled()) {
              log.debug(" :: add catalog resource: {}", r.getURL().toExternalForm());
            }

            catalogFiles.add(r.getURL());

          }
        );
    }

    if (log.isDebugEnabled()) {
      scanner.verbose();
    }

    scanner.scan();

    return catalogFiles;

  }

  protected List<URL> getClassPathElementURLs(List<String> elements) throws MalformedURLException {
    List<URL> result = new ArrayList<>(elements.size());
    for (String dep : elements) {
      result.add(new File(dep).toURI().toURL());
    }
    return result;
  }

  /*
   * FIXME need a function to resolve catalog files
   *
   * the function needs to "find" catalog files in the maven project src|target|catalog resources specified
   *
   * the convention is to look for <t>catalog.xml</t> and retorn a list of URLs we can then use to construct the
   *
   */

  protected List<URL> getCatalogUrls() throws ArtifactResolutionException, IOException {

    final List<URL> allCatalogUrls = new ArrayList<>(catalogs.length);

    for (ResourceEntry catalog : catalogs) {
      List<URL> resolvedCatalogUrls = resourceEntryResolver.createResourceEntryUrls(
        repositorySystemSession,
        remoteRepositories,
        catalog,
        sourceDirectory.getAbsolutePath(),
        CollectionUtils.toArray(includes, String.class),
        CollectionUtils.toArray(excludes, String.class)
      );
      allCatalogUrls.addAll(resolvedCatalogUrls);
    }

    return allCatalogUrls;

  }

}
