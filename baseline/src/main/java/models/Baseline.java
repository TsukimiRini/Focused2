package models;

import java.util.List;
import java.util.Map;

public abstract class Baseline {
  public String framework = "android";
  public String projectName = "CloudReader";
  public String projectDir =
          System.getProperty("user.home") + "/coding/xll/android/" + projectName;

  public Baseline(String framework, String projectName, String projectDir) {
    this.framework = framework;
    this.projectName = projectName;
    this.projectDir = projectDir;
  }
  public void run(Map<String, List<String>> categorizedFiles){

  }
}
