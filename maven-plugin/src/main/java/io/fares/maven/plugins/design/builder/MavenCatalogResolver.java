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

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Objects;

import static java.text.MessageFormat.format;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import io.fares.maven.plugins.design.builder.flattener.DependencyResource;
import io.fares.maven.plugins.design.builder.flattener.ResourceEntry;

/**
 * A {@link CatalogResolver} that is capable to convert a <code>maven:</code>
 * protocol based URI string into a system resolvable absolute URI. It can also
 * handle <code>classpath:</code> type resource strings.
 */
public final class MavenCatalogResolver extends CatalogResolver {

    public static final String URI_SCHEME_MAVEN = "maven";
    public static final String URI_SCHEME_CLASSPATH = "classpath";
    Logger log = LoggerFactory.getLogger(MavenCatalogResolver.class);
    private RepositorySystemSession repositorySystemSession;

    private ArtifactResolver artifactResolver;

    /**
     * The {@link ClassLoader} used to resolve a <code>classpath:</code>
     * resource URI.
     */
    private ClassLoader classLoader;

    // FIXME move classloader construction to here
    public MavenCatalogResolver() {

    }


    /**
     * Construct a JlibsResolverBridge that is capable of resolving resources inside
     * Maven a list of {@link Dependency}.
     *
     * @param catalogManager          The {@link CatalogManager} to be used to resolve any entity
     *                                requests
     * @param repositorySystemSession the repository session to use by this resolver
     * @param artifactResolver        the aether resolver use for artifact resolution
     */
    public MavenCatalogResolver(CatalogManager catalogManager,
                                RepositorySystemSession repositorySystemSession,
                                ArtifactResolver artifactResolver) {
        this(catalogManager, repositorySystemSession, artifactResolver, null);
    }

    /**
     * Construct a JlibsResolverBridge that is capable of resolving resources inside
     * Maven a list of {@link Dependency}.
     *
     * @param catalogManager          The {@link CatalogManager} to be used to resolve any entity
     *                                requests
     * @param repositorySystemSession the repository session to use by this resolver
     * @param artifactResolver        the aether resolver use for artifact resolution
     * @param classloader             the classloader to use to resolve (FIXME resolve what?)
     */
    public MavenCatalogResolver(CatalogManager catalogManager,
                                RepositorySystemSession repositorySystemSession,
                                ArtifactResolver artifactResolver,
                                ClassLoader classloader) {

        // catalog gets initialized
        super(Objects.requireNonNull(catalogManager, "The catalog manager must not be null."));
        this.artifactResolver = Objects.requireNonNull(artifactResolver, "Artifact resolver must not be null.");
        this.repositorySystemSession = Objects.requireNonNull(repositorySystemSession, "Repository system session must not be null.");

        if (classloader != null) {
            this.classLoader = classloader;
        } else {
            this.classLoader = Thread.currentThread().getContextClassLoader();
        }

    }

    @Override
    public String getResolvedEntity(String publicId, String systemId) {

        // We rely on the parent to resolve the provided args in the catalogs it
        // has available, if nothing is in the catalog, chances are we will not
        // find it elsewhere.

        // FIXME what about a systemId that is perfectly valid, eg.
        // classpath:META-INF/bla.xml

        final String result = super.getResolvedEntity(publicId, systemId);

        if (result == null) {
            return null;
        }

        // having resolved the resource in catalog, we an now actually try to
        // turn it into a readable URI.
        try {
            final URI uri = new URI(result);
            // now see if we can resolve the protocol
            if (URI_SCHEME_MAVEN.equals(uri.getScheme())) {
                final String schemeSpecificPart = uri.getSchemeSpecificPart();
                return resolveMavenEntity(schemeSpecificPart);
            }
            if (URI_SCHEME_CLASSPATH.equals(uri.getScheme())) {
                final String schemeSpecificPart = uri.getSchemeSpecificPart();
                return resolveClasspathEntity(schemeSpecificPart);
            } else {
                return result;
            }
        } catch (URISyntaxException urisex) {
            getCatalog().getCatalogManager().debug.message(1, format("Error creating uri for [{0}].", result));
            return result;
        }
    }

    private String resolveMavenEntity(String schemeSpecificPart) {

        try {
            final DependencyResource dependencyResource = DependencyResource
                    .valueOf(schemeSpecificPart);

            // TODO check if we need remote repos here !
            Artifact artifact = new DefaultArtifact(dependencyResource.getGroupId(), dependencyResource.getArtifactId(), dependencyResource.getClassifier(), dependencyResource.getType(), dependencyResource.getVersion());
            ArtifactRequest request = new ArtifactRequest(artifact, null, null);

            ArtifactResult result = artifactResolver.resolveArtifact(repositorySystemSession, request);
            artifact = result.getArtifact();
            final URL resourceURL = createArtifactResourceUrl(artifact, schemeSpecificPart);
            if (log.isDebugEnabled())
                log.debug("Resolved dependency resource [{}] to resource URL [{}].", dependencyResource, resourceURL);
            return resourceURL.toExternalForm();

        } catch (IllegalArgumentException iaex) {
            getCatalog().getCatalogManager().debug.message(1, format("Error parsing dependency descriptor [{0}]: {1}",
                    schemeSpecificPart, iaex.getMessage()));
        } catch (ArtifactResolutionException e) {
            // TODO review logging
            getCatalog().getCatalogManager().debug.message(1, format("Failed to resolve [{0}].",
                    schemeSpecificPart));
        } catch (Exception ex) {
            // TODO review logging
            getCatalog().getCatalogManager().debug.message(1, format("Error resolving dependency resource [{0}].",
                    schemeSpecificPart));
        }

        // if everything does not work just return empty handed
        return null;
    }

    /**
     * This will create the actual resource URL for the artifact to use.
     * <p>
     * FIXME same logic to resolve resource (artifact + embedded resource) as {@link io.fares.maven.plugins.design.builder.flattener.FlattenImportPathMojo#createResourceEntryUrls(ResourceEntry, String, String[], String[])}
     *
     * @param artifact
     * @param resource
     * @return
     * @throws MojoExecutionException
     */
    private URL createArtifactResourceUrl(final Artifact artifact,
                                          String resource) throws MojoExecutionException {

        final File artifactFile = artifact.getFile();

        // try filesystem:
        if (artifactFile.isDirectory()) {
            final File resourceFile = new File(artifactFile, resource);
            try {
                return resourceFile.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new MojoExecutionException(format("Could not create an URL for dependency directory [{0}] and resource [{1}].", artifactFile, resource), e);
            }
        }

        // try jar:
        try {
            return new URL(format("jar:{0}!/{1}", artifactFile.toURI().toURL().toExternalForm(), resource));
        } catch (MalformedURLException e) {
            throw new MojoExecutionException(format("Could not create an URL for dependency file [{0}] and resource [{1}].", artifactFile, resource));
        }

        // TODO try classpath:


    }

    /**
     * TODO check out the cxf implementation <a href=
     * "https://github.com/apache/cxf/blob/master/core/src/main/java/org/apache/cxf/resource/URIResolver.java"
     * >URIResolver.java</a>, a much better approach.
     *
     * @param schemeSpecificPart the classpath string such as
     *                           <code>"classpath:META-INF/catalog.xml"</code>
     * @return the actual resource location on the classpath
     */
    private String resolveClasspathEntity(String schemeSpecificPart) {
        final URL resource = getClassLoader().getResource(schemeSpecificPart);
        if (resource == null) {
            return null;
        } else {
            return resource.toString();
        }
    }


    private void validateSession() {
        if (repositorySystemSession == null) {
            throw new IllegalStateException("no maven session was provided to maven catalog resolver");
        }
    }

    public RepositorySystemSession getRepositorySystemSession() {
        return repositorySystemSession;
    }

    public void setRepositorySystemSession(RepositorySystemSession repositorySystemSession) {
        this.repositorySystemSession = repositorySystemSession;
    }

    /**
     * @return The classloader that is used by this resolver to resolve the
     * provided resource if it is a <code>classpath:</code> protocol
     * resource.
     */
    public ClassLoader getClassLoader() {
        // need to set asn inital classloader first
        if (this.classLoader == null) {
            getCatalog().getCatalogManager().debug.message(1, format("No classloader has been provided to [{0}], defaulting to Thead ContextClassLoader.", getClass().getSimpleName()));
            // NOTE this may not work reliably under certain circumstances so
            // advided to set the ClassLoader specifically.
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


}
