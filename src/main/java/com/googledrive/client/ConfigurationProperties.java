package com.googledrive.client;

import java.io.InputStream;
import java.util.Properties;

public class ConfigurationProperties {

  private static String fileProperties = "/app.properties";

  public synchronized static String getProperty(String pKey) {

    String result = "";

    Properties props = new Properties();
    InputStream input =
        ConfigurationProperties.class.getClassLoader().getResourceAsStream(fileProperties);
    try {
      props.load(input);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    result = props.getProperty(pKey);

    props = null;
    return result;
  }
}
