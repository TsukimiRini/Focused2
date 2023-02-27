package model.TreeInfo;

import org.apache.commons.lang3.tuple.Pair;
import utils.MatcherUtils;

import java.util.List;

public class TreeNodeAttrValue {
  public String valueOrFunc;
  public List<String> args;
  public TreeNodeAttrValueType type;

  public TreeNodeAttrValue(String source) {
    if (source.startsWith("\"") && source.endsWith("\"")) {
      type = TreeNodeAttrValueType.LITERAL;
    } else if (source.startsWith("$")) {
      type = TreeNodeAttrValueType.FUNCTION;
    } else if (isInteger(source)) {
      type = TreeNodeAttrValueType.INTEGER;
    } else {
      type = TreeNodeAttrValueType.CST_NODE_TYPE;
    }

    switch (type) {
      case LITERAL:
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

  private boolean isInteger(String source) {
    for (int i = 0; i < source.length(); i++) {
      if (!Character.isDigit(source.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
