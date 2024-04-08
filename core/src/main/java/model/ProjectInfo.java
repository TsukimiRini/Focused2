package model;

import utils.FileUtil;

import java.util.HashSet;
import java.util.Set;

public class ProjectInfo {
  public String framework;
  public Set<Language> languages = new HashSet<>();
  public String projectDir;
  public String configPath;

  public ProjectInfo(String framework, String projectDir) {
    this.framework = framework;
    configPath =
        System.getProperty("user.dir")
            + "/translator/src/main/resources/"
            + framework
            + "_config.fcs";
    switch (framework) {
      case "android":
        addLanguage(Language.JAVA);
        addLanguage(Language.XML);
        break;
      case "springmvc":
        addLanguage(Language.JAVA);
        addLanguage(Language.HTML);
        addLanguage(Language.JSP);
        break;
      case "mybatis":
        addLanguage(Language.JAVA);
        addLanguage(Language.XML);
        addLanguage(Language.SQL);
        break;
      case "web":
        addLanguage(Language.HTML);
        addLanguage(Language.CSS);
        break;
      case "cpython":
        addLanguage(Language.Python);
        addLanguage(Language.CLike);
        addLanguage(Language.C);
        addLanguage(Language.H);
        addLanguage(Language.CC);
        addLanguage(Language.CPP);
        addLanguage(Language.HPP);
      case "rust":
        addLanguage(Language.Rust);
        break;
      default:
        addLanguage(Language.JAVA);
    }
    this.projectDir = projectDir;
    FileUtil.setRootPath(projectDir);
  }

  private void addLanguage(Language lang) {
    this.languages.add(lang);
  }
}
