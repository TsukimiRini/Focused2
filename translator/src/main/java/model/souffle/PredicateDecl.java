package model.souffle;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class PredicateDecl extends SouffleStmt {
  public String predicateName;
  List<Pair<String, String>> params = new ArrayList<>();

  public PredicateDecl(String predicateName, String paramName, String paramType) {
    this.predicateName = predicateName;
    addParam(paramName, paramType);
  }

  public PredicateDecl(String predicateName) {
    this.predicateName = predicateName;
  }

  public void addParam(String paramName, String paramType) {
    params.add(Pair.of(paramName, paramType));
  }

  public String toString() {
    StringBuilder paramsDecl = new StringBuilder();
    for (int i = 0; i < params.size(); i++) {
      if (i != 0) paramsDecl.append(", ");
      paramsDecl.append(params.get(i).getKey()).append(":").append(params.get(i).getValue());
    }
    return String.format(".decl %s(%s)", predicateName, paramsDecl);
  }
}
