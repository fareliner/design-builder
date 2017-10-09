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
import java.io.File;
import java.net.URI;

public class PublicCatalogWriter extends FileByFileCatalogWriter {

  private PublicEntry option;

  PublicCatalogWriter(PublicEntry option) throws ParserConfigurationException {
    this.option = option;
  }

  PublicCatalogWriter(PublicEntry option, URI catalogLocation) throws ParserConfigurationException {
    super(catalogLocation);
    this.option = option;
  }

  @Override
  protected void doWrite(Element catalogElement, File schemaFile) throws MojoExecutionException {

    // region construct publicId
    String targetNameSpace = getNamespaceFromSchema(schemaFile);

    String publicId;

    if (option.getPublicId() != null) {
      publicId = option.getPublicId();
    } else if (option.getPublicId() == null && targetNameSpace != null) {
      // tns is used as publicId
      publicId = targetNameSpace;
    } else {
      throw new MojoExecutionException("Schema file " + schemaFile.getName() + " does not contain a targetNamespace. " +
        "Please either add a targetNamespace to the schema or configure the publicId option in the maven plugin configuration.");
    }
    // endregion

    // region append if required
    char appendSeparator = option.getAppendSeparator();

    if (option.isAppendSchemaFile()) {
      // if we append and the separator was not overridden we swap for urn separator
      if (option.getAppendSeparator() == PublicEntry.DEFAULT_SEPARATOR) {
        int schemeIndex = publicId.indexOf(':');
        if (schemeIndex > 0) {
          String scheme = publicId.substring(0, schemeIndex);
          switch (scheme) {
            case "urn":
              appendSeparator = ':';
              break;
            default:
              break;
          }
        }
      }
      // trim trailing spacer just in case
      publicId = publicId.replaceAll(appendSeparator + "$", "");
      String schemaFileName = schemaFile.getName();
      publicId = publicId + appendSeparator + schemaFileName;

    }
    // endregion

    URI schemaURI = schemaFile.getAbsoluteFile().toURI();

    URI schemaToCatalogRelativeURI = getCatalogLocation().relativize(schemaURI);

    // region write schema element
    Element uriSuffixE = catalogElement.getOwnerDocument().createElementNS("urn:oasis:names:tc:entity:xmlns:xml:catalog", "public");
    uriSuffixE.setAttribute("publicId", publicId);
    uriSuffixE.setAttribute("uri", schemaToCatalogRelativeURI.toString());
    catalogElement.appendChild(uriSuffixE);
    // endregion

    if (log.isDebugEnabled() || isVerbose()) {
      log.info("add catalog entry: <public publicId=\"{}\" uri=\"{}\" />", publicId, schemaToCatalogRelativeURI.toString());
    }

  }

}
