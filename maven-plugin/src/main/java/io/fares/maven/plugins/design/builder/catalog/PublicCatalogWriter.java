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

package io.fares.maven.plugins.design.builder.catalog;

import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.net.URI;

public class PublicCatalogWriter extends FileByFileCatalogWriter {

  private final PublicOption option;

  PublicCatalogWriter(PublicOption option) throws ParserConfigurationException {
    this.option = option;
  }

  PublicCatalogWriter(PublicOption option, URI catalogLocation) throws ParserConfigurationException {
    super(catalogLocation);
    this.option = option;
  }

  @Override
  protected void doWrite(Element catalogElement, File schemaFile) throws MojoExecutionException {

    // construct publicId
   String publicId = constructEntityId(option, schemaFile);

  // if there is no publicId we need to skip this one
   if (publicId == null) {
     if (log.isWarnEnabled()) {
       log.warn("refuse adding catalog public entry: file {} is a chameleon schema", schemaFile.getName());
     }
     return;
   }

    // construct actual schema uri
    URI uri = constructUri(option, schemaFile);

    // region write schema element
    Element uriSuffixE = catalogElement.getOwnerDocument().createElementNS("urn:oasis:names:tc:entity:xmlns:xml:catalog", "public");
    uriSuffixE.setAttribute("publicId", publicId);
    uriSuffixE.setAttribute("uri", uri.toASCIIString());
    catalogElement.appendChild(uriSuffixE);
    // endregion

    if (log.isDebugEnabled() || isVerbose()) {
      log.info("add catalog entry: <public publicId=\"{}\" uri=\"{}\" />", publicId, uri.toASCIIString());
    }

  }

}
