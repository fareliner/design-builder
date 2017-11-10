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

package io.fares.maven.plugins.design.builder.flattener;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import org.apache.xerces.util.XMLCatalogResolver;
import org.xml.sax.InputSource;

import jlibs.xml.sax.crawl.XMLCrawler;
import io.fares.design.builder.JlibsResolverBridge;

import io.fares.maven.plugins.design.builder.scanner.CatalogFileScanner;
import io.fares.maven.plugins.design.builder.scanner.CatalogFileScannerFactory;
import io.fares.maven.plugins.design.builder.MavenCatalogResolver;
import io.fares.maven.plugins.design.builder.MavenCatalogResolverFactory;
import io.fares.maven.plugins.design.builder.scanner.SimpleSourceInclusionScanner;

@SuppressWarnings("UnusedDeclaration") // Used reflectively by Maven.
@Mojo(
  name = "flatten",
  defaultPhase = LifecyclePhase.GENERATE_SOURCES,
  threadSafe = true,
  requiresDependencyCollection = ResolutionScope.COMPILE,
  requiresDependencyResolution = ResolutionScope.COMPILE
)
public class FlattenImportPathMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
  private ArtifactRepository localRepository;

  /**
   * The list of resources we want to transfer.
   */
  @Parameter(defaultValue = "${project.resources}", readonly = true, required = true)
  private List<Resource> resources;

  @Component
  private ProjectBuilder projectBuilder;

  @Component
  private MavenCatalogResolverFactory mavenCatalogResolverFactory;

  @Component
  private CatalogFileScannerFactory catalogFileScannerFactory;
  /**
   * The entry point to Aether, i.e. the component doing all the work.
   */
  @Component
  private RepositorySystem repositorySystem;

  /**
   * The current repository/network configuration of Maven.
   */
  @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
  private RepositorySystemSession repositorySystemSession;

  /**
   * The project's remote repositories to use for the resolution of plugins and their dependencies.
   */
  @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
  private List<RemoteRepository> remoteRepositories;

  /**
   * A specified resource location pattern used to locate the
   * <code>catalog.xml</code> resources that are used by the flattening
   * engine. The pattern can be any that is accepted by the Spring
   * {@code org.springframework.core.io.support.PathMatchingResourcePatternResolver}
   */
  @Parameter(defaultValue = "${project.compileClasspathElements}", required = true, readonly = true)
  private List<String> compileClasspathElements;

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File outputDirectory;

  @Parameter(property = "catalogs")
  private ResourceEntry[] catalogs;

  /**
   * Also scan classpath dependencies for catalog files. Use in conjunction with {@link #catalogFilter} to select specific entries. If not selected,
   */
  @Parameter(property = "flatten.catalog.scanDependencies", defaultValue = "false")
  private boolean scanDependencies;

  /**
   * This flag can be used to scan for different catalog files in the classpath
   */
  @Parameter(property = "flatten.catalog.filter", alias = "catalogFilter", defaultValue = "^(.*/)?catalog\\.xml")
  private String catalogFilter = "^(.*/)?catalog\\.xml";

  /**
   * This can be used instead of the sourceDirectory to process a single xsd
   * or wsdl file e.g. for a web URL reference.
   */
  @Parameter(property = "flatten.target", alias = "flattenTarget")
  private String flattenTarget;

  /**
   * The directory that contains the source files
   */
  @Parameter(alias = "sourceDirectory", property = "sourceDirectory", defaultValue = "${project.basedir}", required = true)
  private File sourceDirectory;

  /**
   * A list of inclusion filters for the catalog generator.
   */
  @Parameter
  private Set<String> includes = new HashSet<>();

  /**
   * A list of exclusion filters for the catalog generator.
   */
  @Parameter
  private Set<String> excludes = new HashSet<>();

  /**
   * If set to true, Maven default excludes are NOT added to all the excludes
   * lists.
   */
  @Parameter(property = "maven.dmb.disableDefaultExcludes", alias = "disableDefaultExcludes", defaultValue = "false")
  private boolean disableDefaultExcludes;

  @Parameter(property = "error.halt", alias = "haltOnError", defaultValue = "false")
  private boolean haltOnError;
  /**
   * This flag will cause the content of existing imports to be overridden if
   * it was already flattened. Please make sure you always use -U clean
   * install if using this flag otherwise you'll end up with stale files
   * preventing the refresh flattening.
   */
  @Parameter(property = "flatten.override.existing.reference", alias = "overrideExistingReference", defaultValue = "true")
  private boolean overrideExistingReference = true;

  @Parameter(property = "verbose", defaultValue = "false")
  private boolean verbose;

  @Parameter(property = "skip", defaultValue = "false")
  private boolean skip;

  public FlattenImportPathMojo() {
    catalogs = new ResourceEntry[0];
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    if (skip)
      return;

    // Not sure if this is the way to go in a standard maven build. Why does
    // this directory not exist in a compile cycle?
    if (!outputDirectory.exists()) {
      boolean targetCreated = outputDirectory.mkdirs();
      if (!targetCreated) {
        throw new MojoExecutionException("failed to create output directory " + outputDirectory.getAbsolutePath());
      }
      if (getLog().isDebugEnabled() || verbose) {
        getLog().debug("Need to create output directory " + outputDirectory.toString());
      }
    }

    if (getLog().isDebugEnabled() || verbose)
      getLog().info("Flattened schemata will be placed in " + outputDirectory.toString());

    // TODO change to use classpath scanner to either a) look into the plugin classpath or b) look into the project classpath or catalog files

    /*
     *
     * Assemble a classpath list that will be used to scan for catalog files. This classpath list is based on the
     * maven plugin configuration.
     *
     * The default is:
     * 1. the sources folder
     * 2. the maven plugin catalog config
     * 3. the project dependency tree
     *
     */

    try {

      List<URL> catalogFiles = new LinkedList<>();

      CatalogFileScanner catScanner = catalogFileScannerFactory.newInstance(
        repositorySystemSession,
        catalogs,
        sourceDirectory,
        includes,
        excludes);

      if (scanDependencies) {
        catScanner.setCompileClasspathElements(compileClasspathElements);
      }

      catalogFiles.addAll(catScanner.scan(catalogFilter));

      // the crawler is our friend as he can flatten path references of
      // all sorts of XML documents including xsd and wsdl imports
      if (getLog().isDebugEnabled() || verbose) {
        getLog().info("Following catalogs have been provided: ");
        for (URL catalogFile : catalogFiles) {
          getLog().info(" :: " + catalogFile.getPath());
        }
      }

      MavenCatalogResolver resolver = mavenCatalogResolverFactory.newInstance(repositorySystemSession, catalogFiles);
      // r = createXercesResolver(catalogURLs);

      JlibsResolverBridge resolverBridge = new JlibsResolverBridge(resolver);

      // either flatten file or sources
      Set<URL> artifacts = new HashSet<>(10);

      if (sourceDirectory == null && flattenTarget == null) {
        // error out
        throw new MojoExecutionException("Neither sourceDirectory nor flattenTarget provided.");

      } else if (flattenTarget != null) {
        // FIXME should really resolve either source and target as a project resource add the flatten target to the artefact list

        if (getLog().isDebugEnabled() || verbose)
          getLog().info("Flattened target " + flattenTarget);

        URL url = new URL(flattenTarget);

        artifacts.add(url);

      } else if (sourceDirectory.exists()) {
        // scan the source directory for any configured (xsd) files
        SimpleSourceInclusionScanner scanner = getSourceInclusionScanner();
        // FIXME get resources from all URLs listed in resources
        // attribute - just do a loop in the scanner
        Set<File> files = scanner.getIncludedSources(sourceDirectory);
        for (File file : files) {
          artifacts.add(file.toURI().toURL());
        }

      }

      // check we got something
      if (artifacts.size() == 0) {
        getLog().warn("No resources for catalog file found in " + sourceDirectory.toString());
      }

      /*************************************************************************
       * flatten the passed targetFiles to the outputDirectory
       *
       */

      List<Throwable> errorEncountered = new LinkedList<>();

      for (URL targetFile : artifacts) {
        try {
          if (getLog().isDebugEnabled() || verbose)
            getLog().info("Flatten file: " + targetFile.toExternalForm());
          // FIXME do a proper URI check
          InputSource source = new InputSource(targetFile.toExternalForm());
          XMLCrawler crawler = new XMLCrawler();
          crawler.setResolver(resolverBridge);
          crawler.crawl(source, new SimpleNameCrawlerListener(
            outputDirectory, overrideExistingReference), null);
        } catch (Throwable e) {
          errorEncountered.add(e);
          getLog().error("Failed processing " + targetFile, e);
          if (haltOnError)
            throw e;
        } finally {
          // FIXME no longer needed? crawler.reset();
        }
      }

      // if not haltonerror and we some ...
      if (!haltOnError && errorEncountered.size() > 0) {
        // TODO give all errors back
        throw new MojoExecutionException(
          "Errors encountered during processing.",
          errorEncountered.get(0));
      }

    } catch (IOException e) {
      throw new MojoExecutionException("Failed to resolve catalog files.", e);
    } catch (Throwable e) {
      throw new MojoExecutionException("Some other failure occurred.", e);
    }

  }


  @SuppressWarnings("unused")
  private XMLCatalogResolver createXercesResolver(List<URL> catalogsArg)
    throws IOException, URISyntaxException {

    if (catalogsArg.size() > 0 && (getLog().isDebugEnabled() || verbose)) {
      getLog().debug("Adding catalogs to resolver: ");
    }

    String[] catalogs = new String[catalogsArg.size()];

    int i = 0;
    for (URL catalogURL : catalogsArg) {
      catalogs[i++] = catalogURL.toURI().toASCIIString();
      if (getLog().isDebugEnabled() || verbose) {
        getLog().debug("add catalog to resolver: " + catalogs[i - 1]);
      }
    }

    return createXercesResolver(catalogs);

  }

  /**
   * Setup a <code>XMLCatalogResolver</code> to be used by the crawler.
   *
   * @param catalogs A list of catalog locations. All need to be valid URIs.
   *
   * @return a configured catalog resolver
   */
  private XMLCatalogResolver createXercesResolver(String[] catalogs) {

    XMLCatalogResolver resolver = new XMLCatalogResolver();
    // TODO config option to toggle publicId or systemId ??
    resolver.setPreferPublic(true);
    resolver.setCatalogList(catalogs);
    return resolver;

  }

  /**
   * @return a simple source inclusions scanner
   */
  protected SimpleSourceInclusionScanner getSourceInclusionScanner() {

    if (includes.isEmpty()) {
      includes.add("**/*.xsd");
      includes.add("**/*.wsdl");
      // TODO add any others by default?
    }

    return new SimpleSourceInclusionScanner(includes, excludes);

  }


  /*
   *
   * Getters and Setters
   *
   */

  public MavenProject getProject() {
    return project;
  }

  public void setProject(MavenProject project) {
    this.project = project;
  }

  public RepositorySystemSession getRepositorySystemSession() {
    return repositorySystemSession;
  }

  public void setRepositorySystemSession(RepositorySystemSession repositorySystemSession) {
    this.repositorySystemSession = repositorySystemSession;
  }

  public RepositorySystem getRepositorySystem() {
    return repositorySystem;
  }

  public void setRepositorySystem(RepositorySystem repositorySystem) {
    this.repositorySystem = repositorySystem;
  }

  public MavenCatalogResolverFactory getMavenCatalogResolverFactory() {
    return mavenCatalogResolverFactory;
  }

  public void setMavenCatalogResolverFactory(MavenCatalogResolverFactory mavenCatalogResolverFactory) {
    this.mavenCatalogResolverFactory = mavenCatalogResolverFactory;
  }

}
