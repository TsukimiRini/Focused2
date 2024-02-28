package model;

import ai.serenade.treesitter.Node;
import ai.serenade.treesitter.Tree;
import ai.serenade.treesitter.TreeCursor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CSTTree implements Serializable {
  public String nodeType;
  public String filePath;
  public String snippet;
  public String fieldName;
  public int startIdx, endIdx;
  public Map<String, List<CSTTree>> children; // key: type, value: CSTTrees
  public List<CSTTree> childrenSeq;
  public Map<String, String> fields;
  public CSTTree parent;

  // init from node
  public CSTTree(
      String nodeType, String filePath, String snippet, CSTTree parent, int startIdx, int endIdx) {
    this.nodeType = nodeType;
    this.filePath = filePath;
    this.snippet = snippet;
    this.startIdx = startIdx;
    this.endIdx = endIdx;
    this.parent = parent;
    children = new HashMap<>();
    childrenSeq = new ArrayList<>();
    fields = new HashMap<>();
  }

  // init from tree
  public CSTTree(String filePath, String source, Tree tree) {
    this(filePath, source, null, tree.getRootNode().walk(), null);
//    System.out.println(tree.getRootNode().getNodeString());
  }

  // init from cursor
  public CSTTree(
      String filePath, String source, CSTTree parent, TreeCursor cursor, String fieldName) {
    if (cursor == null) return;
    this.filePath = filePath;
    this.nodeType = removePreUnderscore(cursor.getCurrentNode().getType());
    this.fieldName = fieldName;
    this.startIdx = cursor.getCurrentNode().getStartByte();
    this.endIdx = cursor.getCurrentNode().getEndByte();
    this.snippet = source.substring(startIdx, endIdx);
    this.children = new HashMap<>();
    this.childrenSeq = new ArrayList<>();
    this.fields = new HashMap<>();
    this.parent = parent;

    if (cursor.gotoFirstChild()) {
      Node cur = cursor.getCurrentNode();
      addChild(new CSTTree(filePath, source, this, cur.walk(), cursor.getCurrentFieldName()));
      while (cursor.gotoNextSibling()) {
        cur = cursor.getCurrentNode();
        addChild(new CSTTree(filePath, source, this, cur.walk(), cursor.getCurrentFieldName()));
      }
    }
  }

  public List<CSTTree> getDescendantsByType(String source) {
    int pointIdx = source.indexOf(".");
    if (pointIdx == -1) pointIdx = source.length();
    String beforePoint = source.substring(0, pointIdx),
        afterPoint = pointIdx == source.length() ? "" : source.substring(pointIdx + 1);
    List<CSTTree> res = new ArrayList<>();
    beforePoint = beforePoint.replaceAll("\\*", ".+");
    String finalBeforePoint = beforePoint;
    List<String> childrenKey =
        children.keySet().stream()
            .filter(childKey -> childKey.matches(finalBeforePoint))
            .collect(Collectors.toList());

    if (afterPoint.isBlank()) {
      childrenKey.forEach(key -> res.addAll(children.get(key)));
    } else {
      childrenKey.forEach(
          key ->
              children
                  .get(key)
                  .forEach(child -> res.addAll(child.getDescendantsByType(afterPoint))));
    }
    return res;
  }

  public String getDescendantByField(String source) {
    return fields.get(source);
  }

  private void addChild(CSTTree child) {
    String childType = child.nodeType;
    if (!children.containsKey(childType)) {
      children.put(childType, new ArrayList<>());
    }
    children.get(childType).add(child);
    childrenSeq.add(child);

    if (child.fieldName != null) {
      fields.put(child.fieldName, child.snippet);
    }
  }

  private String removePreUnderscore(String type) {
    while (type.startsWith("_")) {
      type = type.substring(1);
    }
    return type;
  }
}
