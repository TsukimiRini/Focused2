package model;

import utils.MatcherUtils;

import java.util.ArrayList;
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

    if (template == null) {
      throw new IllegalArgumentException("template not found");
    }

    String finalLang = lang, finalFile = file, finalElement = element;

    // TODO: can lang & file be override
    if (template.lang != null) this.lang = template.lang;

    if (template.file != null) {
      finalFile = template.replacePlaceholder(template.file.toString(), values);
    }

    template.branches.forEach(
        (anchor, branch) ->
            branch.forEach(
                br ->
                    this.addAnchoredBranch(
                        anchor, template.replacePlaceholder(br.toString(), values))));

    template.defaultBranches.forEach(
        branch -> this.addDefaultBranch(template.replacePlaceholder(branch.toString(), values)));

    if (template.elementRoot != null) {
      if (finalElement != null)
        throw new IllegalArgumentException("template element can't be override");
      String elementSource = template.elementRoot.toString();
      finalElement = template.replacePlaceholder(elementSource, values);
    }

    fillInFields(finalLang, finalFile, finalElement, branches);
  }

  private void fillInFields(String lang, String file, String element, List<String> branches) {
    if (this.lang == null) this.lang = lang;
    else if (lang != null) throw new IllegalArgumentException("invalid config");

    branches.forEach(this::addBranch);

    if (file != null) {
      if (this.file == null) {
        List<String> segments = MatcherUtils.matchSegments(file);
        if (segments == null) {
          logger.error("Invalid Element {}", label);
          throw new IllegalArgumentException("invalid config");
        }
        this.file = getPatternRoot(segments);
        //        this.captures.addAll(this.file.captures);
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
  }

  public SegmentPattern getBranchNode(String branch) {
    List<String> segments = MatcherUtils.matchSegments(branch);
    if (segments == null) {
      logger.error("Invalid Branch {}", branch);
      throw new IllegalArgumentException("invalid config");
    }
    return getPatternRoot(segments);
  }

  public void addAnchoredBranch(String anchor, String branch) {
    addToBranchMap(anchor, getBranchNode(branch));
  }

  public void addDefaultBranch(String branch) {
    defaultBranches.add(getBranchNode(branch));
  }

  public void addBranch(String branch) {
    SegmentPattern branchRoot = getBranchNode(branch);
    String firstSeg = branchRoot.text.text;
    if (firstSeg.startsWith("@")) {
      addToBranchMap(firstSeg.substring(1), branchRoot);
    } else {
      defaultBranches.add(branchRoot);
    }
  }

  private void addToBranchMap(String anchor, SegmentPattern branch) {
    if (!branches.containsKey(anchor)) {
      branches.put(anchor, new ArrayList<>());
    }
    branches.get(anchor).add(branch);
  }

  private SegmentPattern getPatternRoot(List<String> segments) {
    SegmentPattern root = null;
    SegmentPattern cur = null;
    if (segments == null || segments.isEmpty()) return null;
    for (String seg : segments) {
      SegmentPattern p = cur;
      if (MatcherUtils.isEdge(seg)) {
        cur = new SegmentPattern(SegmentType.EDGE, seg.substring(1, seg.length() - 1), branches);
      } else if (seg.length() == 0) {
        cur = new SegmentPattern(SegmentType.EDGE, "*", branches);
      } else {
        cur = new SegmentPattern(SegmentType.NODE, seg, branches);
      }
      cur.setParent(p);
      if (root == null) root = cur;
      this.captures.addAll(cur.captures);
    }
    cur.addDefaultBranches(defaultBranches);
    //    if (root.segType == SegmentType.EDGE && root.text.text.length() == 0)
    //      root = (SegmentPattern) root.parent;
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
