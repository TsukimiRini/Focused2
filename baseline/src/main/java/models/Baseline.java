package models;

import com.google.common.collect.Multiset;
import com.opencsv.exceptions.CsvException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class Baseline {
  public static final Logger logger = LoggerFactory.getLogger(Baseline.class);
  public String framework = "android";
  public String projectName = "CloudReader";
  public String projectDir = System.getProperty("user.home") + "/coding/xll/android/" + projectName;
  public String outputDir = System.getProperty("user.dir") + "/baseline/main/resources/";
  public String outputPath = outputDir + "/" + framework + "/" + projectName;

  static {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
  }

  public Baseline(String framework, String projectName, String projectDir, String outputDir) {
    this.framework = framework;
    this.projectName = projectName;
    this.projectDir = projectDir;
    this.outputDir = outputDir;
  }

  public Multiset<?> readInFocusedOutput(String fileName) {
    return null;
  }

  public void run(Map<String, List<String>> categorizedFiles) {}

  // validate and output results
  public void validate(String resPath) throws CsvException {}
}
