package model;

import model.ProjectInfo;

public class SharedStatus {
  public static ProjectInfo projectInfo = null;

  public static void initProjectInfo(String framework, String projectDir, String projectName){
    projectInfo = new ProjectInfo(framework, projectDir, projectName);
  }
}
