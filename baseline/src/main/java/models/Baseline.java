package models;

import java.util.List;
import java.util.Map;

public abstract class Baseline {
  public String framework = "android";
  public String projectName = "CloudReader";
  public String projectDir = System.getProperty("user.home") + "/coding/xll/android/" + projectName;
  public String outputDir = System.getProperty("user.dir") + "/baseline/main/resources/";
  public String outputPath = outputDir + "/" + framework + "/" + projectName;

  public Baseline(String framework, String projectName, String projectDir, String outputDir) {
    this.framework = framework;
    this.projectName = projectName;
    this.projectDir = projectDir;
    this.outputDir = outputDir;
  }

  public void run(Map<String, List<String>> categorizedFiles) {}
}
