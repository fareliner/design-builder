package io.fares.maven.plugins.design.builder.catalog;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.net.URL;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PublicCatalogWriterTest {

  @Mock
  Element catalogElement;

  @Test
  void itShouldSkipNonNamespaceSchema() throws ParserConfigurationException, MojoExecutionException {
    PublicCatalogWriter w = new PublicCatalogWriter(new PublicOption());
    URL resourceURL = getClass().getResource("/unit/catalog/public/no-target-namespace/GlobalDataTypes.xsd");
    File schemaFile = new File(resourceURL.getFile());
    w.doWrite(catalogElement, schemaFile);
    verifyNoInteractions(catalogElement);
  }

}
