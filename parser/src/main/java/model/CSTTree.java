package model;

import ai.serenade.treesitter.Node;
import ai.serenade.treesitter.Tree;
import ai.serenade.treesitter.TreeCursor;
import org.treesitter.*;

import java.io.Serializable;
import java.util.*;
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
  }

  public CSTTree(
      String filePath,
      String source,
      String[] splitted,
      int[] sourceLen,
      TSTree tree,
      TSLanguage language) {
    this(filePath, source, splitted, sourceLen, null, tree.getRootNode(), null, language);
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

  private int coorToIdx(TSPoint point, String source) {
    int row = point.getRow(), col = point.getColumn();
    int idx = 0;
    String[] lines = source.split("\n", -1);
    //    for (int i = 0; i < row; i++) {
    //      idx += (lines[i] + "\n").length();
    //    }
    idx =
        Arrays.stream(Arrays.copyOfRange(lines, 0, row)).map(String::length).reduce(0, Integer::sum)
            + row;
    idx += new String(Arrays.copyOfRange(lines[row].getBytes(), 0, col)).length();
    return idx;
  }

  private int byteIndexToCharIndex(int byteIndex, String source) {
    byte[] bytes = Arrays.copyOfRange(source.getBytes(), 0, byteIndex);
    return new String(bytes).length();
  }

  public CSTTree(
      String filePath,
      String source,
      String[] splitted,
      int[] sourceLen,
      CSTTree parent,
      TSNode cur,
      String fieldName,
      TSLanguage language) {
    if (cur == null) return;
    this.filePath = filePath;
    this.nodeType = removePreUnderscore(cur.getType());
    this.fieldName = fieldName;
    int startRow = cur.getStartPoint().getRow(), startCol = cur.getStartPoint().getColumn();
    int endRow = cur.getEndPoint().getRow(), endCol = cur.getEndPoint().getColumn();
    this.startIdx =
        startRow == 0
            ? 0
            : sourceLen[startRow - 1]
                + new String(Arrays.copyOfRange(splitted[startRow].getBytes(), 0, startCol))
                    .length();
    this.endIdx =
        endRow == 0
            ? 0
            : sourceLen[endRow - 1]
                + new String(Arrays.copyOfRange(splitted[endRow].getBytes(), 0, endCol)).length();
    //    byte[] bytes = Arrays.copyOfRange(source.getBytes(), startIdx, endIdx);
    //    this.snippet = new String(bytes);
    this.snippet = source.substring(startIdx, endIdx);
    this.endIdx = this.startIdx + this.snippet.strip().length();
    this.snippet = this.snippet.strip();

    this.children = new HashMap<>();
    this.childrenSeq = new ArrayList<>();
    this.fields = new HashMap<>();
    this.parent = parent;

    for (int idx = 0; idx < cur.getChildCount(); idx++) {
      TSNode namedNode = cur.getChild(idx);
//      String childFieldName = cur.getFieldNameForChild(idx);
      //      if ((childFieldName == null || childFieldName.equals("")) && !namedNode.isNamed()) {
      //        continue;
      //      }

      addChild(new CSTTree(filePath, source, splitted, sourceLen, this, namedNode, cur.getFieldNameForChild(idx), language));
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
