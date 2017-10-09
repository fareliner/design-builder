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
import javax.xml.transform.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

import org.xmlunit.builder.Input;
import org.xmlunit.matchers.CompareMatcher;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.project.MavenProject;

public class GenerateCatalogMojoTest {

  protected final Logger log = LoggerFactory.getLogger(GenerateCatalogMojoTest.class);

  @Rule
  public MojoRule rule = new MojoRule();

  @Rule
  public TestResources resources = new TestResources("src/test/resources/unit", "target/ut/");

  @Test
  public void testSystemSuffixBasic() throws Exception {
    runTest("catalog/systemsuffix/basic");
  }

  @Test
  public void testSystemSuffixLevelTwo() throws Exception {
    runTest("catalog/systemsuffix/level-2");
  }

  @Test
  public void testSystemSuffixRelativeTarget() throws Exception {
    runTest("catalog/systemsuffix/relative-target");
  }

  @Test
  public void testSystemSuffixLevelZero() throws Exception {
    runTest("catalog/systemsuffix/level-0", "lvl-01/cat.xcat");
  }

  @Test
  public void testSystemSuffixExcludeSchema() throws Exception {
    runTest("catalog/systemsuffix/exclude-schema");
  }

  @org.junit.Ignore("Multi Execution test does not work in maven test harness")
  @Test
  public void testSystemSuffixMultiExecution() throws Exception {
    runTest("catalog/systemsuffix/execution-config");
  }


  @Test
  public void testSystemRewriteBasic() throws Exception {
    runTest("catalog/rewritesystem/basic");
  }

  @Test
  public void testSystemBasic() throws Exception {
    runTest("catalog/system/basic");
  }

  @Test
  public void testSystemMultiple() throws Exception {
    runTest("catalog/system/multi-files");
  }

  @Test
  public void testSystemDefaultSystemId() throws Exception {
    runTest("catalog/system/default-systemid");
  }

  @Test(expected = MojoExecutionException.class)
  public void testSystemNoSystemId() throws Exception {
    runTest("catalog/system/no-systemid");
  }

  @Test
  public void testTargetNamespaceAsUrn() throws Exception {
    runTest("catalog/system/tns-as-urn");
  }

  @Test
  public void testSystemTnsExtracted() throws Exception {
    runTest("catalog/system/tns-extracted");
  }

  @Test
  public void testSystemUriPrefix() throws Exception {
    runTest("catalog/system/uri-prefix");
  }

  @Test
  public void testSystemUriPrefixNamespaceOverride() throws Exception {
    runTest("catalog/system/uri-prefix-ns");
  }

  @Test
  public void testUriBasic() throws Exception {
    runTest("catalog/uri/basic");
  }

  @Test
  public void testUriMultiple() throws Exception {
    runTest("catalog/uri/multi-files");
  }

  @Test
  public void testUriTnsExtracted() throws Exception {
    runTest("catalog/uri/tns-extracted");
  }

  @Test
  public void testPublicBasic() throws Exception {
    runTest("catalog/public/basic");
  }

  @Test
  public void testPublicIdOverride() throws Exception {
    runTest("catalog/public/override-pid");
  }

  @Test
  public void testPublicSepUrnDefaultAndAppend() throws Exception {
    runTest("catalog/public/urn-append");
  }

  @Test
  public void testPublicSepOverrideWithUrnAndAppend() throws Exception {
    runTest("catalog/public/override-sep-urn-append");
  }

  @Test
  public void testPublicIdAppendFileOverride() throws Exception {
    runTest("catalog/public/override-pid-append");
  }

  @Test
  public void testPublicSeparatorOverrideAndAppend() throws Exception {
    runTest("catalog/public/override-sep-append");
  }

  @Test
  public void testPublicPublicIdSeparatorOverrideAndAppend() throws Exception {
    runTest("catalog/public/override-pid-sep-append");
  }


  @Test
  public void testMulti() throws Exception {
    runTest("catalog/mixed/basic");
  }

  private void runTest(String projectPath) throws Exception {
    runTest(projectPath, "catalog.xml");
  }


  private void runTest(String projectPath, String catalogName) throws Exception {

    File baseDir = resources.getBasedir(projectPath);
    MavenProject project = rule.readMavenProject(baseDir);
    GenerateCatalogMojo mojo = (GenerateCatalogMojo) rule.lookupConfiguredMojo(project, "catalog");
    mojo.execute();

    // check catalog file has been created
    assertTrue("catalog was not generated", (new File(baseDir, catalogName)).exists());

    // check it contains expected values
    Source actualCatalog = Input.fromFile(mojo.getTargetCatalogFile()).build();
    Source expectedCatalog = Input.fromFile(new File(baseDir, "expectedCatalog.xml")).build();

    assertThat(actualCatalog, CompareMatcher.isIdenticalTo(expectedCatalog));

  }

}
