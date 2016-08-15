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

package io.fares.maven.plugins.design.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.net.URL;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.impl.ArtifactResolver;

import org.apache.xml.resolver.CatalogManager;

@Named("mavenCatalogResolverFactory")
@Component(role = MavenCatalogResolverFactory.class)
public class MavenCatalogResolverFactory implements Service {

    private final Logger log = LoggerFactory.getLogger(MavenCatalogResolverFactory.class);

    @Requirement
    private ArtifactResolver artifactResolver;

    public MavenCatalogResolver newInstance(RepositorySystemSession session, List<URL> catalogURLs) {
        // create a catalog manager from discovered catalog files
        final CatalogManager catalogManager = new CatalogManager();
        catalogManager.setIgnoreMissingProperties(true);
        if (log.isDebugEnabled()) {
            catalogManager.setVerbosity(9);
        }

        catalogManager.setUseStaticCatalog(false);
        catalogManager.setIgnoreMissingProperties(true);

        // need to prep the manager with resolved catalog URLs
        boolean firstEntry = true;
        StringBuilder catB = new StringBuilder();
        for (URL catalogURL : catalogURLs) {
            if (!firstEntry) {
                catB.append(';');
            }
            catB.append(catalogURL.toExternalForm());
            firstEntry = false;
        }
        catalogManager.setCatalogFiles(catB.toString());

        return new MavenCatalogResolver(catalogManager, session, artifactResolver);
    }

    @Override
    public void initService(ServiceLocator locator) {
    }

}
