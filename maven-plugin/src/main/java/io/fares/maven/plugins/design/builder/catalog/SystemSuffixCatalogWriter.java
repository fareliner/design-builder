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

import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;

public class SystemSuffixCatalogWriter extends FileByFileCatalogWriter {

  private SystemSuffixEntry option;

  SystemSuffixCatalogWriter(SystemSuffixEntry option) throws ParserConfigurationException {
    this.option = option;
  }

  @Override
  public void doWrite(File schemaFile) {

    URI schemaURI = schemaFile.getAbsoluteFile().toURI();

    URI schemaLocationURI = schemaURI.getPath().endsWith("/")
      ? schemaURI.resolve("../")
      : schemaURI.resolve(".");

    // TODO add some validation to check that the catalog and sources are in the same tree (e.g. cannot create a catalog outside jar for XSDs in jar etc.)

    if (log.isDebugEnabled() || isVerbose()) {
      log.info("   - schema [" + schemaURI.getPath() + ']');
    }

    // calculate natural offset between schema and catalog
    // int naturalOffset = catalogLocationURI.compareTo(schemaURI);

    URI systemIdSuffixURI = null;

    if (option.getPathOffset() == 0) {
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
      for (int i = 0; i < option.getPathOffset(); i++) {
        sb.append("../");
      }
      sb.append('.');
      systemIdSuffixURI = schemaLocationURI.resolve(sb.toString())
        .relativize(schemaURI);
    }

    URI schemaToCatalogRelativeURI = getCatalogLocation().relativize(schemaURI);

    // region write schema element
    Element uriSuffixE = getDocument().createElementNS("urn:oasis:names:tc:entity:xmlns:xml:catalog", "systemSuffix");
    uriSuffixE.setAttribute("systemIdSuffix", systemIdSuffixURI.toString());
    uriSuffixE.setAttribute("uri", schemaToCatalogRelativeURI.toString());
    getElement().appendChild(uriSuffixE);
    // endregion

    if (log.isDebugEnabled() || isVerbose()) {
      log.info("add catalog entry: <systemSuffix systemIdSuffix=\"{}\" uri=\"{}\" />",
        systemIdSuffixURI.toString(),
        schemaToCatalogRelativeURI.toString()
      );
    }

  }

}
