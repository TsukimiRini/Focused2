import me.tongfei.progressbar.ProgressBar;
import model.*;
import model.TreeInfo.TreeInfoConf;
import model.URI.URIEdge;
import model.URI.URINode;
import model.URI.URISegment;
import model.config.ConfigLinkBlock;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.FileUtil;
import utils.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseBuilder {
  public static final String framework = "cpython";
  public static final String projectName = "pytorch";
  public static final String projectDir =
      System.getProperty("user.home") + "/coding/xll-gt/" + framework + "/projects/" + projectName;

  //  public static final String projectDir =
  //      System.getProperty("user.dir") + "/matcher/src/main/resources/toy_android";
  public static final String outputDir =
      System.getProperty("user.home")
          + "/coding/dlData/"
          + framework
          + "/"
          + projectName
          + "/inputs";

  public static Map<Language, TreeInfoConf> treeInfoConfs = new HashMap<>();

  public static Map<Language, URINode> uriTrees = new HashMap<>();

  protected static Logger logger = LoggerFactory.getLogger(DatabaseBuilder.class);

  static {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
  }

  public static void main(String[] args) throws IOException {
    SharedStatus.initProjectInfo(framework, projectDir);
    mapConfs();

    // get cst
    Map<Language, Map<String, CSTTree>> cstForLangs = null;
    if (framework.equals("web") || framework.equals("cpython"))
      cstForLangs = CSTBuilderNG.buildCST();
    else cstForLangs = CSTBuilder.buildCST();

    // cst -> ast
    for (Language lang : cstForLangs.keySet()) {
      Map<String, CSTTree> cstTree = cstForLangs.get(lang);
      URITreeBuilder builder = new URITreeBuilder(treeInfoConfs.get(lang));
      URINode tree = builder.buildFromCST(cstTree);
      uriTrees.put(lang, tree);
    }

    // load focused config
    Pair<List<URIPattern>, List<ConfigLinkBlock>> config =
        ConfigLoader.load(SharedStatus.projectInfo.configPath);

    // match uris for each rule
    for (URIPattern pattern : config.getLeft()) {
      Set<ElementInstance> instances;
      Language patternLang = Language.ANY;
      if (pattern.lang != null) {
        patternLang = Language.valueOfLabel(pattern.lang);
      }
      if (patternLang != Language.ANY) {
        List<Language> targetLangs = new ArrayList<>();
        if (patternLang == Language.CLike) {
          targetLangs =
              Arrays.asList(Language.C, Language.CPP, Language.HPP, Language.CC, Language.H);
        } else {
          targetLangs.add(patternLang);
        }
        instances = new HashSet<>();
        for (Language lang : targetLangs) {
          instances.addAll(getInstancesFromSingleTree(lang, pattern, uriTrees.get(lang)));
        }
      } else {
        instances = new HashSet<>();
        uriTrees.forEach(
            (lang, tree) -> instances.addAll(getInstancesFromSingleTree(lang, pattern, tree)));
      }

      outputInstances(pattern, instances, outputDir + "/" + pattern.label + ".facts");
    }
  }

  private static void mapConfs() {
    for (Language lang : SharedStatus.projectInfo.languages) {
      switch (lang) {
        case JAVA:
          treeInfoConfs.put(
              lang,
              new TreeInfoConf(
                  System.getProperty("user.dir") + "/parser/src/main/resources/java.tree"));
          break;
        case XML:
          treeInfoConfs.put(
              lang,
              new TreeInfoConf(
                  System.getProperty("user.dir") + "/parser/src/main/resources/xml.tree"));
          break;
        case HTML:
          treeInfoConfs.put(
              lang,
              new TreeInfoConf(
                  System.getProperty("user.dir") + "/parser/src/main/resources/html.tree"));
          break;
        case CSS:
          treeInfoConfs.put(
              lang,
              new TreeInfoConf(
                  System.getProperty("user.dir") + "/parser/src/main/resources/css.tree"));
          break;
        case CLike:
        case C:
        case H:
        case CC:
        case CPP:
        case HPP:
          treeInfoConfs.put(
              lang,
              new TreeInfoConf(
                  System.getProperty("user.dir") + "/parser/src/main/resources/cpp.tree"));
          break;
        case Python:
          treeInfoConfs.put(
              lang,
              new TreeInfoConf(
                  System.getProperty("user.dir") + "/parser/src/main/resources/python.tree"));
          break;
        default:
          throw new IllegalArgumentException("language not supported");
      }
    }
  }

  private static Set<ElementInstance> getInstancesFromSingleTree(
      Language lang, URIPattern pattern, URINode tree) {
    Set<ElementInstance> res = new HashSet<>();
    try {
      Set<ElementInstance> fileNodes =
          matchPatternLayer(new ElementInstance(lang), pattern.file, tree, MatchedType.FILE);
      SegmentPattern wildCardRoot = new SegmentPattern(SegmentType.NODE, "**");
      SegmentPattern wildCardEdge = new SegmentPattern(SegmentType.EDGE, "*");
      wildCardRoot.child = wildCardEdge;
      wildCardEdge.child = pattern.file;
      fileNodes.addAll(
          matchPatternLayer(new ElementInstance(lang), wildCardRoot, tree, MatchedType.FILE));
      //      SegmentPattern wildCardRoot = new SegmentPattern(SegmentType.NODE, "**");
      //      SegmentPattern wildCardEdge = new SegmentPattern(SegmentType.EDGE, "*");
      //      wildCardEdge.setParent(wildCardRoot);
      //      pattern.file.setParent(wildCardEdge);
      //      Set<ElementInstance> fileNodes =
      //          matchPatternFromBottom(new ElementInstance(lang), wildCardRoot, tree,
      // MatchedType.FILE);
      if (pattern.elementRoot == null) return fileNodes;
      for (ElementInstance fileNode :
          ProgressBar.wrap(
              fileNodes, "Matching pattern " + pattern.label + " in " + lang.toString())) {
        pattern.elementRoot.setParent(wildCardEdge);
        res.addAll(matchPatternFromBottom(fileNode, wildCardRoot, fileNode.file));
        //        wildCardEdge.child = pattern.elementRoot;
        //        res.addAll(matchPatternFromBottom(fileNode, wildCardRoot, fileNode.file));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return res;
  }

  private static Set<ElementInstance> matchPatternFromBottom(
      ElementInstance fileNode, SegmentPattern pattern, URINode tree)
      throws CloneNotSupportedException {
    return matchPatternFromBottom(fileNode, pattern, tree, MatchedType.ELEMENT);
  }

  private static Set<ElementInstance> matchPatternFromBottom(
      ElementInstance fileNode, SegmentPattern pattern, URINode tree, MatchedType matchedType)
      throws CloneNotSupportedException {
    Set<ElementInstance> res = new HashSet<>();
    SegmentPattern tail = (SegmentPattern) pattern.getTail();
    Set<ElementInstance> filteredNodes = new HashSet<>();
    getNodesByNodePattern(tree, tail, fileNode, matchedType, filteredNodes);
    filteredNodes.forEach(
        node -> {
          try {
            URINode curNode = matchedType == MatchedType.FILE ? node.file : node.element;
            res.addAll(matchPatternLayerReverse(node, tail, curNode));
          } catch (CloneNotSupportedException e) {
            e.printStackTrace();
          }
        });
    return res;
  }

  private static Set<ElementInstance> matchPatternLayerReverse(
      ElementInstance inst, SegmentPattern pattern, URINode tree)
      throws CloneNotSupportedException {
    if (pattern == null || pattern.text.text.equals("**") && pattern.parent == null) {
      return Set.of(inst);
    }
    if (pattern.segType == SegmentType.EDGE) {
      throw new IllegalArgumentException("the first segment of the pattern should be node pattern");
    }

    Set<ElementInstance> res = new HashSet<>();

    if (tree == null) return res;
    else if (pattern.segType == SegmentType.NODE && (tree.isDir() || tree.type.equals("FILE")))
      return res;

    // match text
    SegmentPattern curPattern =
        (SegmentPattern) (pattern.text.text.equals("**") ? pattern.parent.parent : pattern);
    Pattern textPattern = getTextPattern(curPattern, inst);
    Matcher matcher = textPattern.matcher(tree.getName());
    if (matcher.matches()) {
      ElementInstance newInst = inst.clone();
      for (String capture : curPattern.text.captures) {
        if (!newInst.capVal.containsKey(capture)) {
          try {
            newInst.addCapture(capture, matcher.group(capture));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }

      // match type and attr
      if (!checkType(curPattern.type, tree.type)) newInst = null;
      newInst = checkAttr(curPattern, tree, newInst);

      // match edge
      newInst = checkEdge((SegmentPattern) curPattern.parent, tree.edgeToParent, newInst);

      // match branches
      //      Set<ElementInstance> newInsts = new HashSet<>();
      //      if (newInst != null && !curPattern.branches.isEmpty()) {
      //        newInsts.addAll(checkBranchesFromRoot(curPattern, tree, newInst));
      //      } else if (newInst != null) {
      //        newInsts.add(newInst);
      //      }

      Set<ElementInstance> newInsts = checkBranches(curPattern, tree, newInst);
      if (newInsts == null) newInsts = new HashSet<>();

      newInsts.forEach(
          each -> {
            try {
              res.addAll(
                  matchPatternLayerReverse(
                      each, (SegmentPattern) curPattern.parent.parent, tree.getParent()));
            } catch (CloneNotSupportedException e) {
              e.printStackTrace();
            }
          });
    }

    if (pattern.text.text.equals("**")) {
      res.addAll(matchPatternLayerReverse(inst, pattern, tree.getParent()));
    }

    return res;
  }

  private static Set<ElementInstance> matchPatternLayer(
      ElementInstance inst, SegmentPattern pattern, URINode tree, MatchedType checkObject)
      throws CloneNotSupportedException {
    if (pattern == null) {
      ElementInstance newInstance = inst.clone();
      switch (checkObject) {
        case FILE:
          if (tree.isDir()) return new HashSet<>();
          newInstance.setFilePath(tree);
          break;
        case ELEMENT:
          newInstance.element = tree;
          break;
        case BRANCH:
          newInstance.addBranch(tree);
          break;
      }
      return Set.of(newInstance);
    }

    if (pattern.segType == SegmentType.EDGE) {
      throw new IllegalArgumentException("the first segment of the pattern should be node pattern");
    }

    if (tree == null || checkObject == MatchedType.FILE && !tree.isDir()) return new HashSet<>();

    Set<ElementInstance> res = new HashSet<>();
    Pattern textPattern = getTextPattern(pattern, inst);
    SegmentPattern curPattern =
        (SegmentPattern) (pattern.text.text.equals("**") ? pattern.child.child : pattern);

    for (String childName : tree.children.keySet()) {
      Matcher matcher = textPattern.matcher(childName);
      if (matcher.find()) {
        ElementInstance newInstance = inst.clone();
        // fill in captures of new instance
        for (String capName : curPattern.text.captures) {
          if (!newInstance.capVal.containsKey(capName)) {
            newInstance.capVal.put(capName, matcher.group(capName));
          }
        }
        // iterate for child
        for (URINode child : tree.children.get(childName)) {
          // check type
          if (!checkType(curPattern.type, child.type)) continue;

          // check attribute
          ElementInstance childInst = checkAttr(curPattern, child, newInstance);

          if (curPattern.parent != null && curPattern.parent.segType == SegmentType.EDGE) {
            childInst =
                checkEdge((SegmentPattern) curPattern.parent, child.edgeToParent, childInst);
          }

          SegmentPattern nextNodePattern = (SegmentPattern) curPattern.child;
          if (curPattern.child != null && curPattern.child.segType == SegmentType.EDGE) {
            nextNodePattern = (SegmentPattern) nextNodePattern.child;
          }

          if (childInst == null) continue;

          // find all possible children
          Set<ElementInstance> childSet =
              matchPatternLayer(
                  childInst, curPattern.child == null ? null : nextNodePattern, child, checkObject);

          if (checkObject == MatchedType.FILE || checkObject == MatchedType.BRANCH) {
            res.addAll(childSet);
            continue;
          }

          // check branch
          for (ElementInstance toCheck : childSet) {
            Set<ElementInstance> checkedSet = checkBranches(curPattern, child, toCheck);
            if (checkedSet != null) {
              res.addAll(checkedSet);
            }
          }
        }
      }
    }

    // ignore current layer if current seg pattern is "**"
    if (pattern.text.text.equals("**")) {
      for (List<URINode> children : tree.children.values()) {
        for (URINode child : children)
          res.addAll(matchPatternLayer(inst, pattern, child, checkObject));
      }
    }

    return res;
  }

  public static Set<ElementInstance> getNodesByNodePattern(
      URINode tree, SegmentPattern pattern, ElementInstance inst)
      throws CloneNotSupportedException {
    Set<ElementInstance> res = new HashSet<>();
    getNodesByNodePattern(tree, pattern, inst, MatchedType.ELEMENT, res);
    return res;
  }

  public static Boolean getNodesByNodePattern(
      URINode tree,
      SegmentPattern pattern,
      ElementInstance inst,
      MatchedType type,
      Set<ElementInstance> res)
      throws CloneNotSupportedException {
    if (type == MatchedType.FILE && !tree.isDir()) return true;

    //    Set<ElementInstance> res = new HashSet<>();
    if (pattern.text.text.equals("**")) {
      throw new IllegalArgumentException("the first segment of the pattern should not be **");
    }
    Pattern textPattern = getTextPattern(pattern, inst);
    boolean continueFlag = true;
    for (String childName : tree.children.keySet()) {
      // iterate for child
      for (URINode child : tree.children.get(childName)) {
        continueFlag &= getNodesByNodePattern(child, pattern, inst, type, res);
      }
    }
    if (!continueFlag) return false;

    if (tree.getName() == null) return true;
    if (!checkType(pattern.type, tree.type)) {
      return true;
    }
    Matcher matcher = textPattern.matcher(tree.getName());
    boolean matched = matcher.find();
    if (matched) {
      ElementInstance newInstance = inst.clone();

      // check attribute
      ElementInstance childInst = checkAttr(pattern, tree, newInstance);

      if (pattern.parent != null && pattern.parent.segType == SegmentType.EDGE) {
        childInst = checkEdge((SegmentPattern) pattern.parent, tree.edgeToParent, childInst);
      }

      if (childInst != null) {
        if (type == MatchedType.FILE) childInst.setFilePath(tree);
        else if (type == MatchedType.ELEMENT) childInst.element = tree;
        for (String capture : pattern.text.captures) {
          if (!childInst.capVal.containsKey(capture)) {
            childInst.addCapture(capture, matcher.group(capture));
          }
        }
        res.add(childInst);
      } else if (pattern.isLeafType()) {
        return false;
      }
    } else if (pattern.isLeafType()) {
      return false;
    }
    return true;
  }

  private static Pattern getTextPattern(SegmentPattern pattern, ElementInstance instance) {
    if (pattern.text == null) {
      logger.error("text pattern is null");
      throw new IllegalStateException("text pattern is null");
    }
    IdentifierPattern text = pattern.text;
    if ("**".equals(text.text)) {
      return getTextPattern((SegmentPattern) pattern.child.child, instance);
    } else {
      // TODO: same capture may happen
      String regex = replaceCaptureAndRemoveEscape(text, instance);
      try {
        return Pattern.compile("^" + regex + "$");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private static String replaceCaptureAndRemoveEscape(
      IdentifierPattern text, ElementInstance instance) {
    String regex = StringUtil.escapedForRegex(text.text, false);
    regex = regex.replaceAll("(?<!\\\\)\\*", ".+");
    for (String capName : text.captures) {
      // TODO: corner case -- when capVal contains variable enclosed by brackets with the same name
      // as captures
      if (instance.capVal.containsKey(capName)) {
        String replaceVal =
            StringUtil.escapedForRegex(StringUtil.escapedForRegex(instance.capVal.get(capName)));
        regex = regex.replaceAll("\\\\\\(" + capName + "\\\\\\)", replaceVal);
      } else regex = regex.replaceAll("\\\\\\(" + capName + "\\\\\\)", "(?<" + capName + ">.+)");
    }

    regex = regex.replaceAll("\\\\\\\\([\\(\\)*])", "$1");

    return regex;
  }

  private static ElementInstance checkAttr(
      SegmentPattern pattern, URISegment node, ElementInstance instance)
      throws CloneNotSupportedException {
    // should get new instance if updated
    if (instance == null) {
      return null;
    }
    ElementInstance finalInst = instance.clone();

    // check attributes
    for (String attributeName : pattern.attributes.keySet()) {
      if (!node.containsKey(attributeName)) return null;
      IdentifierPattern attrVal = pattern.attributes.get(attributeName);
      String regex = replaceCaptureAndRemoveEscape(attrVal, instance);
      Pattern regexPattern = Pattern.compile(regex);
      Matcher matcher = regexPattern.matcher(node.get(attributeName));
      if (!matcher.find()) return null;

      // check captures in attr val
      for (String capName : attrVal.captures) {
        if (!instance.hasCapture(capName)) {
          finalInst.addCapture(capName, matcher.group(capName));
        }
      }
    }

    return finalInst;
  }

  private static ElementInstance checkEdge(
      SegmentPattern pattern, URIEdge node, ElementInstance instance)
      throws CloneNotSupportedException {
    if (instance == null) {
      return null;
    }
    if (checkType(pattern.text, node.type)) return checkAttr(pattern, node, instance);
    return null;
  }

  private static Set<ElementInstance> checkBranches(
      SegmentPattern pattern, URINode node, ElementInstance instance)
      throws CloneNotSupportedException {
    if (instance == null) return null;
    Set<ElementInstance> res = Set.of(instance);
    for (SegmentPattern branchPattern : pattern.branches) {
      Set<ElementInstance> curBranch = new HashSet<>();
      for (ElementInstance iterateInst : res) {
        curBranch.addAll(
            matchPatternLayer(
                iterateInst, (SegmentPattern) branchPattern.child, node, MatchedType.BRANCH));
        curBranch.removeIf(b -> b.branches.contains(b.element));
        if (curBranch.isEmpty()) return null;
      }
      res = curBranch;
    }
    return res;
  }

  private static Set<ElementInstance> checkBranchesFromRoot(
      SegmentPattern pattern, URINode node, ElementInstance instance)
      throws CloneNotSupportedException {
    Set<ElementInstance> res = Set.of(instance);
    for (SegmentPattern branchPattern : pattern.branches) {
      Set<ElementInstance> curBranch = new HashSet<>();
      Set<ElementInstance> candidates =
          getNodesByNodePattern(node, (SegmentPattern) branchPattern.getTail(), instance);
      for (ElementInstance iterateInst : res) {
        for (ElementInstance candidate : candidates) {
          Set<ElementInstance> branches =
              matchPatternLayerReverse(
                  candidate, (SegmentPattern) branchPattern.getTail(), candidate.element);
          branches.forEach(
              b -> {
                try {
                  if (b.hasConflictWith(iterateInst)) return;
                  ElementInstance newInst = iterateInst.clone();
                  newInst.capVal.putAll(b.capVal);
                  newInst.addBranch(b.element);
                  curBranch.add(newInst);
                } catch (CloneNotSupportedException e) {
                  e.printStackTrace();
                }
              });
        }
      }
      res = curBranch;
    }
    return res;
  }

  private static boolean checkType(IdentifierPattern typePattern, String type) {
    if (typePattern == null || typePattern.toString().equals("*")) return true;
    if (type == null) return false;
    return typePattern.toString().equals(type);
  }

  private static void outputInstances(
      URIPattern pattern, Set<ElementInstance> instances, String outputPath) throws IOException {
    File outputFile = FileUtil.createOrClearFile(outputPath);
    for (ElementInstance instance : instances) {
      FileUtil.appendTo(outputFile, instance.toOutputString(pattern) + "\n");
    }
  }
}
