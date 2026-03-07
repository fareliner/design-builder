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
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates XML import/include link rules for flattening.
 */
final class XmlLinkRules {

  private static final String INCLUDE = "include";

  private static final String IMPORT = "import";

  private static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";

  private static final String NS_WSDL = "http://schemas.xmlsoap.org/wsdl/";

  private static final String NS_XSL = "http://www.w3.org/1999/XSL/Transform";

  private static final QName QN_SCHEMA_ELEMENT = new QName(NS_XSD, "schema");

  private static final QName QN_SCHEMA_IMPORT = new QName(NS_XSD, IMPORT);

  private static final QName QN_SCHEMA_INCLUDE = new QName(NS_XSD, INCLUDE);

  private static final QName QN_WSDL_ELEMENT = new QName(NS_WSDL, "definitions");

  private static final QName QN_WSDL_TYPES = new QName(NS_WSDL, "types");

  private static final QName QN_WSDL_IMPORT = new QName(NS_WSDL, IMPORT);

  private static final QName QN_WSDL_INCLUDE = new QName(NS_WSDL, INCLUDE);

  private static final QName QN_XSL_ELEMENT = new QName(NS_XSL, "stylesheet");

  private static final QName QN_XSL_IMPORT = new QName(NS_XSL, IMPORT);

  private static final QName QN_XSL_INCLUDE = new QName(NS_XSL, INCLUDE);

  private static final QName ATTR_NAMESPACE = new QName("namespace");

  private static final QName ATTR_SCHEMA_LOCATION = new QName("schemaLocation");

  private static final QName ATTR_LOCATION = new QName("location");

  private static final QName ATTR_HREF = new QName("href");

  private final List<LinkRule> rules = Arrays.asList(
    new LinkRule(new QName[]{ QN_SCHEMA_ELEMENT, QN_SCHEMA_IMPORT }, ATTR_NAMESPACE, ATTR_SCHEMA_LOCATION),
    new LinkRule(new QName[]{ QN_SCHEMA_ELEMENT, QN_SCHEMA_INCLUDE }, null, ATTR_SCHEMA_LOCATION),
    new LinkRule(new QName[]{ QN_WSDL_ELEMENT, QN_WSDL_IMPORT }, ATTR_NAMESPACE, ATTR_LOCATION),
    new LinkRule(new QName[]{ QN_WSDL_ELEMENT, QN_WSDL_INCLUDE }, null, ATTR_LOCATION),
    new LinkRule(new QName[]{ QN_WSDL_ELEMENT, QN_WSDL_TYPES, QN_SCHEMA_ELEMENT, QN_SCHEMA_IMPORT }, ATTR_NAMESPACE, ATTR_SCHEMA_LOCATION),
    new LinkRule(new QName[]{ QN_WSDL_ELEMENT, QN_WSDL_TYPES, QN_SCHEMA_ELEMENT, QN_SCHEMA_INCLUDE }, null, ATTR_SCHEMA_LOCATION),
    new LinkRule(new QName[]{ QN_XSL_ELEMENT, QN_XSL_IMPORT }, null, ATTR_HREF),
    new LinkRule(new QName[]{ QN_XSL_ELEMENT, QN_XSL_INCLUDE }, null, ATTR_HREF)
  );

  String extensionForRoot(QName root) {
    if (QN_SCHEMA_ELEMENT.equals(root)) {
      return "xsd";
    }
    if (QN_WSDL_ELEMENT.equals(root)) {
      return "wsdl";
    }
    if (QN_XSL_ELEMENT.equals(root)) {
      return "xsl";
    }
    return "xml";
  }

  LinkReference detect(Deque<QName> path, StartElement startElement) {
    LinkRule rule = findRule(path);
    if (rule == null) {
      return null;
    }

    Attribute locationAttr = findAttribute(startElement, rule.locationAttribute);
    if (locationAttr == null) {
      return null;
    }

    String location = locationAttr.getValue();
    if (location == null || location.trim().isEmpty()) {
      return null;
    }

    String namespace = null;
    if (rule.namespaceAttribute != null) {
      Attribute namespaceAttr = findAttribute(startElement, rule.namespaceAttribute);
      if (namespaceAttr != null) {
        namespace = namespaceAttr.getValue();
      }
    }

    return new LinkReference(namespace, location, rule.locationAttribute);
  }

  private LinkRule findRule(Deque<QName> path) {
    if (path.isEmpty()) {
      return null;
    }
    for (LinkRule rule : rules) {
      if (rule.matches(path)) {
        return rule;
      }
    }
    return null;
  }

  private Attribute findAttribute(StartElement element, QName attrName) {
    Iterator<?> attributes = element.getAttributes();
    while (attributes.hasNext()) {
      Attribute attribute = (Attribute) attributes.next();
      QName name = attribute.getName();
      if (attrName.getLocalPart().equals(name.getLocalPart())) {
        String expectedNamespace = attrName.getNamespaceURI();
        String namespace = name.getNamespaceURI();
        if (expectedNamespace == null || expectedNamespace.isEmpty()
          || expectedNamespace.equals(namespace)) {
          return attribute;
        }
      }
    }
    return null;
  }

  static final class LinkReference {

    final String namespace;

    final String location;

    final QName locationAttribute;

    private LinkReference(String namespace, String location, QName locationAttribute) {
      this.namespace = namespace;
      this.location = location;
      this.locationAttribute = locationAttribute;
    }
  }

  private static final class LinkRule {

    private final List<QName> path;

    private final QName namespaceAttribute;

    private final QName locationAttribute;

    private LinkRule(QName[] path, QName namespaceAttribute, QName locationAttribute) {
      this.path = new ArrayList<>(Arrays.asList(path));
      this.namespaceAttribute = namespaceAttribute;
      this.locationAttribute = locationAttribute;
    }

    private boolean matches(Deque<QName> candidatePath) {
      if (candidatePath.size() != path.size()) {
        return false;
      }
      int index = 0;
      for (QName qName : candidatePath) {
        QName expected = path.get(index++);
        if (!expected.getLocalPart().equals(qName.getLocalPart())) {
          return false;
        }
        if (!expected.getNamespaceURI().equals(qName.getNamespaceURI())) {
          return false;
        }
      }
      return true;
    }
  }
}
