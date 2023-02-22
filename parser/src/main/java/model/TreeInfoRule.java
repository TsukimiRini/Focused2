package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TreeInfoRule extends HashMap<String, List<String>> {
  public String label;
  public TreeInfoRuleType ruleType;
  public List<String> nodeTypes = new ArrayList<>();
  public TreeInfoConf conf;

  public TreeInfoRule(String label, TreeInfoConf conf) {
    this.label = label;
    this.conf = conf;
    if (label.contains("/")) {
      ruleType = TreeInfoRuleType.EDGE;
    } else {
      ruleType = TreeInfoRuleType.NODE;
    }
  }

  public TreeInfoRule(String label, TreeInfoConf conf, boolean isDefault) {
    this(label, conf);

    if (isDefault) {
      put("name", Arrays.asList("this"));
    }
  }

  public void addAttribute(String key, String value) {
    String[] splitValue = value.split("\\|");
    List<String> valGroup = new ArrayList<>();
    for (String val : splitValue) {
      processAndRecord(val, valGroup);
    }
    if (valGroup.isEmpty()) {
      throw new IllegalArgumentException("illegal value in uriTree conf");
    }
    put(key, valGroup);
  }

  private void processAndRecord(String value, List<String> valGroup) {
    value = value.strip();
    if (value.startsWith("_")) {
      valGroup.addAll(conf.nodeVariable.get(value));
      nodeTypes.addAll(conf.nodeVariable.get(value));
    } else {
      valGroup.add(value);
      if (!value.startsWith("\"")) {
        nodeTypes.add(value);
      }
    }
  }
}
