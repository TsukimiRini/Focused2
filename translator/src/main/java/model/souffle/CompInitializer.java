package model.souffle;

public class CompInitializer extends SouffleStmt {
  public String compName, instanceName;

  public CompInitializer(String instanceName, String compName) {
    this.compName = compName;
    this.instanceName = instanceName;
  }

  public String toString() {
    return ".init " + instanceName + " = " + compName;
  }
}
