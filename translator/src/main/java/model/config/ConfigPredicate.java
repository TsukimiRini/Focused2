package model.config;

import model.enums.LogicRelationType;
import org.apache.commons.lang3.tuple.Pair;
import utils.MatcherUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static utils.StringUtil.capitalize;

public class ConfigPredicate {
  public String predicateName;
  public List<ConfigPredicate> params = new ArrayList<>();
  public LogicRelationType logicType = LogicRelationType.NONE;
  public int logicDepth = 0, logicOps = 0;
  // TODO: fully support not logic
  public boolean not = false;

  public ConfigPredicate(String predicateName, List<String> paraNames) {
    setUp(predicateName, paraNames);
  }

  public ConfigPredicate(String source) {
    if (source.startsWith("!")) {
      source = source.substring(1);
      not = true;
    }
    Pair<String, List<String>> parseRes = MatcherUtils.parsePredicate(source);
    if (parseRes == null) {
      predicateName = source;
      params = null;
    } else setUp(parseRes.getKey(), parseRes.getValue());
  }

  public void setUp(String predicateName, List<String> paraNames) {
    this.predicateName = predicateName;
    for (String paraName : paraNames) {
      params.add(new ConfigPredicate(paraName));
    }
  }

  public void setLogic(LogicRelationType type, int logicDepth, int logicOps) {
    this.logicType = type;
    this.logicDepth = logicDepth;
    this.logicOps = logicOps;
  }

  public void replaceLayerAndCaps(
      Map<String, List<String>> layers, Map<String, List<String>> caps) {
    for (ConfigPredicate param : params) {
      if (param.params == null) {
        Pair<String, Pair<Boolean, String>> layerOrCap =
            MatcherUtils.parseURIRefAttrInConfig(param.predicateName);
        if (layerOrCap == null) continue;
        if (layerOrCap.getRight().getKey()) {
          if (!caps.containsKey(layerOrCap.getLeft()))
            caps.put(layerOrCap.getLeft(), new ArrayList<>());
          caps.get(layerOrCap.getLeft()).add(layerOrCap.getRight().getRight());
          param.predicateName =
              "cap"
                  + capitalize(layerOrCap.getLeft())
                  + capitalize(layerOrCap.getRight().getRight());
        } else {
          if (!layers.containsKey(layerOrCap.getLeft()))
            layers.put(layerOrCap.getLeft(), new ArrayList<>());
          layers.get(layerOrCap.getLeft()).add(layerOrCap.getRight().getRight());
          param.predicateName =
              layerOrCap.getRight().getRight().startsWith("branches")
                  ? capitalize(layerOrCap.getLeft())
                      + "_"
                      + capitalize(layerOrCap.getRight().getRight()).replace("[", "").replace("]", "")
                  : "attr"
                      + capitalize(layerOrCap.getLeft())
                      + capitalize(layerOrCap.getRight().getRight());
        }
      } else {
        param.replaceLayerAndCaps(layers, caps);
      }
    }
  }

  public String toString() {
    // assign predicate or match predicate: reduced to equal constraint
    if (predicateName.equals("assign") || predicateName.equals("match")) {
      if (params == null || params.size() != 2) {
        throw new IllegalArgumentException("assign predicate should accept 2 params");
      }
      return params.get(0) + (not ? "!=" : "=") + params.get(1);
    }
    StringBuilder sb = new StringBuilder((not ? "!" : "") + predicateName);
    if (params != null) {
      sb.append("(").append(params.get(0));
      for (int i = 1; i < params.size(); i++) {
        sb.append(", ").append(params.get(i));
      }
      sb.append(")");
    }
    return sb.toString();
  }
}
