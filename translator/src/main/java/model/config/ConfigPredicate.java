package model.config;

import model.enums.LogicRelationType;
import org.apache.commons.lang3.tuple.Pair;
import utils.MatcherUtils;

import java.util.ArrayList;
import java.util.List;

import static utils.MatcherUtils.parsePredicate;

public class ConfigPredicate {
  public String predicateName;
  public List<Pair<String, String>> params = new ArrayList<>();
  public LogicRelationType logicType = LogicRelationType.NONE;
  public int logicDepth  = 0;

  public ConfigPredicate(String predicateName, List<String> paraNames) {
    setUp(predicateName, paraNames);
  }

  public ConfigPredicate(String source){
    Pair<String, List<String>> parseRes = MatcherUtils.parsePredicate(source);
    setUp(parseRes.getKey(), parseRes.getValue());
  }

  public void setUp(String predicateName, List<String> paraNames) {
    this.predicateName = predicateName;
    for (String para : paraNames) {
      params.add(Pair.of(para, null));
    }
  }

  public void setLogic(LogicRelationType type, int logicDepth){
    this.logicType = type;
    this.logicDepth = logicDepth;
  }
}
