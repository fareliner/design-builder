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

import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.util.Iterator;

public class UriCatalogWriter extends FileByFileCatalogWriter {

  private UriEntry option;

  UriCatalogWriter(UriEntry option) throws ParserConfigurationException {
    this.option = option;
  }

  UriCatalogWriter(UriEntry option, URI catalogLocation) throws ParserConfigurationException {
    super(catalogLocation);
    this.option = option;
  }

  @Override
  protected void doWrite(File schemaFile) throws MojoExecutionException {

    // region construct uri name
    // TODO if option.systemId is null we should just get the target namespace from each schema file (and fail if neither is provided)

    String targetNameSpace = getNamespaceFromSchema(schemaFile);

    String uriName = null;

    if (option.getNamePrefix() == null && targetNameSpace != null) {
      uriName = targetNameSpace;
    } else if (option.getNamePrefix() != null) {
      uriName = option.getNamePrefix();
    } else {
      throw new MojoExecutionException("Schema file " + schemaFile.getName() + " does not contain a targetNamespace. " +
        "Please either add a targetNamespace to the schema or configure a default systemId in the maven plugin configuration.");
    }

    if (option.isAppendSchemaFile()) {
      // trim trailing slash off systemId just in case
      uriName = uriName.replaceAll("/$", "");
      String schemaFileName = schemaFile.getName();
      uriName = String.format("%s/%s", uriName, schemaFileName);
    }
    // endregion

    URI schemaURI = schemaFile.getAbsoluteFile().toURI();

    URI schemaToCatalogRelativeURI = getCatalogLocation().relativize(schemaURI);

    // region write schema element
    Element uriSuffixE = getDocument().createElementNS("urn:oasis:names:tc:entity:xmlns:xml:catalog", "uri");
    uriSuffixE.setAttribute("name", uriName);
    uriSuffixE.setAttribute("uri", schemaToCatalogRelativeURI.toString());
    getElement().appendChild(uriSuffixE);
    // endregion

    if (log.isDebugEnabled() || isVerbose()) {
      log.info("add catalog entry: <system systemId=\"{}\" uri=\"{}\" />", uriName, schemaToCatalogRelativeURI.toString());
    }

  }

}
