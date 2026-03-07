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

package io.fares.maven.plugins.design.builder.flattener;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

/**
 * Resolves import/include links through the configured catalog resolver.
 */
public class CatalogUriResolver {

  private static final Logger log = LoggerFactory.getLogger(CatalogUriResolver.class);

  private final CatalogResolver resolver;

  public CatalogUriResolver(CatalogResolver resolver) {
    this.resolver = Objects.requireNonNull(resolver, "catalog resolver cannot be null");
  }

  public String resolve(String namespace, String base, String location) {
    try {
      InputSource source = resolver.resolveEntity(namespace, location);
      if (source != null) {
        return new URL(source.getSystemId()).toExternalForm();
      }
    } catch (Exception e) {
      log.warn("Failed to resolve entity systemId {}", location, e);
    }

    Catalog catalog = resolver.getCatalog();

    try {
      String result = catalog.resolveURI(location);
      if (result != null) {
        return result;
      }
    } catch (Exception e) {
      log.warn("Failed to resolve uri {}", location, e);
    }

    try {
      String publicId = namespace != null ? namespace : location;
      String systemId = namespace != null ? location : null;
      String result = catalog.resolvePublic(publicId, systemId);
      if (result != null) {
        return result;
      }
    } catch (Exception e) {
      log.debug("Failed to resolve uri {}", location, e);
    }

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
        return uri.toString();
      }
    }

    if (log.isWarnEnabled()) {
      log.warn("Unable to resolve publicId={} systemId={}", namespace, location);
    }

    return null;
  }

}
