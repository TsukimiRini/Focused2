package model.URI;

import model.CSTTree;
import model.TreeInfo.TreeInfoConf;
import model.TreeInfo.TreeInfoRule;
import model.TreeInfo.TreeNodeAttrValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class URINode extends URISegment {
  public URIEdge edgeToParent;
  public Map<String, List<URINode>> children = new HashMap<>(); // key: nodeName
  public String snippets;

  public URINode(String nodeName, String nodeType) {
    put("name", nodeName);
    this.type = nodeType;
  }

  public String toString() {
    boolean isFilePath = isDir();
    boolean noNeedToAppendParent = edgeToParent == null || !isFilePath && edgeToParent.from.isDir();
    return (noNeedToAppendParent
            ? ""
            : (edgeToParent.from.toString()
                + (isFilePath ? "//" : "/" + edgeToParent.toString() + "/")))
        + get("name")
        + (isFilePath ? "" : ("::" + (type == null ? "NONE" : type) + getAttrStr()));
  }

  public Boolean isDir() {
    return type.equals("DIRECTORY") || type.equals("ROOT");
  }

  public String getEdgeType(List<TreeInfoRule> edgeRules, URINode parent) {
    for (TreeInfoRule rule : edgeRules) {
      if (rule.isMatchedEdgeRule(parent, this)) {
        TreeNodeAttrValue typeVal = rule.get("type").get(0);
        if (!typeVal.isLiteralType()) {
          throw new IllegalArgumentException("invalid edge type");
        }
        return typeVal.valueOrFunc.iterator().next();
      }
    }
    return "";
  }

  public void setEdge(String type, URINode parent) {
    edgeToParent = new URIEdge(type, parent);
  }

  public void analyzeAndSetEdge(List<TreeInfoRule> edgeRules, URINode parent) {
    String type = getEdgeType(edgeRules, parent);
    setEdge(type, parent);
  }

  public void addCST(String filePath, CSTTree tree, TreeInfoConf conf) {
    String[] dirOrFile = filePath.split("/|\\\\");
    URINode iter = this;
    for (int i = 0; i < dirOrFile.length; i++) {
      String layer = dirOrFile[i];

      URINode childDir = iter.children.containsKey(layer) ? iter.children.get(layer).get(0) : null;
      if (childDir == null) {
        childDir = new URINode(layer, i == dirOrFile.length - 1 ? "FILE" : "DIRECTORY");
        childDir.setEdge("", iter);
        iter.addChild(childDir);
      }
      iter = childDir;
    }

    List<TreeInfoRule> rulesForTree = conf.getNodeRule(tree.nodeType);
    TreeInfoRule matched = null;
    for (TreeInfoRule rule : rulesForTree) {
      if (rule.nameCovered(tree)) {
        matched = rule;
      }
    }
    iter.addCST(tree, conf, matched);
  }

  public void addCST(CSTTree tree, TreeInfoConf conf, TreeInfoRule matchedNodeRule) {
    String curNodeType = tree.nodeType;
    // get next node
    URINode nextNode = this;
    if (matchedNodeRule != null && !tree.snippet.isBlank()) {
      // default behavior: attr value = the first node of the given type
      Pair<List<String>, Map<String, String>> attrs = getAttrVal(tree, matchedNodeRule);
      if (attrs.getLeft().size() == 1) {
        nextNode = new URINode(attrs.getLeft().get(0), curNodeType);
        nextNode.analyzeAndSetEdge(conf.edgeRules, this);
        nextNode.putAll(attrs.getRight());
        nextNode.snippets = tree.snippet;
        addChild(nextNode);
      } else {
        String edgeType = null;
        for (String name : attrs.getLeft()) {
          URINode oneNode = new URINode(name, curNodeType);
          if (edgeType == null) edgeType = oneNode.getEdgeType(conf.edgeRules, this);
          oneNode.setEdge(edgeType, this);
          oneNode.putAll(attrs.getRight());
          nextNode.snippets = tree.snippet;
          addChild(oneNode);
        }
        nextNode = null;
      }
      //      List<String> nameNodeTypes = matchedNodeRule.nodeTypesForName;
      //      String nextNodeName = null;
      //      for (String nameNodeType : nameNodeTypes) {
      //        if (tree.children.containsKey(nameNodeType)) {
      //          nextNodeName = tree.children.get(nameNodeType).get(0).snippet;
      //          break;
      //        }
      //      }
      //      nextNode = new URINode(nextNodeName, curNodeType);
      //      nextNode.analyzeAndSetEdge(conf.edgeRules, this);
    }

    if (nextNode == null) return;

    // iterate over child nodes
    for (String type : tree.children.keySet()) {
      List<TreeInfoRule> curRules = conf.getNodeRule(type);
      for (CSTTree childTree : tree.children.get(type)) {
        TreeInfoRule ruleForNextNode = null;
        for (TreeInfoRule rule : curRules) {
          if (rule.nameCovered(childTree)) {
            ruleForNextNode = rule;
            break;
          }
        }
        nextNode.addCST(childTree, conf, ruleForNextNode);
      }
    }
  }

  private Pair<List<String>, Map<String, String>> getAttrVal(CSTTree tree, TreeInfoRule rule) {
    // default behavior: name value = all the nodes of the first matched type
    //                   attr value = the first node of the first matched type
    List<String> nameVals = null;
    Map<String, String> attrMap = new HashMap<>();

    for (String attrKey : rule.keySet()) {
      List<TreeNodeAttrValue> attrValues = rule.get(attrKey);
      for (TreeNodeAttrValue attrVal : attrValues) {
        List<String> tryToGetVal = new ArrayList<>(attrVal.getVal(tree));
        if (!tryToGetVal.isEmpty()) {
          // TODO: remove temprate patch
          if (!attrVal.isListType()) {
            tryToGetVal = List.of(tryToGetVal.get(0));
          }
          if (attrKey.equals("name")) nameVals = tryToGetVal;
          else attrMap.put(attrKey, tryToGetVal.get(0).replaceAll("\\s+", " "));
          break;
        }
      }
    }

    if (nameVals == null) {
      throw new IllegalArgumentException("no name found");
    }
    return Pair.of(nameVals, attrMap);
  }

  private void addChild(URINode child) {
    String nodeName = child.get("name");
    if (!children.containsKey(nodeName)) {
      children.put(nodeName, new ArrayList<>());
    }
    children.get(nodeName).add(child);
  }
}
