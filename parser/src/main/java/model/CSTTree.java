package model;

import ai.serenade.treesitter.Node;
import ai.serenade.treesitter.Tree;
import ai.serenade.treesitter.TreeCursor;

import java.util.ArrayList;
import java.util.List;

public class CSTTree {
  String nodeType;
  String filePath;
  String snippet;
  int startIdx, endIdx;
  List<CSTTree> children;

  // init from node
  public CSTTree(
      String nodeType, String filePath, String snippet, int startIdx, int endIdx) {
    this.nodeType = nodeType;
    this.filePath = filePath;
    this.snippet = snippet;
    this.startIdx = startIdx;
    this.endIdx = endIdx;
    children = new ArrayList<>();
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
    this.children = new ArrayList<>();

    if (cursor.gotoFirstChild()) {
      Node cur = cursor.getCurrentNode();
      children.add(new CSTTree(filePath, source, cur.walk()));
      while (cursor.gotoNextSibling()) {
        cur = cursor.getCurrentNode();
        children.add(new CSTTree(filePath, source, cur.walk()));
      }
    }
  }
}
