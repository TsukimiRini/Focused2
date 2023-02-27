package model;

import ai.serenade.treesitter.Node;
import ai.serenade.treesitter.Tree;
import ai.serenade.treesitter.TreeCursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CSTTree {
  public String nodeType;
  public String filePath;
  public String snippet;
  public int startIdx, endIdx;
  public Map<String, List<CSTTree>> children; // key: type, value: CSTTrees
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
  }

  // init from tree
  public CSTTree(String filePath, String source, Tree tree) {
    this(filePath, source, null, tree.getRootNode().walk());
  }

  // init from cursor
  public CSTTree(String filePath, String source, CSTTree parent, TreeCursor cursor) {
    if (cursor == null) return;
    this.filePath = filePath;
    this.nodeType = cursor.getCurrentNode().getType();
    this.startIdx = cursor.getCurrentNode().getStartByte();
    this.endIdx = cursor.getCurrentNode().getEndByte();
    this.snippet = source.substring(startIdx, endIdx);
    this.children = new HashMap<>();
    this.parent = parent;

    if (cursor.gotoFirstChild()) {
      Node cur = cursor.getCurrentNode();
      addChild(new CSTTree(filePath, source, this, cur.walk()));
      while (cursor.gotoNextSibling()) {
        cur = cursor.getCurrentNode();
        addChild(new CSTTree(filePath, source, this, cur.walk()));
      }
    }
  }

  public List<CSTTree> getDescendants(String source) {
    int pointIdx = source.indexOf(".");
    if (pointIdx == -1) pointIdx = source.length();
    String beforePoint = source.substring(0, pointIdx),
        afterPoint = pointIdx == source.length() ? "" : source.substring(pointIdx + 1);
    List<CSTTree> res = new ArrayList<>();
    beforePoint = beforePoint.replaceAll("\\*", ".+");
    Pattern layerPattern = Pattern.compile(beforePoint);
    List<String> childrenKey =
        children.keySet().stream()
            .filter(childKey -> layerPattern.matcher(childKey).find())
            .collect(Collectors.toList());

    if (afterPoint.isBlank()) {
      childrenKey.forEach(key -> res.addAll(children.get(key)));
    } else {
      childrenKey.forEach(
          key -> children.get(key).forEach(child -> res.addAll(child.getDescendants(afterPoint))));
    }
    return res;
  }

  private void addChild(CSTTree child) {
    String childType = child.nodeType;
    if (!children.containsKey(childType)) {
      children.put(childType, new ArrayList<>());
    }
    children.get(childType).add(child);
  }
}
