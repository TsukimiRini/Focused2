package model.TreeInfo;

import model.CSTTree;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.MatcherUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TreeNodeAttrValue {
  protected static final Logger logger = LoggerFactory.getLogger(TreeNodeAttrValue.class);
  public Set<String> valueOrFunc = new HashSet<>();
  public List<Pair<String, List<String>>> args;
  public TreeNodeAttrValueType type;

  static {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
  }

  public TreeNodeAttrValue(String source, TreeInfoRule rule) {
    if (source.startsWith("\"") && source.endsWith("\"")) {
      type = TreeNodeAttrValueType.LITERAL;
    } else if (source.startsWith("$")) {
      type = TreeNodeAttrValueType.FUNCTION;
    } else if (source.startsWith("[") && source.endsWith("]")) {
      type = TreeNodeAttrValueType.LIST_OF_TYPE;
    } else if (source.startsWith("{") && source.endsWith("}")) {
      type = TreeNodeAttrValueType.FIELD_NAME;
    } else if (isInteger(source)) {
      type = TreeNodeAttrValueType.INTEGER;
    } else if (source.contains("[") && source.contains("]")) {
      type = TreeNodeAttrValueType.INDEXED_CST_NODE_TYPE;
    } else {
      type = TreeNodeAttrValueType.CST_NODE_TYPE;
    }

    switch (type) {
      case FIELD_NAME:
      case LITERAL:
        valueOrFunc.add(source.substring(1, source.length() - 1));
        break;
      case LIST_OF_TYPE:
        setValueByVar(source.substring(1, source.length() - 1), rule);
        break;
      case FUNCTION:
        Pair<String, List<String>> parsePair = MatcherUtils.parseDollarFunction(source);
        valueOrFunc.add(parsePair.getLeft());
        setArgsByVar(parsePair.getRight(), rule);
        break;
      case INTEGER:
        valueOrFunc.add(source);
        break;
      case CST_NODE_TYPE:
      case INDEXED_CST_NODE_TYPE:
        setValueByVar(source, rule);
        break;
    }
  }

  private void setValueByVar(String val, TreeInfoRule rule) {
    valueOrFunc.addAll(rule.listTypesFor(val));
  }

  private void setArgsByVar(List<String> inputArgs, TreeInfoRule rule) {
    args = new ArrayList<>();
    for (String arg : inputArgs) {
      args.add(Pair.of(arg, new ArrayList<>(rule.listTypesFor(arg))));
    }
  }

  public TreeNodeAttrValue(String val, TreeNodeAttrValueType type) {
    this.valueOrFunc.add(val);
    this.type = type;
  }

  public boolean isIntegerType() {
    return type == TreeNodeAttrValueType.INTEGER;
  }

  public boolean isLiteralType() {
    return type == TreeNodeAttrValueType.LITERAL;
  }

  public boolean isNodeType() {
    return type == TreeNodeAttrValueType.CST_NODE_TYPE;
  }

  public boolean isListType() {
    return type == TreeNodeAttrValueType.LIST_OF_TYPE;
  }

  public boolean isFieldNameType() {
    return type == TreeNodeAttrValueType.FIELD_NAME;
  }

  @Override
  public String toString() {
    switch (type) {
      case FUNCTION:
        return "$" + valueOrFunc;
      case LITERAL:
      case INTEGER:
      case CST_NODE_TYPE:
      case INDEXED_CST_NODE_TYPE:
        return valueOrFunc.toString();
    }
    return null;
  }

  public Set<String> getVal(CSTTree curNode, TreeInfoRule rule) {
    Set<String> res = new HashSet<>();
    switch (type) {
      case FUNCTION:
        res.add(getFuncReturnVal(curNode, rule));
        break;
      case LITERAL:
      case INTEGER:
        res.addAll(valueOrFunc);
        break;
      case CST_NODE_TYPE:
      case LIST_OF_TYPE:
        valueOrFunc.forEach(
            value ->
                res.addAll(
                    value.equals("this")
                        ? List.of(curNode.snippet)
                        : curNode.getDescendantsByType(value).stream()
                            .map(descendant -> descendant.snippet)
                            .collect(Collectors.toList())));
        break;
      case INDEXED_CST_NODE_TYPE:
        String indexed_str = valueOrFunc.iterator().next();
        String node_type = indexed_str.substring(0, indexed_str.indexOf("["));
        int index =
            Integer.parseInt(
                indexed_str.substring(indexed_str.indexOf("[") + 1, indexed_str.indexOf("]")));
        if (curNode.getDescendantsByType(node_type).size() <= index) break;
        if (index < 0) {
          index = curNode.getDescendantsByType(node_type).size() + index;
        }
        String indexed_snippet = curNode.getDescendantsByType(node_type).get(index).snippet;
        if (indexed_snippet != null) res.add(indexed_snippet);
        break;
      case FIELD_NAME:
        String descendantSnippet = curNode.getDescendantByField(valueOrFunc.iterator().next());
        if (descendantSnippet != null) res.add(descendantSnippet);
        break;
    }
    return res;
  }

  public String getFuncReturnVal(CSTTree curNode, TreeInfoRule rule) {
    if (type != TreeNodeAttrValueType.FUNCTION) return null;
    String res = null;
    List<String> arg = null;
    switch (valueOrFunc.iterator().next()) {
      case "cnt":
        if (args.size() != 1) {
          throw new IllegalArgumentException("function cnt must have 1 argument");
        }
        // TODO: can arg=this?
        arg = args.get(0).getRight();
        res =
            String.valueOf(
                arg.stream()
                    .map(x -> curNode.getDescendantsByType(x).size())
                    .reduce(0, Integer::sum));
        break;
      case "idx":
        if (args.size() != 1) {
          throw new IllegalArgumentException("function idx must have 1 argument");
        }
        arg = args.get(0).getRight();
        if (!(arg.size() == 1 && arg.get(0).equals("this"))) {
          throw new IllegalArgumentException("not supported yet");
        }
        List<CSTTree> siblings =
            curNode.parent.childrenSeq.stream()
                .filter(x -> rule.isMatchedTypePatterns(x.nodeType, rule.childNodeType))
                .collect(Collectors.toList());
        res = String.valueOf(siblings.indexOf(curNode));
        break;
    }
    return res;
  }

  private boolean isInteger(String source) {
    for (int i = 0; i < source.length(); i++) {
      if (!Character.isDigit(source.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
