package model;

import model.Language;
import utils.FileUtil;

import java.util.HashSet;
import java.util.Set;

public class ProjectInfo {
  public String framework;
  public Set<Language> languages = new HashSet<>();
  public String projectDir;

  public ProjectInfo(String framework, String projectDir){
    this.framework = framework;
    switch(framework){
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
      default:
        addLanguage(Language.JAVA);
    }
    this.projectDir = projectDir;
    FileUtil.setRootPath(projectDir);
  }

  private void addLanguage(Language lang){
    this.languages.add(lang);
  }
}
