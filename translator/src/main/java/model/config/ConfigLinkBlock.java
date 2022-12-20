package model.config;

import model.enums.PredicateType;
import org.apache.commons.lang3.tuple.Pair;
import utils.MatcherUtils;

import java.util.*;

public class ConfigLinkBlock {
  public String blockName;
  public List<ConfigLinkBlock> baseBlocks = new ArrayList<>();
  public Map<String, List<ConfigLink>> predicateDecls = new HashMap<>();
  public Map<String, PredicateType> predicateTypes = new HashMap<>();
  public Map<String, List<String>> paramTypes = new HashMap<>();

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
    Map<String, List<String>> baseBlockPredicates =
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

  private void getParaTypesFromDeclStmt(
      List<String> elementPredicates,
      Map<String, List<String>> baseBlockPredicates,
      Set<String> visited,
      String predicateName) {
    if (visited.contains(predicateName)) return;
    visited.add(predicateName);
    List<String> curParamTypes = new ArrayList<>();
    for (int i = 0; i < predicateDecls.get(predicateName).get(0).decl.params.size(); i++)
      curParamTypes.add("");
    for (ConfigLink link : predicateDecls.get(predicateName)) {
      List<String> paramNames = link.decl.params;
      for (ConfigPredicate condition : link.conditions) {
        boolean isElementPredicate = false, isBaseBlockPredicate = false, isInnerPredicate = false;
        String conditionName = condition.predicateName;
        if (elementPredicates.contains(conditionName)) isElementPredicate = true;
        else if (baseBlockPredicates.containsKey(conditionName)) isBaseBlockPredicate = true;
        else if (predicateDecls.containsKey(conditionName)) isInnerPredicate = true;
        for (int i = 0; i < condition.params.size(); i++) {
          String param = condition.params.get(i);
          Pair<Boolean, String> parseAsAttr = MatcherUtils.parseURIRefAttrInConfig(param);
          param = parseAsAttr.getRight();
          int idxInDecl = paramNames.indexOf(param);
          if (idxInDecl == -1 || curParamTypes.get(idxInDecl).length() != 0) continue;
          else if (parseAsAttr.getKey()) {
            curParamTypes.set(idxInDecl, "URI");
          } else if (isElementPredicate) {
            curParamTypes.set(idxInDecl, "URI");
          } else if (isBaseBlockPredicate) {
            curParamTypes.set(idxInDecl, baseBlockPredicates.get(conditionName).get(i));
          } else if (isInnerPredicate) {
            getParaTypesFromDeclStmt(
                elementPredicates, baseBlockPredicates, visited, conditionName);
            if (paramTypes.containsKey(conditionName)) {
              curParamTypes.set(idxInDecl, paramTypes.get(conditionName).get(i));
            }
          } else {
            curParamTypes.set(idxInDecl, "symbol");
          }
        }
      }
    }
    curParamTypes.replaceAll(
        ele -> {
          if (ele.length() == 0) return "symbol";
          return ele;
        });
    paramTypes.put(predicateName, curParamTypes);
  }
}
