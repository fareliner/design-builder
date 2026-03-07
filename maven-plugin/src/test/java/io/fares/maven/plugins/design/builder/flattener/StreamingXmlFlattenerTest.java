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

import org.apache.xml.resolver.tools.CatalogResolver;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StreamingXmlFlattenerTest {

  @Test
  public void testFlattenPreservesCommentsAndWhitespace() throws Exception {
    Path tempDir = Files.createTempDirectory("streaming-xml-flattener");
    Path sourceDir = Files.createDirectories(tempDir.resolve("src"));
    Path outputDir = Files.createDirectories(tempDir.resolve("out"));

    Path wsdl = sourceDir.resolve("service.wsdl");
    Path xsd = sourceDir.resolve("schema.xsd");

    Files.write(wsdl, (
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<!--root comment-->\n" +
        "<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
        "  <wsdl:types>\n" +
        "    <!--types comment-->\n" +
        "    <xsd:schema targetNamespace=\"urn:test\">\n" +
        "      <xsd:import namespace=\"urn:test\" schemaLocation=\"schema.xsd\"/>\n" +
        "    </xsd:schema>\n" +
        "  </wsdl:types>\n" +
        "</wsdl:definitions>\n"
    ).getBytes(StandardCharsets.UTF_8));

    Files.write(xsd, (
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\">\n" +
        "  <!--schema comment-->\n" +
        "  <xsd:element name=\"e\" type=\"xsd:string\"/>\n" +
        "</xsd:schema>\n"
    ).getBytes(StandardCharsets.UTF_8));

    CatalogUriResolver resolver = new CatalogUriResolver(new CatalogResolver()) {
      @Override
      public String resolve(String namespace, String base, String location) {
        return null;
      }
    };

    StreamingXmlFlattener flattener = new StreamingXmlFlattener(
      resolver,
      new SimpleNameCrawlerListener(outputDir.toFile(), true)
    );

    File rootOutput = flattener.crawl(wsdl.toUri().toURL());
    assertNotNull(rootOutput);
    assertTrue(rootOutput.exists());

    File importedOutput = outputDir.resolve("schema.xsd").toFile();
    assertTrue(importedOutput.exists());

    String flattenedWsdl = new String(Files.readAllBytes(rootOutput.toPath()), StandardCharsets.UTF_8);
    assertTrue(flattenedWsdl.contains("<!--root comment-->"));
    assertTrue(flattenedWsdl.contains("<!--types comment-->"));
    assertTrue(flattenedWsdl.contains("  <wsdl:types>"));
    assertTrue(flattenedWsdl.contains("schemaLocation=\"schema.xsd\""));

    String flattenedXsd = new String(Files.readAllBytes(importedOutput.toPath()), StandardCharsets.UTF_8);
    assertTrue(flattenedXsd.contains("<!--schema comment-->"));
    assertTrue(flattenedXsd.contains("\n  <xsd:element"));
    assertTrue(flattenedXsd.contains("type=\"xsd:string\""));
  }

  @Test
  public void testFlattenRelativizesNestedImport() throws Exception {
    Path tempDir = Files.createTempDirectory("streaming-xml-relativize");
    Path sourceDir = Files.createDirectories(tempDir.resolve("src").resolve("sub"));
    Path outputDir = Files.createDirectories(tempDir.resolve("out"));

    Path rootSchema = sourceDir.resolve("root.xsd");
    Path nestedSchema = sourceDir.resolve("nested.xsd");

    Files.write(rootSchema, (
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\">\n" +
        "  <xsd:include schemaLocation=\"nested.xsd\"/>\n" +
        "</xsd:schema>\n"
    ).getBytes(StandardCharsets.UTF_8));

    Files.write(nestedSchema, (
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"urn:test\"/>\n"
    ).getBytes(StandardCharsets.UTF_8));

    CatalogUriResolver resolver = new CatalogUriResolver(new CatalogResolver()) {
      @Override
      public String resolve(String namespace, String base, String location) {
        return null;
      }
    };

    StreamingXmlFlattener flattener = new StreamingXmlFlattener(
      resolver,
      new SimpleNameCrawlerListener(outputDir.toFile(), true)
    );

    URL source = rootSchema.toUri().toURL();
    File flattened = flattener.crawl(source);
    assertTrue(flattened.exists());

    String xml = new String(Files.readAllBytes(flattened.toPath()), StandardCharsets.UTF_8);
    assertTrue(xml.contains("schemaLocation=\"nested.xsd\""));

    assertEquals(outputDir.resolve("root.xsd").toFile().getAbsolutePath(), flattened.getAbsolutePath());
    assertTrue(outputDir.resolve("nested.xsd").toFile().exists());
  }
}
