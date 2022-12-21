package model.config;

import model.enums.PredicateType;
import utils.MatcherUtils;

import java.util.*;

public class ConfigLinkBlock {
  public String blockName;
  public List<ConfigLinkBlock> baseBlocks = new ArrayList<>();
  public Map<String, List<ConfigLink>> predicateDecls = new HashMap<>();
  public Map<String, PredicateType> predicateTypes = new HashMap<>();
  public Map<String, List<Set<String>>> paramTypes = new HashMap<>();

  public ConfigLinkBlock(String name) {
    this.blockName = name;
  }

  public void addBaseBlock(ConfigLinkBlock block) {
    baseBlocks.add(block);
  }

  public void addPredicateDecl(ConfigLink link, boolean isPublic) {
    String predicateName = link.decl.predicateName;
    if (!predicateDecls.containsKey(predicateName)) {
      predicateDecls.put(predicateName, new ArrayList<>());
      predicateTypes.put(predicateName, isPublic ? PredicateType.PUBLIC : PredicateType.PRIVATE);
    }
    predicateDecls.get(predicateName).add(link);
  }

  public void getParaTypes(List<String> elementPredicates) {
    Map<String, List<Set<String>>> baseBlockPredicates =
        baseBlocks.stream()
            .reduce(
                new HashMap<>(),
                (map, block) -> {
                  map.putAll(block.paramTypes);
                  return map;
                },
                (a, b) -> null);

    Set<String> visited = new HashSet<>();
    for (String predicateName : predicateDecls.keySet()) {
      getParaTypesFromDeclStmt(elementPredicates, baseBlockPredicates, visited, predicateName);
    }
  }

  // TODO: refer para types completely
  private void getParaTypesFromDeclStmt(
      List<String> elementPredicates,
      Map<String, List<Set<String>>> baseBlockPredicates,
      Set<String> visited,
      String predicateName) {
    if (visited.contains(predicateName)) return;
    visited.add(predicateName);
    List<Set<String>> curParamTypes = new ArrayList<>();
    for (int i = 0; i < predicateDecls.get(predicateName).get(0).decl.params.size(); i++)
      curParamTypes.add(new HashSet<>());
    for (ConfigLink link : predicateDecls.get(predicateName)) {
      List<ConfigPredicate> paramNames = link.decl.params;
      Map<String, Set<String>> varTypes = link.varTypes;
      for (ConfigPredicate condition : link.conditions) {
        boolean isElementPredicate = false, isBaseBlockPredicate = false, isInnerPredicate = false;
        String conditionName = condition.predicateName;
        if (elementPredicates.contains(conditionName)) isElementPredicate = true;
        else if (baseBlockPredicates.containsKey(conditionName)) isBaseBlockPredicate = true;
        else if (predicateDecls.containsKey(conditionName)) isInnerPredicate = true;
        for (int i = 0; i < condition.params.size(); i++) {
          String param = condition.params.get(i).toString();
          //          Pair<Boolean, String> parseAsAttr =
          // MatcherUtils.parseURIRefAttrInConfig(param);
          //          if (parseAsAttr.getKey()) param = parseAsAttr.getRight();
          if (!MatcherUtils.matchVariable(param)) continue;
          int idxInDecl = indexOfParamInPredicateList(paramNames, param);
          if (!varTypes.containsKey(param)) varTypes.put(param, new HashSet<>());
          if (isElementPredicate) {
            varTypes.get(param).add(conditionName);
          } else if (isBaseBlockPredicate) {
            varTypes.get(param).addAll(baseBlockPredicates.get(conditionName).get(i));
          } else if (isInnerPredicate) {
            getParaTypesFromDeclStmt(
                elementPredicates, baseBlockPredicates, visited, conditionName);
            if (paramTypes.containsKey(conditionName)) {
              varTypes.get(param).addAll(paramTypes.get(conditionName).get(i));
            }
          }
          if (idxInDecl != -1) curParamTypes.get(idxInDecl).addAll(varTypes.get(param));
          //          else {
          //            Set<String> curParamSet = curParamTypes.get(idxInDecl);
          //            if (curParamSet.size() > 1
          //                || !curParamSet.isEmpty() && !curParamSet.toArray()[0].equals("symbol"))
          // {
          //              throw new IllegalArgumentException("ambiguous type");
          //            }
          //            curParamTypes.get(idxInDecl).add("symbol");
          //          }
        }
      }
    }
    //    curParamTypes.forEach(
    //        ele -> {
    //          if (ele.size() == 0) ele.add("symbol");
    //        });
    paramTypes.put(predicateName, curParamTypes);
  }

  private int indexOfParamInPredicateList(List<ConfigPredicate> predicates, String param) {
    for (int i = 0; i < predicates.size(); i++) {
      if (predicates.get(i).toString().equals(param)) return i;
    }
    return -1;
  }
}
