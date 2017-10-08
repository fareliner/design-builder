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

import java.net.URI;
import java.io.File;
import java.net.URISyntaxException;

import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;

public class SystemCatalogWriter extends FileByFileCatalogWriter {

  private SystemEntry option;

  SystemCatalogWriter(SystemEntry option) throws ParserConfigurationException {
    this.option = option;
  }

  SystemCatalogWriter(SystemEntry option, URI catalogLocation) throws ParserConfigurationException {
    super(catalogLocation);
    this.option = option;
  }

  @Override
  protected void doWrite(File schemaFile) throws MojoExecutionException {

    // region construct systemId
    // systemId from options will trump the schema namespace
    String systemId = option.getSystemId();
    // if no override, try to guess the targetNamespace from the schema
    if (systemId == null) {
      String namespace = getNamespaceFromSchema(schemaFile);
      if (namespace != null) {
        systemId = namespace;
      } else {
        systemId = option.getDefaultSystemId();
      }
    }

    if (systemId == null) {
      throw new MojoExecutionException("Schema file " + schemaFile.getName() + " does not contain a targetNamespace. " +
        "Please either add a targetNamespace to the schema or configure a default systemId in the maven plugin configuration.");
    }

    // now work out if and how to append the schema name to that systemId if configured
    if (option.isAppendSchemaName()) {
      String separator = option.getAppendSeparator();
      if (separator == null) {
        separator = guessSeparatorFromUri(systemId);
      }

      // trim trailing separator off systemId just in case
      String escaped = separator.replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\]\\}\\)‌​\\?\\*\\+\\.\\>]", "\\\\$0");
      systemId = systemId.replaceAll(escaped + "$", "");
      String schemaFileName = schemaFile.getName();
      systemId = String.format("%s%s%s", systemId, separator, schemaFileName);
    }
    // endregion

    // region construct actual schema uri
    URI uri;
    if (option.getUriPrefix() != null) {
      String uriPrefix = option.getUriPrefix().replaceAll("/$", "");
      String uriString = String.format("%s/%s", uriPrefix, schemaFile.getName());
      try {
        uri = new URI(uriString);
      } catch (URISyntaxException e) {
        throw new MojoExecutionException(uriString + " is not a valid uri", e);
      }
    } else {
      URI schemaURI = schemaFile.getAbsoluteFile().toURI();
      uri = getCatalogLocation().relativize(schemaURI);
    }
    // endregion

    // region write schema element
    Element uriSuffixE = getDocument().createElementNS("urn:oasis:names:tc:entity:xmlns:xml:catalog", "system");
    uriSuffixE.setAttribute("systemId", systemId);
    uriSuffixE.setAttribute("uri", uri.toASCIIString());
    getElement().appendChild(uriSuffixE);
    // endregion

    if (log.isDebugEnabled() || isVerbose()) {
      log.info("add catalog entry: <system systemId=\"{}\" uri=\"{}\" />", systemId, uri);
    }

  }

}
