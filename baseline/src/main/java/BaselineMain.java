import model.SharedStatus;
import models.Baseline;
import text_based.TextBased;
import utils.FileUtil;

import java.util.List;
import java.util.Map;

public class BaselineMain {
  public static String framework = "android";
  public static String projectName = "CloudReader";
  public static String projectDir = "D:\\Projects\\XLL\\" + projectName;
  public static String outputDir = "D:\\Projects\\Focused2\\baseline\\src\\main\\resources\\text_based_output";

  public static void main(String[] args) {
    Baseline baseline = new TextBased(framework, projectName, projectDir, outputDir);
    SharedStatus.initProjectInfo(framework, projectDir);
    Map<String, List<String>> categorizedFiles =
        FileUtil.listFilePathsInLanguages(
            SharedStatus.projectInfo.projectDir, SharedStatus.projectInfo.languages);

    baseline.run(categorizedFiles);
  }
}
