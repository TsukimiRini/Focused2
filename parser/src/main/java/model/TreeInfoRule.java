package model;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TreeInfoRule extends HashMap<String, List<String>> {
  public String label;
  public List<String> parentNodeType, childNodeType;
  public TreeInfoRuleType ruleType;
  public List<String> nodeTypesForName = new ArrayList<>();
  public TreeInfoConf conf;

  public TreeInfoRule(String label, TreeInfoConf conf) {
    this.label = label;
    this.conf = conf;
    if (label.contains("/")) {
      ruleType = TreeInfoRuleType.EDGE;
      int slashIdx = label.indexOf('/');
      parentNodeType = listTypesFor(label.substring(0, slashIdx), conf);
      childNodeType = listTypesFor(label.substring(slashIdx + 1), conf);
    } else {
      ruleType = TreeInfoRuleType.NODE;
    }
  }

  // iterate over all the possible type regex
  private List<String> listTypesFor(String pattern, TreeInfoConf conf) {
    if (pattern.isEmpty()) return List.of("");

    int underIdx = pattern.indexOf("._");
    if (pattern.charAt(0) == '_') underIdx = -1;
    if (underIdx == -1) return List.of(pattern);
    else underIdx += 1;
    int nextSegIdx = pattern.substring(underIdx).indexOf('.');
    if (nextSegIdx != -1) nextSegIdx += underIdx;
    else nextSegIdx = pattern.length();

    List<String> coll = new ArrayList<>();
    String abstractVar = pattern.substring(underIdx, nextSegIdx);
    String prefix = pattern.substring(0, underIdx);
    conf.nodeVariable.get(abstractVar).forEach(var -> coll.add(prefix + var));

    List<String> posix =
        listTypesFor(
            nextSegIdx + 1 >= pattern.length() ? "" : pattern.substring(nextSegIdx + 1), conf);
    List<String> res = new ArrayList<>();
    for (String left : coll) {
      for (String right : posix) {
        res.add(left + (right.equals("") ? "" : ".") + right);
      }
    }
    return res;
  }

  public TreeInfoRule(String label, TreeInfoConf conf, boolean isDefault) {
    this(label, conf);

    if (isDefault) {
      put("name", List.of("this"));
    }
  }

  public void addAttribute(String key, String value) {
    String[] splitValue = value.split("\\|");
    List<String> valGroup = new ArrayList<>();
    for (String val : splitValue) {
      processAndRecord(val, valGroup, key.equals("name"));
    }
    if (valGroup.isEmpty()) {
      throw new IllegalArgumentException("illegal value in uriTree conf");
    }

    put(key, valGroup);
  }

  public boolean nodeTypesForNameCovered(CSTTree tree) {
    Set<String> ruleNodeTypes = new HashSet<>(nodeTypesForName);
    Set<String> curNodeTypes = tree.children.keySet();
    int originalSize = ruleNodeTypes.size();
    ruleNodeTypes.removeAll(curNodeTypes);
    return curNodeTypes.size() < originalSize;
  }

  public boolean isMatchedEdgeRule(String parentType, URINode childNode) {
    return isMatchedChildNode(childNode) && isMatchedTypePatterns(parentType, parentNodeType);
  }

  private int getChildEdgeDepth() {
    if (ruleType != TreeInfoRuleType.EDGE) return 0;
    return childNodeType.get(0).split("\\.").length;
  }

  private boolean isMatchedChildNode(URINode childNode) {
    if (ruleType != TreeInfoRuleType.EDGE) return false;
    int childDepth = getChildEdgeDepth();
    StringBuilder sb = new StringBuilder(childNode.type);
    URINode parent = childNode.edgeToParent.from;
    for (int i = 1; i < childDepth && parent != null; i++) {
      sb.insert(0, parent.type + ".");
      parent = parent.edgeToParent.from;
    }

    return isMatchedTypePatterns(sb.toString(), childNodeType);
  }

  private boolean isMatchedTypePatterns(String node, List<String> nodeTypes) {
    for (String patternString : nodeTypes) {
      Pattern pattern = Pattern.compile(patternString);
      Matcher matcher = pattern.matcher(node);
      if (matcher.find()) {
        return true;
      }
    }
    return false;
  }

  private void processAndRecord(String value, List<String> valGroup, boolean isName) {
    value = value.strip();
    if (value.startsWith("_")) {
      valGroup.addAll(conf.nodeVariable.get(value));
      if (isName) nodeTypesForName.addAll(conf.nodeVariable.get(value));
    } else {
      valGroup.add(value);
      if (isName && !value.startsWith("\"")) {
        nodeTypesForName.add(value);
      }
    }
  }
}
