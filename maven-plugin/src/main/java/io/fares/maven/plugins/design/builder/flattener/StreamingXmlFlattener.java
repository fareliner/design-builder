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

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Flattens XML imports/includes while preserving comments and whitespace by streaming events.
 */
public class StreamingXmlFlattener {

  private final CatalogUriResolver resolver;

  private final SimpleNameCrawlerListener fileMapper;

  private final XmlLinkRules linkRules = new XmlLinkRules();

  private final XMLInputFactory inputFactory;

  private final XMLOutputFactory outputFactory;

  private final XMLEventFactory eventFactory;

  private final Map<URL, File> crawled = new HashMap<>();

  public StreamingXmlFlattener(CatalogUriResolver resolver,
                               SimpleNameCrawlerListener fileMapper) {
    this.resolver = resolver;
    this.fileMapper = fileMapper;
    this.inputFactory = XMLInputFactory.newFactory();
    this.outputFactory = XMLOutputFactory.newFactory();
    this.eventFactory = XMLEventFactory.newFactory();

    this.inputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
    this.inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
    this.inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
    this.inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
  }

  public File crawl(URL sourceUrl) throws IOException {
    File existing = crawled.get(sourceUrl);
    if (existing != null) {
      return existing;
    }

    try (InputStream raw = openInputStream(sourceUrl);
         InputStream in = new BufferedInputStream(raw)) {
      XMLEventReader reader = inputFactory.createXMLEventReader(sourceUrl.toExternalForm(), in);
      List<XMLEvent> preamble = new ArrayList<>();
      Deque<QName> path = new ArrayDeque<>();
      File currentOutputFile = null;
      XMLEventWriter writer = null;

      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();

        if (currentOutputFile == null) {
          preamble.add(event);
          if (event.isStartElement()) {
            StartElement root = event.asStartElement();
            String extension = linkRules.extensionForRoot(root.getName());
            currentOutputFile = fileMapper.toFile(sourceUrl, extension);
            if (currentOutputFile.getParentFile() != null && !currentOutputFile.getParentFile().exists()) {
              boolean created = currentOutputFile.getParentFile().mkdirs();
              if (!created) {
                throw new IOException("Failed to create output directory " + currentOutputFile.getParentFile().getAbsolutePath());
              }
            }
            crawled.put(sourceUrl, currentOutputFile);
            writer = outputFactory.createXMLEventWriter(new FileOutputStream(currentOutputFile), "UTF-8");
            for (XMLEvent buffered : preamble) {
              XMLEvent toWrite = buffered;
              if (buffered.isStartElement()) {
                toWrite = processStartElement(buffered.asStartElement(), path, sourceUrl, currentOutputFile);
              }
              writer.add(toWrite);
            }
            preamble.clear();
          }
          continue;
        }

        if (event.isStartElement()) {
          StartElement startElement = processStartElement(event.asStartElement(), path, sourceUrl, currentOutputFile);
          writer.add(startElement);
          continue;
        }

        if (event.isEndElement() && !path.isEmpty()) {
          path.removeLast();
        }

        writer.add(event);
      }

      if (writer != null) {
        writer.flush();
        writer.close();
      } else {
        throw new IOException("Input source does not contain any XML start element: " + sourceUrl.toExternalForm());
      }

      return currentOutputFile;
    } catch (XMLStreamException e) {
      throw new IOException("Failed to flatten XML " + sourceUrl.toExternalForm(), e);
    }
  }

  private StartElement processStartElement(StartElement startElement,
                                           Deque<QName> path,
                                           URL currentDocumentUrl,
                                           File currentOutputFile) throws IOException {
    path.addLast(startElement.getName());
    XmlLinkRules.LinkReference linkReference = linkRules.detect(path, startElement);
    if (linkReference == null) {
      return startElement;
    }

    String resolvedLocation = resolveAbsoluteLink(
      linkReference.namespace,
      currentDocumentUrl,
      linkReference.location);
    if (resolvedLocation == null || resolvedLocation.trim().isEmpty()) {
      return startElement;
    }

    URL linkedUrl = toURL(resolvedLocation);
    File linkedFile = crawled.get(linkedUrl);
    if (linkedFile == null && fileMapper.doCrawl(linkedUrl)) {
      linkedFile = crawl(linkedUrl);
    }

    String replacement = resolvedLocation;
    if (linkedFile != null) {
      replacement = relativize(currentOutputFile.getParentFile(), linkedFile);
    }

    return replaceAttribute(startElement, linkReference.locationAttribute, replacement);
  }

  private StartElement replaceAttribute(StartElement startElement,
                                        QName attributeName,
                                        String replacement) {
    List<Attribute> rewritten = new ArrayList<>();
    Iterator<?> attributes = startElement.getAttributes();
    while (attributes.hasNext()) {
      Attribute attribute = (Attribute) attributes.next();
      QName name = attribute.getName();
      if (attributeName.getLocalPart().equals(name.getLocalPart())
        && ( attributeName.getNamespaceURI().isEmpty()
        || attributeName.getNamespaceURI().equals(name.getNamespaceURI()) )) {
        String ns = name.getNamespaceURI() == null ? "" : name.getNamespaceURI();
        String prefix = name.getPrefix() == null ? "" : name.getPrefix();
        rewritten.add(eventFactory.createAttribute(prefix, ns, name.getLocalPart(), replacement));
      } else {
        rewritten.add(attribute);
      }
    }

    return eventFactory.createStartElement(
      startElement.getName().getPrefix(),
      startElement.getName().getNamespaceURI(),
      startElement.getName().getLocalPart(),
      rewritten.iterator(),
      startElement.getNamespaces(),
      startElement.getNamespaceContext()
    );
  }

  private String resolveAbsoluteLink(String namespace, URL currentDocumentUrl, String location) {
    String resolved = resolver.resolve(namespace, currentDocumentUrl.toExternalForm(), location);
    if (resolved != null) {
      return resolved;
    }
    try {
      URI baseUri = currentDocumentUrl.toURI();
      return baseUri.resolve(location).toString();
    } catch (URISyntaxException e) {
      return location;
    }
  }

  private URL toURL(String value) throws MalformedURLException {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return new URL(value.trim());
    } catch (MalformedURLException ignore) {
      return new File(value.trim()).toURI().toURL();
    }
  }

  private InputStream openInputStream(URL url) throws IOException {
    if ("file".equalsIgnoreCase(url.getProtocol())) {
      try {
        return new FileInputStream(new File(url.toURI()));
      } catch (URISyntaxException e) {
        return url.openStream();
      }
    }
    return url.openStream();
  }

  private String relativize(File fromDir, File toFile) {
    Path from = fromDir.toPath();
    Path to = toFile.toPath();
    String relative = from.relativize(to).toString();
    return relative.replace(File.separatorChar, '/');
  }

}
