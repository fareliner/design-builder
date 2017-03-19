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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

@Mojo(
  name = "catalog",
  defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
  threadSafe = true,
  requiresDependencyResolution = ResolutionScope.NONE
)
public class GenerateCatalogMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(property = "verbose", defaultValue = "false")
  private boolean verbose;

  @Parameter(property = "skip", defaultValue = "false")
  private boolean skip;

  @Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
  private List<Resource> resources;

  @Parameter(alias = "sourceDirectory", property = "sourceDirectory", defaultValue = "${project.basedir}", required = true)
  private File sourceDirectory;

  @Parameter(alias = "targetCatalogFile", property = "catalog.file", defaultValue = "catalog.xml", required = true)
  private File targetCatalogFile;

  /**
   * Specifies how much offset the schema file should have to its parent directory. This offset will be used to
   * populate the systemIdSuffix path depth.
   */
  @Parameter(alias = "systemIdPathOffset", property = "systemid.suffix.offset", defaultValue = "0")
  private int systemIdPathOffset;

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
  private File outputDirectory;

  @Parameter
  private Set<String> includes = new HashSet<String>();

  @Parameter
  private Set<String> excludes = new HashSet<String>();

  private transient Transformer transformer;

  public void execute() throws MojoExecutionException {

    if (skip)
      return;

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
      catalogTargetDirectory.mkdirs();
    }

    URI catalogLocationURI = catalogTargetDirectory.toURI();

    if (getLog().isDebugEnabled() || verbose) {
      getLog().info(
        "   - catalog base [" + catalogLocationURI.getPath() + ']');
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

      //region define catalog header
      DocumentBuilderFactory docFactory = DocumentBuilderFactory
        .newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      Document doc = docBuilder.newDocument();
      Element re = doc.createElementNS(
        "urn:oasis:names:tc:entity:xmlns:xml:catalog", "catalog");
      doc.appendChild(re);

      Element decDef = doc.createElementNS("urn:oasis:names:tc:entity:xmlns:xml:catalog", "system");
      decDef.setAttribute("systemId", "http://www.oasis-open.org/committees/entity/release/1.1/catalog.dtd");
      decDef.setAttribute("uri", "resource:org/apache/xml/resolver/etc/catalog.dtd");
      re.appendChild(decDef);
      // endregion

      for (File schemaFile : schemaFiles) {

        URI schemaURI = schemaFile.getAbsoluteFile().toURI();

        URI schemaLocationURI = schemaURI.getPath().endsWith("/")
          ? schemaURI.resolve("../")
          : schemaURI.resolve(".");

        // TODO add some validation to check that the catalog and sources are in the same tree (e.g. cannot create a catalog outside jar for xsds in jar etc.)

        if (getLog().isDebugEnabled() || verbose) {
          getLog().info(
            "   - schema [" + schemaURI.getPath()
              + ']');
        }

        // calculate natural offset between schema and catalog
        // int naturalOffset = catalogLocationURI.compareTo(schemaURI);

        URI systemIdSuffixURI = null;

        if (systemIdPathOffset == 0) {
          systemIdSuffixURI = schemaLocationURI.relativize(schemaURI);
        } else {
                    /*
                     * The systemIdSuffixURI must be offset by systemIdPathOffset relative to the schemaLocationURI. This is
                     * to ensure the schema can be imported with a parent folder prefix but the catalog file can resolve
                     * the schema resource relative to itself using the schemaToCatalogRelativeURI.
                     *
                     * Example: A schema in a nested path <code>src/main/resources/test-execution-config/GlobalDataTypes.xsd</code>
                     *          results into the systemIdSuffixURI <code>test-execution-config/GlobalDataTypes.xsd</code>
                     *
                     */
          StringBuilder sb = new StringBuilder();
          for (int i = 0; i < this.systemIdPathOffset; i++) {
            sb.append("../");
          }
          sb.append('.');
          systemIdSuffixURI = schemaLocationURI.resolve(sb.toString())
            .relativize(schemaURI);
        }


        URI schemaToCatalogRelativeURI = catalogLocationURI.relativize(schemaURI);

        // region write schema element
        Element uriSuffixE = doc.createElementNS("urn:oasis:names:tc:entity:xmlns:xml:catalog", "systemSuffix");
        uriSuffixE.setAttribute("systemIdSuffix", systemIdSuffixURI.toString());
        uriSuffixE.setAttribute("uri", schemaToCatalogRelativeURI.toString());
        re.appendChild(uriSuffixE);
        // endregion

        if (getLog().isDebugEnabled() || verbose) {
          getLog().info(
            String.format(
              "add catalog entry: <systemSuffix systemIdSuffix=\"%s\" uri=\"%s\" />",
              systemIdSuffixURI.toString(),
              schemaToCatalogRelativeURI.toString()
            ));
        }

      }

      if (getLog().isInfoEnabled()) {
        getLog().info("Write catalog to " + targetCatalogFile.getAbsoluteFile().toURI().toString());
      }

      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(targetCatalogFile);

      getTransformer().transform(source, result);

    } catch (InclusionScanException e) {
      throw new MojoExecutionException("Failed to get included files.", e);
    } catch (ParserConfigurationException e) {
      throw new MojoExecutionException(
        "Failed to configure the xml builder.", e);
    } catch (TransformerConfigurationException e) {
      throw new MojoExecutionException(
        "Failed to configure the xml transformer.", e);
    } catch (TransformerException e) {
      throw new MojoExecutionException(
        "Failed to generate catalog file.", e);
    }

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
   * @return a configured xml transformer for use
   * @throws TransformerConfigurationException
   */
  Transformer getTransformer() throws TransformerConfigurationException {

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

  public int getSystemIdPathOffset() {
    return systemIdPathOffset;
  }

  public void setSystemIdPathOffset(int depth) {
    this.systemIdPathOffset = depth;
  }

}
