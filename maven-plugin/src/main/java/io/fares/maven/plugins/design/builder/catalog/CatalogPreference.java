package io.fares.maven.plugins.design.builder.catalog;

public enum CatalogPreference {

  PUBLIC("public"),
  SYSTEM("system");

  private final String value;

  CatalogPreference(String value) {
    this.value = value;
  }


  public static CatalogPreference fromValue(String v) {
    for (CatalogPreference type : CatalogPreference.values()) {
      if (type.value.equalsIgnoreCase(v)) {
        return type;
      }
    }
    throw new IllegalArgumentException("catalog preference [" + v + "] is invalid");
  }

  public String value() {
    return value;
  }

}
