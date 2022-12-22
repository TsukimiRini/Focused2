package model.config;

import model.URIPattern;
import model.enums.LogicRelationType;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static utils.StringUtil.capitalize;

public class ConfigLink {
  public ConfigPredicate decl;
  public List<ConfigPredicate> conditions = new ArrayList<>();
  public Map<String, Set<String>> varTypes = new HashMap<>();

  public ConfigLink(String declPredicate, List<String> paras) {
    decl = new ConfigPredicate(declPredicate, paras);
  }

  public ConfigLink(String declStmt) {
    decl = new ConfigPredicate(declStmt);
  }

  public void addCondition(
      String conditionPredicate,
      List<String> paras,
      LogicRelationType logic,
      int logicDepth,
      int logicOps) {
    ConfigPredicate newCondition = new ConfigPredicate(conditionPredicate, paras);
    newCondition.setLogic(logic, logicDepth, logicOps);
    conditions.add(newCondition);
  }

  public void addCondition(
      String predicateSource, LogicRelationType logic, int logicDepth, int logicOps) {
    ConfigPredicate newCondition = new ConfigPredicate(predicateSource);
    newCondition.setLogic(logic, logicDepth, logicOps);
    conditions.add(newCondition);
  }

  public String toSouffleCode(Map<String, URIPattern> patterns) {
    Map<String, List<String>> variableToLayer = new HashMap<>(), variableToCap = new HashMap<>();
    StringBuilder sb = new StringBuilder(decl.toString());
    sb.append(" :- ");
    Stack<Integer> depthStack = new Stack<>();
    for (ConfigPredicate predicate : conditions) {
      predicate.replaceLayerAndCaps(variableToLayer, variableToCap);
      int foreground = predicate.logicDepth - predicate.logicOps + 1,
          background = predicate.logicDepth;
      LogicRelationType curOp = predicate.logicType;
      while (!depthStack.isEmpty() && depthStack.peek() > foreground) {
        if ((background - depthStack.peek()) % 2 == 0) {
          sb.append(curOp == LogicRelationType.AND ? "" : " )");
        } else {
          sb.append(curOp == LogicRelationType.AND ? " )" : "");
        }
        depthStack.pop();
      }
      if (!depthStack.isEmpty() && depthStack.peek() == foreground) {
        if ((background - foreground) % 2 == 0) {
          sb.append(curOp == LogicRelationType.AND ? ", " : " ; ");
        } else {
          sb.append(curOp == LogicRelationType.AND ? " ; " : ", ");
        }
        foreground++;
      }
      for (int i = foreground; i <= background; i++) {
        if ((background - i) % 2 == 0) {
          sb.append(curOp == LogicRelationType.AND ? "" : "( ");
        } else {
          sb.append(curOp == LogicRelationType.AND ? "( " : "");
        }
        depthStack.push(i);
      }
      sb.append(predicate);
    }
    while (!depthStack.isEmpty()) {
      int layer = depthStack.pop();
      int base = conditions.get(0).logicDepth;
      LogicRelationType baseOp = conditions.get(0).logicType;
      if (abs(layer - base) % 2 == 0) {
        sb.append(baseOp == LogicRelationType.AND ? "" : " )");
      } else {
        sb.append(baseOp == LogicRelationType.AND ? " )" : "");
      }
    }

    addDefForVars(sb, variableToLayer, variableToCap, patterns);

    sb.append(".");
    return sb.toString();
  }

  private void addDefForVars(
      StringBuilder sb,
      Map<String, List<String>> variableToLayer,
      Map<String, List<String>> variableToCap,
      Map<String, URIPattern> patterns) {
    Set<String> varNames = new HashSet<>(variableToLayer.keySet());
    varNames.addAll(variableToCap.keySet());
    for (String varName : varNames) {
      Set<String> typesForVar = varTypes.get(varName);
      if (typesForVar == null || typesForVar.isEmpty()) continue;
      List<URIPattern> patternsForTypes = new ArrayList<>();
      typesForVar.forEach(s -> patternsForTypes.add(patterns.get(s)));
      String defStmt =
          getDefForEachVar(
              varName, variableToLayer.get(varName), variableToCap.get(varName), patternsForTypes);
      sb.append(", ").append(defStmt);
    }
  }

  private String getDefForEachVar(
      String varName, List<String> layers, List<String> caps, List<URIPattern> types) {
    List<String> defStmts =
        types.stream()
            .map(type -> getDefForEachVarAndType(varName, layers, caps, type))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    assert defStmts.size() > 0;
    if (defStmts.size() == 1) {
      return defStmts.get(0);
    } else {
      StringBuilder sb = new StringBuilder("( ");
      sb.append(defStmts.get(0));
      for (int i = 1; i < defStmts.size(); i++) {
        sb.append(", ").append(defStmts.get(i));
      }
      sb.append(" )");
      return sb.toString();
    }
  }

  private String getDefForEachVarAndType(
      String varName, List<String> layers, List<String> caps, URIPattern type) {
    if ((layers == null || layers.isEmpty()) && (caps == null || caps.isEmpty())) return null;
    StringBuilder sb = new StringBuilder(varName);
    sb.append("=[");
    String[] layerList = {"lang", "file", "element", "branches"};
    for (int i = 0; i < layerList.length; i++) {
      if (i != 0) sb.append(",");
      if (layers == null || !layers.contains(layerList[i])) {
        sb.append("_");
      }
      sb.append("attr").append(capitalize(varName)).append(capitalize(layerList[i]));
    }
    if (caps == null || caps.isEmpty()) {
      sb.append(",_cap").append(varName);
    } else {
      sb.append(",$").append(type.label).append("Cap(");
      List<String> capList = new ArrayList<>(type.captures);
      for (int i = 0; i < capList.size(); i++) {
        if (i != 0) sb.append(",");
        if (!caps.contains(capList.get(i))) {
          sb.append("_");
        }
        sb.append("cap").append(capitalize(varName)).append(capitalize(capList.get(i)));
      }
      sb.append(")");
    }
    sb.append("]");
    return sb.toString();
  }
}
