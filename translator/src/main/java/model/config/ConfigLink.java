package model.config;

import model.enums.LogicRelationType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ConfigLink {
  public ConfigPredicate decl;
  public List<ConfigPredicate> conditions = new ArrayList<>();

  public ConfigLink(String declPredicate, List<String> paras) {
    decl = new ConfigPredicate(declPredicate, paras);
  }

  public ConfigLink(String declStmt) {
    decl = new ConfigPredicate(declStmt);
  }

  public void addCondition(
      String conditionPredicate, List<String> paras, LogicRelationType logic, int logicDepth) {
    ConfigPredicate newCondition = new ConfigPredicate(conditionPredicate, paras);
    newCondition.setLogic(logic, logicDepth);
    conditions.add(newCondition);
  }

  public void addCondition(String predicateSource, LogicRelationType logic, int logicDepth) {
    ConfigPredicate newCondition = new ConfigPredicate(predicateSource);
    newCondition.setLogic(logic, logicDepth);
    conditions.add(newCondition);
  }
}
