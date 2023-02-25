package model;

import ai.serenade.treesitter.Node;
import ai.serenade.treesitter.Tree;
import ai.serenade.treesitter.TreeCursor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSTTree {
  public String nodeType;
  public String filePath;
  public String snippet;
  public int startIdx, endIdx;
  public Map<String, List<CSTTree>> children; // key: type, value: CSTTrees

  // init from node
  public CSTTree(
      String nodeType, String filePath, String snippet, int startIdx, int endIdx) {
    this.nodeType = nodeType;
    this.filePath = filePath;
    this.snippet = snippet;
    this.startIdx = startIdx;
    this.endIdx = endIdx;
    children = new HashMap<>();
  }

  // init from tree
  public CSTTree(String filePath, String source, Tree tree) {
    this(filePath, source, tree.getRootNode().walk());
  }

  // init from cursor
  public CSTTree(String filePath, String source, TreeCursor cursor) {
    if (cursor == null) return;
    this.filePath = filePath;
    this.nodeType = cursor.getCurrentNode().getType();
    this.startIdx = cursor.getCurrentNode().getStartByte();
    this.endIdx = cursor.getCurrentNode().getEndByte();
    this.snippet = source.substring(startIdx, endIdx);
    this.children = new HashMap<>();

    if (cursor.gotoFirstChild()) {
      Node cur = cursor.getCurrentNode();
      addChild(new CSTTree(filePath, source, cur.walk()));
      while (cursor.gotoNextSibling()) {
        cur = cursor.getCurrentNode();
        addChild(new CSTTree(filePath, source, cur.walk()));
      }
    }
  }

  private void addChild(CSTTree child){
    String childType = child.nodeType;
    if(!children.containsKey(childType)){
      children.put(childType, new ArrayList<>());
    }
    children.get(childType).add(child);
  }
}
