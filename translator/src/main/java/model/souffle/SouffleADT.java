package model.souffle;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;

public class SouffleADT extends SouffleStmt {
  public String typeName;
  public String defaultBranch;
  public HashMap<String, List<Pair<String, String>>> fields = new HashMap<>();

  public SouffleADT(String typeName, String defaultBranch) {
    this.typeName = typeName;
    this.defaultBranch = defaultBranch;
  }

  public void addBranch(String label, List<Pair<String, String>> fields) {
    if (fields.size() == 0) return;
    this.fields.put(label, fields);
  }

  public String toString() {
    String typeDecl = String.format(".type %s ", typeName);
    String formatBlank = new String(new char[typeDecl.length()]).replace('\0', ' ');
    StringBuilder sourceCode =
        new StringBuilder(String.format("%s= %s {}\n", typeDecl, defaultBranch));
    for (String label : this.fields.keySet()) {
      StringBuilder branchCode = new StringBuilder(String.format("%s| %s {", formatBlank, label));
      List<Pair<String, String>> fields = this.fields.get(label);
      for (int i = 0; i < fields.size(); i++) {
        if (i != 0) branchCode.append(", ");
        branchCode.append(fields.get(i).getKey()).append(": ").append(fields.get(i).getValue());
      }
      branchCode.append("}\n");
      sourceCode.append(branchCode);
    }
    return sourceCode.toString();
  }
}
