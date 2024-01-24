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

package io.fares.maven.plugins.design.builder.flattener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static java.text.MessageFormat.format;

import io.fares.maven.plugins.design.builder.MavenCatalogResolver;
import io.fares.maven.plugins.utils.IOUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import javax.inject.Named;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Named
@Component(role = ResourceEntryDependencyResolver.class)
public class ResourceEntryDependencyResolver {

  Logger log = LoggerFactory.getLogger(ResourceEntryDependencyResolver.class);

  @Requirement
  ArtifactResolver artifactResolver;

  private boolean disableDefaultExcludes;

  /**
   * FIXME same logic to resolve resource (artifact + embedded resource) as {@link MavenCatalogResolver#createArtifactResourceUrl(Artifact, String)}
   *
   * @param repositorySystemSession a valid repo system
   * @param remoteRepositories      the remote repositories to use to look for the resources
   * @param resourceEntry           the resource to turn into a UKR
   * @param defaultDirectory        the default directory
   * @param defaultIncludes         includes list
   * @param defaultExcludes         excludes list
   * @return a list of URLs representing the resourceEntry set
   * @throws ArtifactResolutionException thrown if the resource entry was not found in the repositories provided
   * @throws IOException                 thrown if any other error occured
   */
  public List<URL> createResourceEntryUrls(RepositorySystemSession repositorySystemSession,
                                           List<RemoteRepository> remoteRepositories,
                                           ResourceEntry resourceEntry,
                                           String defaultDirectory, String[] defaultIncludes,
                                           String[] defaultExcludes) throws ArtifactResolutionException, IOException {
    if (resourceEntry == null) {
      return Collections.emptyList();
    }

    final List<URL> urls = new LinkedList<URL>();

    // 1. resolve file deps

    if (resourceEntry.getFileset() != null) {
      final FileSet fileset = resourceEntry.getFileset();
      urls.addAll(createFileSetUrls(fileset, defaultDirectory,
        defaultIncludes, defaultExcludes));
    }

    // 2. resolve url sets
    if (resourceEntry.getUrl() != null) {
      String urlDraft = resourceEntry.getUrl();
      urls.add(new URL(urlDraft));
    }

    // 3. resolve resource dependency items
    if (resourceEntry.getDependencyResource() != null) {

      DependencyResource dependency = resourceEntry.getDependencyResource();
      Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getType(), dependency.getVersion());
      ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, null);
      ArtifactResult result = artifactResolver.resolveArtifact(repositorySystemSession, request);
      URL resourceURL = result.getArtifact().getFile().toURI().toURL();

      if (resourceURL == null) {
        // TODO try to resolve on classpath or throw an exception cause we cannot find the dependency?
      } else {
        if (log.isDebugEnabled())
          log.debug(format("Resolved dependency resource [{0}] to resource URL [{1}].", dependency.toString(), resourceURL));
        urls.add(resourceURL);
      }

    }
    // end resolve resource dependency items

    /*
     * TODO scan project classpath for catalog files too, this way we would not need to "include" standard packaged design modules
     * but merely add jars to the project classpath and kick off the design build
     */

    return urls;

  }


  private List<URL> createFileSetUrls(final FileSet fileset,
                                      String defaultDirectory, String[] defaultIncludes,
                                      String defaultExcludes[]) throws IOException {

    final String draftDirectory = fileset.getDirectory();
    final String directory = draftDirectory == null ? defaultDirectory : draftDirectory;
    final List<String> includes;
    final List<String> draftIncludes = fileset.getIncludes();

    if (draftIncludes == null || draftIncludes.isEmpty()) {
      includes = defaultIncludes == null ? Collections.<String>emptyList() : Arrays.asList(defaultIncludes);
    } else {
      includes = draftIncludes;
    }

    final List<String> excludes;
    final List<String> draftExcludes = fileset.getExcludes();

    if (draftExcludes == null || draftExcludes.isEmpty()) {
      excludes = defaultExcludes == null ? Collections
        .<String>emptyList() : Arrays.asList(defaultExcludes);
    } else {
      excludes = draftExcludes;
    }

    String[] includesArray = includes.toArray(new String[includes.size()]);
    String[] excludesArray = excludes.toArray(new String[excludes.size()]);

    final List<File> files = IOUtils.scanDirectoryForFiles(new File(directory), includesArray, excludesArray, !disableDefaultExcludes);

    final List<URL> urls = new ArrayList<URL>(files.size());

    for (final File file : files) {
      urls.add(file.toURI().toURL());
    }

    return urls;

  }

}
