package model.souffle;

import java.util.ArrayList;
import java.util.List;

public class Component extends SouffleStmt {
  public String label, instanceName;
  public List<String> baseComp = new ArrayList<>();
  public List<String> innerStmts = new ArrayList<>();
  public List<SouffleStmt> postfixStmts = new ArrayList<>();

  public Component(String label, List<String> baseComp) {
    this.label = label;
    this.instanceName = label.toLowerCase() + "Instance";
    this.baseComp.addAll(baseComp);

    postfixStmts.add(new CompInitializer(this.instanceName, label));
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(".comp ");
    sb.append(label);
    if (!baseComp.isEmpty()) {
      sb.append(" : ");
      for (int i = 0; i < baseComp.size(); i++) {
        if (i != 0) sb.append(", ");
        sb.append(baseComp.get(i));
      }
    }
    sb.append(" {\n");

    for (String stmt : innerStmts) {
      sb.append("    ").append(stmt).append("\n");
    }
    sb.append("}\n");

    for (SouffleStmt stmt : postfixStmts) {
      sb.append(stmt).append("\n");
    }

    return sb.toString();
  }
}
