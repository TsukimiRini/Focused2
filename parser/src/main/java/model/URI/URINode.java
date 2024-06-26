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
    // compress space
    nodeName = nodeName.replaceAll("\\s+", " ");
    if (nodeName.length() > 100) {
      nodeName = nodeName.substring(nodeName.length() - 100);
    }
    put("name", nodeName);
    this.type = nodeType;
  }

  public String getName() {
    return get("name");
  }

  public URINode getParent() {
    return edgeToParent == null ? null : edgeToParent.from;
  }

  public String toString() {
    boolean isFilePath = isDir() || type.equals("FILE");
    boolean noNeedToAppendParent =
        (edgeToParent == null || !isFilePath && edgeToParent.from.isDir());
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

  public void setEdge(String type, URINode parent) {
    edgeToParent = new URIEdge(type, parent);
  }

  public void analyzeAndSetEdge(CSTTree cstTree, List<TreeInfoRule> edgeRules, URINode parent) {
    TreeInfoRule matched = null;
    for (TreeInfoRule rule : edgeRules) {
      if (rule.isMatchedEdgeRule(parent, this)) {
        matched = rule;
        break;
      }
    }
    if (matched == null) {
      setEdge("", parent);
      return;
    }

    Pair<List<String>, Map<String, String>> attrs = getAttrVal(cstTree, matched);
    setEdge(matched.get("type").get(0).valueOrFunc.iterator().next(), parent);
    for (String key : attrs.getRight().keySet()) {
      if (key.equals("type")) continue;
      edgeToParent.put(key, attrs.getRight().get(key));
    }
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
        nextNode.analyzeAndSetEdge(tree, conf.edgeRules, this);
        if (attrs.getRight().containsKey("type")) {
          nextNode.type = attrs.getRight().get("type");
          attrs.getRight().remove("type");
        }
        nextNode.putAll(attrs.getRight());
        nextNode.put("range", tree.startIdx + "-" + tree.endIdx);
        nextNode.snippets = tree.snippet;
        addChild(nextNode);
      } else {
        String edgeType = null;
        for (String name : attrs.getLeft()) {
          URINode oneNode = new URINode(name, curNodeType);
          if (edgeType == null) oneNode.analyzeAndSetEdge(tree, conf.edgeRules, this);
          //          oneNode.setEdge(edgeType, this);
          if (attrs.getRight().containsKey("type")) {
            nextNode.type = attrs.getRight().get("type");
            attrs.getRight().remove("type");
          }
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
        List<String> tryToGetVal = new ArrayList<>(attrVal.getVal(tree, rule));
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

    //    if (nameVals == null) {
    //      throw new IllegalArgumentException("no name found");
    //    }
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
