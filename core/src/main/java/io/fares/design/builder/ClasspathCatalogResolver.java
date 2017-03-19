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

package io.fares.design.builder;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static java.text.MessageFormat.format;

import org.apache.xml.resolver.tools.CatalogResolver;

/**
 * A {@link CatalogResolver} that can resolve <code>classpath:</code> type URI
 * resources.
 * <p>
 * Note: This class was heavily borrowed from the jnet XJC plugin and extended
 * to make the classloader configurable.
 */
public class ClasspathCatalogResolver extends CatalogResolver {

  public static final String URI_SCHEME_CLASSPATH = "classpath";

  /**
   * The {@link ClassLoader} used to resolve a <code>classpath:</code>
   * resource URI.
   */
  private ClassLoader classLoader;

  public ClasspathCatalogResolver() {
    super();
  }

  public ClasspathCatalogResolver(ClassLoader classLoader) {
    super();
    this.classLoader = classLoader;
  }

  /**
   * @return The classloader that is used by this resolver to resolve the
   * provided resource if it is a <code>classpath:</code> protocol
   * resource.
   */
  public ClassLoader getClassLoader() {
    // need to set asn inital classloader first
    if (this.classLoader == null) {
      // NOTE this may not work reliably under certain circumstances so
      // advised to set the ClassLoader specifically.
      this.classLoader = Thread.currentThread().getContextClassLoader();
    }
    return this.classLoader;
  }

  /**
   * Set the classloader used to resolve <code>classpath:</code> specified
   * resource strings.
   *
   * @param classLoader The classloader to be used for <code>classpath:</code> URI
   *                    resolution.
   */
  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public String getResolvedEntity(String publicId, String systemId) {
    final String result = super.getResolvedEntity(publicId, systemId);

    if (result == null) {
      System.err.println(format(
        "Could not resolve publicId [{0}], systemId [{1}]",
        publicId, systemId));
      return null;
    }

    try {
      final URI uri = new URI(result);
      if (URI_SCHEME_CLASSPATH.equals(uri.getScheme())) {
        final String schemeSpecificPart = uri.getSchemeSpecificPart();
        final URL resource = getClassLoader().getResource(
          schemeSpecificPart);
        if (resource == null) {
          return null;
        } else {
          return resource.toString();
        }
      } else {
        return result;
      }
    } catch (URISyntaxException urisex) {
      return result;
    }
  }
}
