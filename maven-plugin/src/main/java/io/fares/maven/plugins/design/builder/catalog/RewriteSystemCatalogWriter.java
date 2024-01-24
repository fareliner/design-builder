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

import java.io.File;
import java.net.URI;

import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;

public class RewriteSystemCatalogWriter extends AbstractCatalogWriter {

  private RewriteSystemOption option;

  RewriteSystemCatalogWriter(RewriteSystemOption option) throws ParserConfigurationException {
    this.option = option;
  }

  RewriteSystemCatalogWriter(RewriteSystemOption option, URI catalogLocation) throws ParserConfigurationException {
    super(catalogLocation);
    this.option = option;
  }

  @Override
  public void write(Element catalogElement, File... schemaFiles) throws MojoExecutionException {

    // region write schema element
    Element uriSuffixE = catalogElement.getOwnerDocument().createElementNS("urn:oasis:names:tc:entity:xmlns:xml:catalog", "rewriteSystem");
    uriSuffixE.setAttribute("systemIdStartString", option.getSystemIdStartString());
    uriSuffixE.setAttribute("rewritePrefix", option.getRewritePrefix());
    catalogElement.appendChild(uriSuffixE);
    // endregion

    if (log.isDebugEnabled() || isVerbose()) {
      log.info("add catalog entry: <rewriteSystem systemIdStartString=\"{}\" rewritePrefix=\"{}\" />",
        option.getSystemIdStartString(),
        option.getRewritePrefix()
      );
    }

  }

}
