import model.SharedStatus;
import models.Baseline;
import text_based.TextBased;
import utils.FileUtil;

import java.util.List;
import java.util.Map;

public class BaselineMain {
  public static String framework = "cpython";
  public static String projectName = "pytorch";
  public static String projectDir =
      System.getProperty("user.home") + "/coding/xll-gt/" + framework + "/projects/" + projectName;
  public static String outputDir =
      System.getProperty("user.dir") + "/baseline/src/main/resources/text_based_output";
  public static String ourResults =
      System.getProperty("user.home")
          + "/coding/dlData/"
          + framework
          + "/"
          + projectName
          + "/outputs/AggregatedInfo.csv";

  public static void main(String[] args) {
    Baseline baseline = new GroundTruth(framework, projectName, projectDir, outputDir);
    SharedStatus.initProjectInfo(framework, projectDir);
    Map<String, List<String>> categorizedFiles =
        FileUtil.listFilePathsInLanguages(
            SharedStatus.projectInfo.projectDir, SharedStatus.projectInfo.languages);

    baseline.run(categorizedFiles);
    if (ourResults != null && !ourResults.isEmpty()) {
      baseline.validate(ourResults);
    }
  }
}
