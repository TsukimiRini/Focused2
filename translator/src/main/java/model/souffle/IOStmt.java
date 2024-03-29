package model.souffle;

public class IOStmt extends SouffleStmt {
  public boolean isOutput;
  public String object;

  public IOStmt(boolean isOutput, String object) {
    this.isOutput = isOutput;
    this.object = object;
  }

  public String toString() {
    if (isOutput) return ".output " + object + "(IO=file, rfc4180=true)";
    else return ".input " + object;
  }
}
