package model.TreeInfo;

import model.CSTTree;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.MatcherUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TreeNodeAttrValue {
  protected static final Logger logger = LoggerFactory.getLogger(TreeNodeAttrValue.class);
  public String valueOrFunc;
  public List<String> args;
  public TreeNodeAttrValueType type;

  static{
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
  }

  public TreeNodeAttrValue(String source) {
    if (source.startsWith("\"") && source.endsWith("\"")) {
      type = TreeNodeAttrValueType.LITERAL;
    } else if (source.startsWith("$")) {
      type = TreeNodeAttrValueType.FUNCTION;
    } else if (source.startsWith("[") && source.endsWith("]")) {
      type = TreeNodeAttrValueType.LIST_OF_TYPE;
    } else if (isInteger(source)) {
      type = TreeNodeAttrValueType.INTEGER;
    } else {
      type = TreeNodeAttrValueType.CST_NODE_TYPE;
    }

    switch (type) {
      case LITERAL:
      case LIST_OF_TYPE:
        valueOrFunc = source.substring(1, source.length() - 1);
        break;
      case FUNCTION:
        Pair<String, List<String>> parsePair = MatcherUtils.parseDollarFunction(source);
        valueOrFunc = parsePair.getLeft();
        args = parsePair.getRight();
        break;
      case INTEGER:
      case CST_NODE_TYPE:
        valueOrFunc = source;
    }
  }

  public TreeNodeAttrValue(String val, TreeNodeAttrValueType type) {
    this.valueOrFunc = val;
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

  @Override
  public String toString() {
    switch (type) {
      case FUNCTION:
        return "$"
            + valueOrFunc
            + "("
            + args.stream().reduce("", (base, arg) -> base + (base.isBlank() ? "" : ", ") + arg)
            + ")";
      case LITERAL:
      case INTEGER:
      case CST_NODE_TYPE:
        return valueOrFunc;
    }
    return null;
  }

  public List<String> getVal(CSTTree curNode) {
    List<String> res = new ArrayList<>();
    switch (type) {
      case FUNCTION:
        res.add(getFuncReturnVal(curNode));
        break;
      case LITERAL:
      case INTEGER:
        res.add(valueOrFunc);
        break;
      case CST_NODE_TYPE:
        if (valueOrFunc.equals("this")) {
          res.add(curNode.snippet);
          break;
        }
      case LIST_OF_TYPE:
        res.addAll(
            curNode.getDescendants(valueOrFunc).stream()
                .map(descendant -> descendant.snippet)
                .collect(Collectors.toList()));
    }
    return res;
  }

  public String getFuncReturnVal(CSTTree curNode) {
    if (type != TreeNodeAttrValueType.FUNCTION) return null;
    String res = null;
    String arg = null;
    switch (valueOrFunc) {
      case "cnt":
        if (args.size() != 1) {
          throw new IllegalArgumentException("function cnt must have 1 argument");
        }
        // TODO: can arg=this?
        arg = args.get(0);
        res = String.valueOf(curNode.getDescendants(arg).size());
        break;
      case "idx":
        if (args.size() != 1) {
          throw new IllegalArgumentException("function idx must have 1 argument");
        }
        arg = args.get(0);
        if (!arg.equals("this")) {
          throw new IllegalArgumentException("not supported yet");
        }
        res = String.valueOf(curNode.parent.children.get(curNode.nodeType).size());
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
