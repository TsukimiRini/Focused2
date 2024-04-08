package model.TreeInfo;

import model.CSTTree;
import model.URI.URINode;

import java.util.*;

public class TreeInfoRule extends HashMap<String, List<TreeNodeAttrValue>> {
  public String label;
  public List<String> parentNodeType, childNodeType;
  public TreeInfoRuleType ruleType;
  public List<String> nodeTypesForName = new ArrayList<>();
  public String fieldNameForName;
  public TreeInfoConf conf;

  public TreeInfoRule(String label, TreeInfoConf conf) {
    this.label = label;
    this.conf = conf;
    if (label.contains("/")) {
      ruleType = TreeInfoRuleType.EDGE;
      int slashIdx = label.indexOf('/');
      parentNodeType = listTypesFor(label.substring(0, slashIdx));
      childNodeType = listTypesFor(label.substring(slashIdx + 1));
    } else {
      ruleType = TreeInfoRuleType.NODE;
    }
  }

  // iterate over all the possible type regex
  public List<String> listTypesFor(String pattern) {
    if (pattern.isEmpty()) return List.of("");

    int underIdx = pattern.indexOf("._");
    if (underIdx != -1) underIdx++;
    if (pattern.charAt(0) == '_') underIdx = 0;
    if (underIdx == -1) return List.of(pattern);
    int nextSegIdx = pattern.substring(underIdx).indexOf('.');
    if (nextSegIdx != -1) nextSegIdx += underIdx;
    else nextSegIdx = pattern.length();

    List<String> coll = new ArrayList<>();
    String abstractVar = pattern.substring(underIdx, nextSegIdx);
    String prefix = pattern.substring(0, underIdx);
    conf.nodeVariable.get(abstractVar).forEach(var -> coll.add(prefix + var));

    List<String> posix =
        listTypesFor(nextSegIdx + 1 >= pattern.length() ? "" : pattern.substring(nextSegIdx + 1));
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
      put("name", List.of(new TreeNodeAttrValue("this", this)));
    }
  }

  public void addAttribute(String key, String value) {
    String[] splitValue = value.split("\\|");
    List<TreeNodeAttrValue> valGroup = new ArrayList<>();
    for (String val : splitValue) {
      processAndRecord(val, valGroup, key.equals("name"));
    }
    if (valGroup.isEmpty()) {
      throw new IllegalArgumentException("illegal value in uriTree conf");
    }

    put(key, valGroup);
  }

  public boolean nameCovered(CSTTree tree) {
    if (fieldNameForName != null) {
      return tree.getDescendantByField(fieldNameForName) != null;
    }
    nodeTypesForName.remove("this");
    Set<String> ruleNodeTypes = new HashSet<>(nodeTypesForName);
    if (ruleNodeTypes.isEmpty()) return true;
    for (String neededNodeType : ruleNodeTypes) {
      List<CSTTree> collected = tree.getDescendantsByType(neededNodeType);
      if (!collected.isEmpty()) return true;
    }
    return false;
  }

  public boolean isMatchedLabelType(String nodeType) {
    if (label.startsWith("_")) {
      return conf.nodeVariable.containsKey(label)
          && conf.nodeVariable.get(label).contains(nodeType);
    }
    return nodeType.matches(getRegexPatternFromSymbol(label));
  }

  public boolean isMatchedEdgeRule(URINode parentNode, URINode childNode) {
    return isMatchedChildNode(parentNode, childNode)
        && isMatchedTypePatterns(parentNode.type, parentNodeType);
  }

  private int getChildEdgeDepth() {
    if (ruleType != TreeInfoRuleType.EDGE) return 0;
    return childNodeType.get(0).split("\\.").length;
  }

  public boolean isMatchedChildNode(URINode parentNode, URINode childNode) {
    if (ruleType != TreeInfoRuleType.EDGE) return false;
    int childDepth = getChildEdgeDepth();
    StringBuilder sb = new StringBuilder(childNode.type);
    for (int i = 1; i < childDepth && parentNode != null && parentNode.edgeToParent != null; i++) {
      sb.insert(0, parentNode.type + ".");
      parentNode = parentNode.edgeToParent.from;
    }

    return isMatchedTypePatterns(sb.toString(), childNodeType);
  }

  public boolean isMatchedTypePatterns(String node, List<String> nodeTypes) {
    for (String patternString : nodeTypes) {
      if (node.matches(getRegexPatternFromSymbol(patternString))) {
        return true;
      }
    }
    return false;
  }

  private String getRegexPatternFromSymbol(String symbol) {
    return symbol.replaceAll("\\.", "\\.").replaceAll("\\*", ".+");
  }

  private void processAndRecord(String value, List<TreeNodeAttrValue> valGroup, boolean isName) {
    value = value.strip();
    TreeNodeAttrValue attrValue = new TreeNodeAttrValue(value, this);
    valGroup.add(attrValue);
    if (isName) {
      switch (attrValue.type) {
        case INDEXED_CST_NODE_TYPE:
          for (String indexed : attrValue.valueOrFunc) {
            nodeTypesForName.add(indexed.substring(0, indexed.indexOf("[")));
          }
          break;
        case CST_NODE_TYPE:
        case LIST_OF_TYPE:
          nodeTypesForName.addAll(attrValue.valueOrFunc);
          break;
        case FIELD_NAME:
          fieldNameForName = attrValue.valueOrFunc.iterator().next();
          break;
      }
    }
  }
}
