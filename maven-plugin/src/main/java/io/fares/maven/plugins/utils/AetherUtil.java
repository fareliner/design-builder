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

package io.fares.maven.plugins.utils;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.*;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.building.*;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultProxySelector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AetherUtil {

  public static final String USER_HOME = System.getProperty("user.home");

  public static final File USER_MAVEN_CONFIGURATION_HOME = new File(USER_HOME, ".m2");

  public static final File DEFAULT_USER_SETTINGS_FILE = new File(USER_MAVEN_CONFIGURATION_HOME, "settings.xml");

  private static final DefaultSettingsBuilderFactory settingsBuilderFactory = new DefaultSettingsBuilderFactory();

  private static File resolveFile(File file, String workingDirectory) {
    return resolveFile(file, new File(workingDirectory));
  }

  private static File resolveFile(File file, File workingDirectory) {

    if (file == null) {
      return null;
    } else if (file.isAbsolute()) {
      return file;
    } else if (file.getPath().startsWith(File.separator)) {
      // drive-relative Windows path
      return file.getAbsoluteFile();
    } else {
      return new File(workingDirectory, file.getPath()).getAbsoluteFile();
    }

  }

  public static ProxySelector newProxySelector(File workingDirectory) throws MojoExecutionException {

    DefaultProxySelector pselector = new DefaultProxySelector();

    File settingsFile = resolveFile(DEFAULT_USER_SETTINGS_FILE, workingDirectory);
    Settings settings = readSettings();
    List<org.eclipse.aether.repository.Proxy> result = new ArrayList<>();
    for (org.apache.maven.settings.Proxy proxy : settings.getProxies()) {
      if (proxy != null) {
        AuthenticationBuilder authBuilder = new AuthenticationBuilder();
        authBuilder.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
        pselector.add(new org.eclipse.aether.repository.Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authBuilder.build()), null);
      }
    }

    return pselector;

  }

  public static Settings readSettings(File userSettingsFile) throws MojoExecutionException {

    SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
    settingsRequest.setUserSettingsFile(userSettingsFile);
    SettingsBuilder settingsBuilder = settingsBuilderFactory.newInstance();
    SettingsBuildingResult settingsResult = null;

    try {
      settingsResult = settingsBuilder.build(settingsRequest);
    } catch (SettingsBuildingException e) {
      throw new MojoExecutionException("failed to read user settings", e);
    }

    return settingsResult.getEffectiveSettings();

  }

  public static Settings readSettings() throws MojoExecutionException {
    return readSettings(DEFAULT_USER_SETTINGS_FILE);
  }

  public static MavenExecutionRequest newExecutionRequestFromSettings(File localRepositoryPath)
    throws Exception {
    return newExecutionRequestFromSettings(readSettings(), localRepositoryPath);
  }

  public static MavenExecutionRequest newExecutionRequestFromSettings(Settings settings, File localRepositoryPath)
    throws MavenExecutionRequestPopulationException {

    MavenExecutionRequest request = new DefaultMavenExecutionRequest();

    if (settings == null) {
      return request;
    }

    request.setOffline(settings.isOffline());
    request.setInteractiveMode(settings.isInteractiveMode());
    request.setPluginGroups(settings.getPluginGroups());

    if (localRepositoryPath == null) {
      request.setLocalRepositoryPath(settings.getLocalRepository());
    } else {
      request.setLocalRepositoryPath(localRepositoryPath);
    }

    for (Server server : settings.getServers()) {
      server = server.clone();
      request.addServer(server);
    }

    for (Proxy proxy : settings.getProxies()) {
      if (!proxy.isActive()) {
        continue;
      }
      proxy = proxy.clone();
      request.addProxy(proxy);
    }

    for (Mirror mirror : settings.getMirrors()) {
      mirror = mirror.clone();
      request.addMirror(mirror);
    }

    request.setActiveProfiles(settings.getActiveProfiles());

    for (org.apache.maven.settings.Profile rawProfile : settings.getProfiles()) {

      request.addProfile(SettingsUtils.convertFromSettingsProfile(rawProfile));

      if (settings.getActiveProfiles().contains(rawProfile.getId())) {

        List<Repository> remoteRepositories = rawProfile.getRepositories();
        for (Repository remoteRepository : remoteRepositories) {
          try {
            request.addRemoteRepository(
              MavenRepositorySystem.buildArtifactRepository(remoteRepository));
          } catch (InvalidRepositoryException e) {
            // do nothing for now
          }
        }

        List<Repository> pluginRepositories = rawProfile.getPluginRepositories();
        for (Repository pluginRepository : pluginRepositories) {
          try {
            request.addPluginArtifactRepository(
              MavenRepositorySystem.buildArtifactRepository(pluginRepository));
          } catch (InvalidRepositoryException e) {
            // do nothing for now
          }
        }

      }
    }

    return request;

  }

}
