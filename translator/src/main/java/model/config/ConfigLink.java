package model.config;

import model.URIPattern;
import model.enums.LogicRelationType;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static utils.StringUtil.capitalize;

public class ConfigLink {
  public ConfigPredicate decl;
  public List<ConfigPredicate> conditions = new ArrayList<>();
  public Map<String, Set<String>> varTypes = new HashMap<>();
  public ConfigLinkBlock block = null;

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
    }
    Set<ConfigPredicate> haveDefined = new HashSet<>();
    for (ConfigPredicate predicate : conditions) {
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
      if (predicate.params.size() > 0) {
        for (int idx = 0; idx < predicate.params.size(); idx++) {
          ConfigPredicate param = predicate.params.get(idx);
          if (addDefForEachVar(sb, idx, variableToLayer, variableToCap, patterns, predicate))
            haveDefined.add(param);
        }
      }
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

    //    addDefForVars(sb, variableToLayer, variableToCap, patterns);

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
      addDefForEachVar(sb, varName, variableToLayer, variableToCap, patterns);
    }
  }

  private void addDefForEachVar(
      StringBuilder sb,
      String varName,
      Map<String, List<String>> variableToLayer,
      Map<String, List<String>> variableToCap,
      Map<String, URIPattern> patterns) {
    Set<String> typesForVar = varTypes.get(varName);
    if (typesForVar == null || typesForVar.isEmpty()) return;
    List<URIPattern> patternsForTypes = new ArrayList<>();
    typesForVar.forEach(s -> patternsForTypes.add(patterns.get(s)));
    String defStmt =
        getDefForEachVar(
            varName, variableToLayer.get(varName), variableToCap.get(varName), patternsForTypes);
    sb.append(", ").append(defStmt);
  }

  private boolean addDefForEachVar(
      StringBuilder sb,
      int paramIndex,
      Map<String, List<String>> variableToLayer,
      Map<String, List<String>> variableToCap,
      Map<String, URIPattern> patterns,
      ConfigPredicate predicate) {
    String predicateName = predicate.predicateName;
    ConfigPredicate param = predicate.params.get(paramIndex);
    String varName = param.toString();
    Set<String> typesForVar = varTypes.get(varName);
    if (typesForVar == null || typesForVar.isEmpty()) return false;

    if (typesForVar.contains(predicateName)) {
      List<URIPattern> patternsForTypes = Collections.singletonList(patterns.get(predicateName));
      String defStmt =
          getDefForEachVar(
              varName, variableToLayer.get(varName), variableToCap.get(varName), patternsForTypes);
      sb.append(", ").append(defStmt);
    } else {
      List<URIPattern> patternsForTypes = new ArrayList<>();
      ConfigLink decl = block.findDeclsByPredicateName(predicateName);
      String declVarName = decl.decl.params.get(paramIndex).toString();
      Set<String> declPossibleTypes = decl.varTypes.get(declVarName);
      declPossibleTypes.forEach(s -> patternsForTypes.add(patterns.get(s)));
      String defStmt =
          getDefForEachVar(
              varName, variableToLayer.get(varName), variableToCap.get(varName), patternsForTypes);
      sb.append(", ").append(defStmt);
    }
    return true;
  }

  private String getDefForEachVar(
      String varName, List<String> layers, List<String> caps, List<URIPattern> types) {
    Set<String> defStmts =
        types.stream()
            .map(type -> getDefForEachVarAndType(varName, layers, caps, type))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    assert defStmts.size() > 0;
    List<String> stmtsList = new ArrayList<>(defStmts);
    if (stmtsList.size() == 0) {
      return null;
    }
    if (stmtsList.size() == 1) {
      return stmtsList.get(0);
    } else {
      StringBuilder sb = new StringBuilder("( ");
      sb.append(stmtsList.get(0));
      for (int i = 1; i < stmtsList.size(); i++) {
        sb.append("; ").append(stmtsList.get(i));
      }
      sb.append(" )");
      return sb.toString();
    }
  }

  private String getDefForEachVarAndType(
      String varName, List<String> layers, List<String> caps, URIPattern type) {
    //    if ((layers == null || layers.isEmpty()) && (caps == null || caps.isEmpty())) return null;
    StringBuilder sb = new StringBuilder(varName);
    sb.append("=[");
    String[] layerList = {"lang", "file", "element", "branches"};
    List<String> trimmedLayers = new ArrayList<>();
    if (layers != null) {
      for (String layer : layers) {
        String layerTrimmed = layer.replaceAll("\\[.*]", "");
        trimmedLayers.add(layerTrimmed);
      }
    }
    for (int i = 0; i < layerList.length; i++) {
      if (i != 0) sb.append(",");
      if (layerList[i].equals("branches")) {
        if (type.branches.isEmpty() && type.defaultBranches.isEmpty()
            || layers == null
            || !trimmedLayers.contains("branches")) {
          sb.append("_attr").append(capitalize(varName)).append("Br");
          continue;
        }
        sb.append("$").append(type.label).append("Br(");
        List<Integer> branchReferences = new ArrayList<>();
        for (String layer : layers) {
          if (layer.startsWith("branches")) {
            branchReferences.add(Integer.parseInt(layer.substring(9, layer.length() - 1)));
          }
        }
        AtomicInteger branchCnt = new AtomicInteger();
        type.branches.forEach((key, value) -> branchCnt.addAndGet(value.size()));
        int branchSize = branchCnt.get() + type.defaultBranches.size();
        Collections.sort(branchReferences);
        for (int j = 0; j < branchSize; j++) {
          if (j != 0) sb.append(",");
          if (branchReferences.contains(j)) {
            sb.append(capitalize(varName)).append("_Branches").append(j);
          } else {
            sb.append("_").append(capitalize(varName)).append("_Branches").append(j);
          }
        }
        sb.append(")");
      } else {
        if (!trimmedLayers.contains(layerList[i])) {
          sb.append("_");
        }
        sb.append("attr").append(capitalize(varName)).append(capitalize(layerList[i]));
      }
    }
    if (caps == null || caps.isEmpty() || type.captures.isEmpty()) {
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
