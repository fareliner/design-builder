<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements. See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership. The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License. You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied. See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:jaxb="http://java.sun.com/xml/ns/jaxb" xmlns:xsd="http://www.w3.org/2001/XMLSchema"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0">

  <xsl:output method="xml" encoding="utf-8" omit-xml-declaration="no" indent="yes"/>

  <!-- process the schemaLocation and flatten the file using a custom extension -->
  <xsl:template match="//jaxb:bindings[@scd and not(@if-exists)]">
    <xsl:copy>
      <xsl:attribute name="if-exists">true</xsl:attribute>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="//jaxb:bindings[@scd='~tns:null']/@scd">
    <xsl:attribute name="scd">
      <xsl:call-template name="stripPackageFromClassName">
        <xsl:with-param name="className" select="ancestor::node()/jaxb:class/@ref"/>
      </xsl:call-template>
    </xsl:attribute>
    <xsl:text>&#10;</xsl:text>
    <xsl:comment>
      <xsl:value-of select="'found ~tns:null in generated episode.'"/>
    </xsl:comment>
  </xsl:template>

  <!-- Identity template for copying everything else -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template name="stripPackageFromClassName">
    <xsl:param name="className"/>
    <!-- we assume its always a type -->
    <xsl:param name="xsdType" select="'~tns'"/>
    <!-- delim for package separation is always . -->
    <xsl:param name="delimiter" select="'.'"/>
    <xsl:choose>
      <xsl:when test="contains($className, $delimiter)">
        <!-- need to strip futher -->
        <xsl:call-template name="stripPackageFromClassName">
          <xsl:with-param name="className" select="substring-after($className, $delimiter)"/>
          <xsl:with-param name="xsdType" select="$xsdType"/>
          <xsl:with-param name="delimiter" select="$delimiter"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <!-- stripped all package names -->
        <xsl:value-of select="concat($xsdType, ':', $className)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
