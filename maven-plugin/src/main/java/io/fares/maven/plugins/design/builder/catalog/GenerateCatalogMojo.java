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

package io.fares.maven.plugins.design.builder.catalog;

import java.io.File;
import java.net.URI;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import io.fares.maven.plugins.design.builder.scanner.InclusionScanException;
import io.fares.maven.plugins.design.builder.scanner.SimpleSourceInclusionScanner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Mojo(
  name = "catalog",
  defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
  threadSafe = true,
  requiresDependencyResolution = ResolutionScope.NONE
)
public class GenerateCatalogMojo extends AbstractMojo {

  /**
   * Generates a catalog in multiple formats.
   * https://www.oasis-open.org/committees/download.php/14809/xml-catalogs.html
   */
  @Parameter
  private CatalogOption catalog;
  /**
   * A reference to the executing maven project.
   */
  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;
  /**
   * If verbose is set to true it will output more information messages than normal.
   */
  @Parameter(property = "verbose", defaultValue = "false")
  private boolean verbose;
  /**
   * Should one have the need to skip execution of the module in a build phase.
   */
  @Parameter(property = "skip", defaultValue = "false")
  private boolean skip;
  /**
   * The list of maven project resources.
   */
  @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
  private List<Resource> resources;
  /**
   * The sources directory where the schema files are located.
   */
  @Parameter(alias = "sourceDirectory", property = "sourceDirectory", defaultValue = "${project.basedir}", required = true)
  private File sourceDirectory;
  /**
   * The directory where the schema file will be written to.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
  private File outputDirectory;
  /**
   * The set of local schema files to include into the catalog generation.
   */
  @Parameter
  private Set<String> includes = new HashSet<String>();
  /**
   * The set of local schema files to exclude from the catalog generation.
   */
  @Parameter
  private Set<String> excludes = new HashSet<String>();
  /**
   * The location where the catalog file will be written to.
   */
  @Parameter(alias = "targetCatalogFile", property = "catalog.file", defaultValue = "catalog.xml", required = true)
  private File targetCatalogFile;
  /**
   * Transformer used to format the catalog file.
   */
  private transient Transformer transformer;

  public void execute() throws MojoExecutionException {

    if (skip)
      return;

    // validate we got something to do
    if (this.catalog == null) {
      throw new MojoExecutionException("No catalog configuration has been provided.");
    }

    // Not sure if this is the way to go in a standard maven build. Why does
    // this directory not exist in a resources cycle?
    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }

    // not sure why sometimes the sources dir is not set but meeh will jsut grab resources 1
    if (sourceDirectory == null) {
      String resourceOne = resources.get(0).getDirectory();
      sourceDirectory = new File(resourceOne);
    }

    // validate sources dir exists
    if (!sourceDirectory.exists()) {
      throw new MojoExecutionException("Source directory "
        + sourceDirectory.toURI().toString()
        + " does not exist");
    }

    if (getLog().isDebugEnabled()) {
      getLog().debug(
        "Process catalog resources in "
          + sourceDirectory.getAbsoluteFile().toURI().toString());
    }

    // validate target catalog file path
    File catalogTargetDirectory = targetCatalogFile.getParentFile();
    if (!catalogTargetDirectory.exists()) {
      if (!catalogTargetDirectory.mkdirs()) {
        throw new MojoExecutionException("Could not create target dir " + catalogTargetDirectory.getAbsolutePath());
      }
    }

    URI catalogLocationURI = catalogTargetDirectory.toURI();

    if (getLog().isDebugEnabled() || verbose) {
      getLog().info("   - catalog base [" + catalogLocationURI.getPath() + ']');
    }

    // TODO validate catalog file param

    try {

      // region define sorted schema files
      SimpleSourceInclusionScanner scanner = getSourceInclusionScanner();
      // FIXME get resources from all URLs listed in resources attribute - just do a loop in the scanner
      Set<File> scannedSchemaFiles = scanner.getIncludedSources(sourceDirectory);

      if (scannedSchemaFiles.size() == 0) {
        getLog().warn("No resources for catalog file found in " + sourceDirectory.toString());
      }

      File[] schemaFiles = scannedSchemaFiles.toArray(new File[scannedSchemaFiles.size()]);
      Arrays.sort(schemaFiles);
      // endregion

      //region construct the catalog (also check if the catalog has a prefer system/public flag)
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.newDocument();
      Element catalogElement = doc.createElementNS("urn:oasis:names:tc:entity:xmlns:xml:catalog", "catalog");
      doc.appendChild(catalogElement);
//    Element catDTD = doc.createElementNS("urn:oasis:names:tc:entity:xmlns:xml:catalog", "system");
//    catDTD.setAttribute("systemId", "http://www.oasis-open.org/committees/entity/release/1.1/catalog.dtd");
//    catDTD.setAttribute("uri", "resource:org/apache/xml/resolver/etc/catalog.dtd");
//    rootElement.appendChild(catDTD);

      // if a preference is specified, we'll add it
      if (this.catalog.getPrefer() != null) {
        CatalogPreference prefer = CatalogPreference.fromValue(this.catalog.getPrefer());
        catalogElement.setAttribute("prefer", prefer.value());
      }
      // endregion

      // region write entries to the catalog
      List<CatalogWriter> catalogWriters = createCatalogWriters(catalogLocationURI);
      for (CatalogWriter writer : catalogWriters) {
        writer.write(catalogElement, schemaFiles);
      }
      // endregion

      // region write catalog
      if (getLog().isInfoEnabled()) {
        getLog().info("Write catalog to " + targetCatalogFile.getAbsoluteFile().toURI().toString());
      }

      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(targetCatalogFile);
      getTransformer().transform(source, result);
      //endregion

    } catch (InclusionScanException e) {
      throw new MojoExecutionException("Failed to get included files.", e);
    } catch (ParserConfigurationException e) {
      throw new MojoExecutionException("Failed to configure the xml builder.", e);
    } catch (TransformerConfigurationException e) {
      throw new MojoExecutionException("Failed to configure the xml transformer.", e);
    } catch (TransformerException e) {
      throw new MojoExecutionException("Failed to generate catalog file.", e);
    }

  }


  /**
   * Contruct a {@link CatalogWriter} from the mojo configuration.
   *
   * @return the catalog writer that will determine which format it is writen to
   *
   * @throws MojoExecutionException if anything goes wrong creating the catalog.
   */
  private List<CatalogWriter> createCatalogWriters(URI catalogLocation) throws MojoExecutionException, ParserConfigurationException {

    CatalogEntries entries = this.catalog;

    List<CatalogWriter> writers = new LinkedList<>();

    for (CatalogFormat format : CatalogFormat.values()) {
      switch (format) {
        case PUBLIC:
          if (entries.getPublic() != null) {
            writers.add(new PublicCatalogWriter(entries.getPublic())
              .withCatalogLocation(catalogLocation)
              .withVerbose(verbose));
          }
          break;
        case SYSTEM:
          if (entries.getSystem() != null) {
            writers.add(new SystemCatalogWriter(entries.getSystem())
              .withCatalogLocation(catalogLocation)
              .withVerbose(verbose));
          }
          break;
        case URI:
          if (entries.getUri() != null) {
            writers.add(new UriCatalogWriter(entries.getUri())
              .withCatalogLocation(catalogLocation)
              .withVerbose(verbose));
          }
          break;
        case REWRITE_SYSTEM:
          if (entries.getRewriteSystem() != null) {
            writers.add(new RewriteSystemCatalogWriter(entries.getRewriteSystem())
              .withCatalogLocation(catalogLocation)
              .withVerbose(verbose));
          }
          break;
        case SYSTEM_SUFFIX:
          if (entries.getSystemSuffix() != null) {
            writers.add(new SystemSuffixCatalogWriter(entries.getSystemSuffix())
              .withCatalogLocation(catalogLocation)
              .withVerbose(verbose));
          }
          break;
      }
    }

    if (writers.size() == 0) {
      throw new MojoExecutionException("No catalog format has been provided. Please specify one of [system|rewriteSystem|systemSuffix|uri|public] in the catalog plugin configuration.");
    }


    return writers;

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

  /**
   * Create a transformer to format the catalog file.
   *
   * @return a configured xml transformer
   *
   * @throws TransformerConfigurationException thrown when the transformer cannot be created
   */
  private Transformer getTransformer() throws TransformerConfigurationException {

    if (this.transformer == null) {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    }

    return this.transformer;

  }

  /**
   * @return the absolute file of the generated catalog
   */
  public File getTargetCatalogFile() {
    return targetCatalogFile;
  }

  public void setTargetCatalogFile(File targetCatalogFile) {
    this.targetCatalogFile = targetCatalogFile;
  }

  public File getSourceDirectory() {
    return sourceDirectory;
  }

}
