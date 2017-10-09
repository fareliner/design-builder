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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.maven.plugin.MojoExecutionException;

abstract class AbstractCatalogWriter implements CatalogWriter {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private boolean verbose = false;

  private URI catalogLocation;

  AbstractCatalogWriter() throws ParserConfigurationException {
  }

  AbstractCatalogWriter(URI catalogLocation) throws ParserConfigurationException {
    this();
    this.catalogLocation = catalogLocation;
  }

  @Override
  public abstract void write(Element catalogElement, File... schemaFiles) throws MojoExecutionException;

  public URI getCatalogLocation() {
    return catalogLocation;
  }

  public void setCatalogLocation(URI catalogLocation) {
    this.catalogLocation = catalogLocation;
  }

  public AbstractCatalogWriter withCatalogLocation(URI catalogLocation) {
    setCatalogLocation(catalogLocation);
    return this;
  }


  public boolean isVerbose() {
    return verbose;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public CatalogWriter withVerbose(boolean verbose) {
    setVerbose(verbose);
    return this;
  }

  public CatalogWriter verbose() {
    setVerbose(true);
    return this;
  }

  protected String getNamespaceFromSchema(File schemaFile) throws MojoExecutionException {

    XMLInputFactory staxFactory = XMLInputFactory.newInstance();

    try {
      XMLEventReader eventReader = staxFactory.createXMLEventReader(new FileReader(schemaFile));
      while (eventReader.hasNext()) {
        XMLEvent event = eventReader.nextEvent();
        if (event.isStartElement()) {
          StartElement startElement = event.asStartElement();
          if ("schema".equals(startElement.getName().getLocalPart())) {
            // first lets try to find the targetNamespace value
            Iterator<Attribute> attributes = startElement.getAttributes();
            while (attributes.hasNext()) {
              Attribute attributeNode = attributes.next();
              if ("targetNamespace".equals(attributeNode.getName().getLocalPart())) {
                return attributeNode.getValue();
              }
            }
          }
        }
      }
    } catch (XMLStreamException e) {
      throw new MojoExecutionException("Schema file " + schemaFile.getName() + " seems to be invalid.", e);
    } catch (FileNotFoundException e) {
      throw new MojoExecutionException("Schema file " + schemaFile.getName() + " does not exists.", e);
    }

    return null;

  }

  protected String guessSeparatorFromUri(String scheme) {

    String separator = "/";

    if (scheme == null ||
      scheme.startsWith("http:") ||
      scheme.startsWith("https:") ||
      scheme.startsWith("file:") ||
      scheme.startsWith("jar:") ||
      scheme.startsWith("classpath:") ||
      scheme.startsWith("mvn:")) {
      return separator;
    } else if (scheme.startsWith("urn:")) {
      return ":";
    }

    return separator;

  }

}
