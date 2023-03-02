package model.TreeInfo;

import utils.FileUtil;

import java.util.*;

public class TreeInfoConf {
  public String confFile;
  public Map<String, List<String>> nodeVariable = new HashMap<>();
  public List<TreeInfoRule> nodeRules = new ArrayList<>();
  public List<TreeInfoRule> edgeRules = new ArrayList<>();

  public TreeInfoConf(String confFile) {
    this.confFile = confFile;
    load();
  }

  public void load() {
    List<String> content = FileUtil.readFileToLines(confFile);
    TreeInfoRule curRule = null;
    for (String line : content) {
      if (line.strip().startsWith("#")) {
        continue;
      } else if (line.contains(":")) {
        int commaIdx = line.indexOf(":");
        String key = line.substring(0, commaIdx),
            value = commaIdx == line.length() - 1 ? "" : line.substring(commaIdx + 1).strip();
        if (value.length() == 0) {
          if (curRule != null) {
            addToRuleSet(
                curRule.ruleType == TreeInfoRuleType.NODE ? nodeRules : edgeRules, curRule);
          }
          curRule = new TreeInfoRule(key, this);
        } else {
          key = key.strip();
          if (curRule == null) {
            throw new IllegalArgumentException("missing label in uriTree conf");
          }
          curRule.addAttribute(key, value);
        }
      } else if (line.contains("=")) {
        int equalIdx = line.indexOf("=");
        String left = line.substring(0, equalIdx), right = line.substring(equalIdx + 1);
        addNodeVariable(left, right);
      } else if (line.strip().length() != 0) {
        if (curRule != null) {
          addToRuleSet(curRule.ruleType == TreeInfoRuleType.NODE ? nodeRules : edgeRules, curRule);
          curRule = null;
        }
        nodeRules.add(new TreeInfoRule(line.strip(), this, true));
      }
    }
    if (curRule != null) {
      addToRuleSet(curRule.ruleType == TreeInfoRuleType.NODE ? nodeRules : edgeRules, curRule);
    }
  }

  public List<TreeInfoRule> getNodeRule(String nodeType) {
    List<TreeInfoRule> res = new ArrayList<>();
    for (TreeInfoRule rule : nodeRules) {
      if (rule.isMatchedLabelType(nodeType)) {
        res.add(rule);
      }
    }
    return res;
  }

  private void addToRuleSet(List<TreeInfoRule> ruleSet, TreeInfoRule rule) {
    ruleSet.add(rule);
  }

  private void addNodeVariable(String left, String right) {
    left = left.strip();
    if (left.length() <= 0) return;
    List<String> nodeTypes = new ArrayList<>();
    Arrays.asList(right.split("\\|"))
        .forEach(
            split -> {
              String stripped = split.strip();
              if (stripped.startsWith("_")) nodeTypes.addAll(nodeVariable.get(stripped));
              else if (stripped.length() > 0) nodeTypes.add(stripped);
            });

    nodeVariable.put(left, nodeTypes);
  }
}
