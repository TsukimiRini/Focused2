package model;

import utils.MatcherUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class URIPattern extends URIBase<SegmentPattern> {
  public Set<String> captures = new HashSet<>();

  public URIPattern(String label, String lang, String file, String element, List<String> branches) {
    super(label);

    this.lang = lang;

    this.file = new SegmentPattern(SegmentType.FILE, file);
    this.captures.addAll(this.file.captures);

    List<String> segments = MatcherUtils.matchSegments(element);
    if (segments == null) {
      logger.error("Invalid Element {}", label);
      throw new IllegalArgumentException("invalid config");
    }
    this.elementRoot = getPatternRoot(segments);

    branches.forEach(this::addBranch);
  }

  public void addBranch(String branch) {
    List<String> segments = MatcherUtils.matchSegments(branch);
    if (segments == null) {
      logger.error("Invalid Branch {}", branch);
      throw new IllegalArgumentException("invalid config");
    }
    SegmentPattern branchRoot = getPatternRoot(segments);
    branches.add(branchRoot);
  }

  private SegmentPattern getPatternRoot(List<String> segments) {
    SegmentPattern root = null;
    if (segments == null) return null;
    for (String seg : segments) {
      SegmentPattern cur;
      if (MatcherUtils.isEdge(seg)) {
        cur = new SegmentPattern(SegmentType.EDGE, seg.substring(1, seg.length() - 1));
      } else if (seg.length() == 0) {
        cur = new SegmentPattern(SegmentType.EDGE, "*");
      } else {
        cur = new SegmentPattern(SegmentType.NODE, seg);
      }
      cur.setParent(root);
      root = cur;
      this.captures.addAll(cur.captures);
    }
    if (root.segType == SegmentType.EDGE && root.text.text.length() == 0) root = (SegmentPattern) root.parent;
    return root;
  }
}
