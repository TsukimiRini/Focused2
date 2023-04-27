import model.SharedStatus;
import models.Baseline;
import text_based.TextBased;
import utils.FileUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class BaselineMain {
  public static String framework = "android";
  public static String projectName = "CloudReader";
  public static String projectDir =
          System.getProperty("user.home") + "/coding/xll/android/" + projectName;

  public static void main(String[] args) throws IOException {
    Baseline baseline = new TextBased(framework, projectName, projectDir);
    SharedStatus.initProjectInfo(framework, projectDir);
    Map<String, List<String>> categorizedFiles =
            FileUtil.listFilePathsInLanguages(
                    SharedStatus.projectInfo.projectDir, SharedStatus.projectInfo.languages);
  }
}
