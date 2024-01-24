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

package io.fares.design.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.tools.CatalogResolver;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import jlibs.xml.sax.crawl.XMLCrawler;

/**
 * Implements the "non-standard" JLibs resolver interface delegating through to a configured SAX @{@link EntityResolver}.
 */
public class JlibsResolverBridge implements XMLCrawler.Resolver {

  private static final Logger log = LoggerFactory.getLogger(JlibsResolverBridge.class);

  private CatalogResolver resolver;

  public JlibsResolverBridge(CatalogResolver resolver) {
    this.resolver = Objects.requireNonNull(resolver);
  }

  @Override
  public String resolve(String namespace, String base, String location) {

    try {
      InputSource source = resolver.resolveEntity(namespace, location);
      if (source != null) {
        return new URL(source.getSystemId()).toExternalForm();
      }
    } catch (Exception ignore) {
      log.warn("Failed to resolve entity systemId " + location, ignore);
    }

    Catalog catalog = resolver.getCatalog();

    try {
      String result = catalog.resolveURI(location);
      if (result != null) {
        return result;
      }
    } catch (Exception ignore) {
      log.warn("Failed to resolve uri " + location, ignore);
    }

    try {
      // check if we have both, if not the publicId will whatever is in location
      String publicId = namespace != null ? namespace : location;
      String systemId = namespace != null ? location : null;
      String result = catalog.resolvePublic(publicId, systemId);
      if (result != null) {
        return result;
      }
    } catch (Exception ignore) {
      log.debug("Failed to resolve uri " + location, ignore);
    }

    // last change file based import relative to the parent
    if (base != null && location != null) {
      String relativeResource;
      if (location.lastIndexOf('/') > -1 && location.lastIndexOf('/') < location.length()) {
        relativeResource = location.substring(location.lastIndexOf('/') + 1);
      } else {
        relativeResource = location;
      }
      URI uri = URI.create(base).resolve(relativeResource);
      if ("file".equals(uri.getScheme())) {
        File f = new File(uri);
        if (f.exists() && f.isFile()) {
          return f.getAbsolutePath();
        }
      } else {
        // blindly trust, could be a related resource on the web, no way to check
        return uri.toString();
      }
    }

    if (log.isWarnEnabled())
      log.warn("Unable to resolve publicId={} systemId={}", namespace, location);

    return null;

  }

  public EntityResolver getResolver() {
    return resolver;
  }

}
