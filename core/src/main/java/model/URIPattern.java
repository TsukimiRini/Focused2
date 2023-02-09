package model;

import utils.MatcherUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class URIPattern extends URIBase<SegmentPattern> {
  public Set<String> captures = new HashSet<>();
  public List<String> params;

  public URIPattern(
      String label,
      String lang,
      String file,
      String element,
      List<String> params,
      List<String> branches) {
    super(label);
    this.params = params;

    fillInFields(lang, file, element, branches);

    this.captures.addAll(this.file.captures);
  }

  public URIPattern(
      URIPattern template,
      List<String> values,
      String label,
      String lang,
      String file,
      String element,
      List<String> params,
      List<String> branches) {
    super(label);
    this.params = params;

    if (template.lang != null) this.lang = template.lang;

    if (template.file != null) {
      this.file =
          new SegmentPattern(
              SegmentType.FILE, template.replacePlaceholder(template.file.toString(), values));
      this.captures.addAll(this.file.captures);
    }

    if (template.elementRoot != null) {
      String elementSource = template.elementRoot.toString();
      List<String> segments =
          MatcherUtils.matchSegments(template.replacePlaceholder(elementSource, values));
      if (segments == null) {
        logger.error("Invalid Element {}", label);
        throw new IllegalArgumentException("invalid config");
      }
      this.elementRoot = getPatternRoot(segments);
    }

    template.branches.forEach(
        branch -> this.addBranch(template.replacePlaceholder(branch.toString(), values)));

    fillInFields(lang, file, element, branches);
  }

  private void fillInFields(String lang, String file, String element, List<String> branches) {
    if (this.lang == null) this.lang = lang;
    else if (lang != null) throw new IllegalArgumentException("invalid config");

    if (file != null) {
      if (this.file == null) {
        this.file = new SegmentPattern(SegmentType.FILE, file);
        this.captures.addAll(this.file.captures);
      } else throw new IllegalArgumentException("invalid config");
    }

    if (element != null) {
      if (this.elementRoot == null) {
        List<String> segments = MatcherUtils.matchSegments(element);
        if (segments == null) {
          logger.error("Invalid Element {}", label);
          throw new IllegalArgumentException("invalid config");
        }
        this.elementRoot = getPatternRoot(segments);
      } else throw new IllegalArgumentException("invalid config");
    }

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
    if (segments == null || segments.isEmpty()) return null;
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
    if (root.segType == SegmentType.EDGE && root.text.text.length() == 0)
      root = (SegmentPattern) root.parent;
    return root;
  }

  public String replacePlaceholder(String source, List<String> values) {
    if (source == null || values == null) return source;
    String res = source;
    for (int i = 0; i < params.size(); i++) {
      String value = values.get(i), paramName = params.get(i);
      if (value.startsWith("\"") || value.startsWith("`")) {
        res = res.replace("<" + paramName + ">", value.substring(1, value.length() - 1));
      } else {
        res = res.replace("<" + paramName + ">", "<" + value + ">");
      }
    }
    return res;
  }
}
