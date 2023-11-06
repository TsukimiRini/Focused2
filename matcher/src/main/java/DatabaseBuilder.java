import model.*;
import model.TreeInfo.TreeInfoConf;
import model.URI.URIEdge;
import model.URI.URINode;
import model.URI.URISegment;
import model.config.ConfigLinkBlock;
import org.apache.commons.lang3.tuple.Pair;
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
  public static final String framework = "android";
  public static final String projectName = "CloudReader";
  public static final String projectDir =
      System.getProperty("user.home") + "/coding/xll/android/" + projectName;
  //  public static final String projectDir =
  //      System.getProperty("user.dir") + "/matcher/src/main/resources/toy_android";
  public static final String outputDir =
      System.getProperty("user.home") + "/coding/dlData/android/" + projectName + "/inputs";

  public static Map<Language, TreeInfoConf> treeInfoConfs = new HashMap<>();

  public static Map<Language, URINode> uriTrees = new HashMap<>();

  protected static Logger logger = LoggerFactory.getLogger(DatabaseBuilder.class);

  public static void main(String[] args) throws IOException {
    SharedStatus.initProjectInfo(framework, projectDir);
    mapConfs();

    // get cst
    Map<Language, Map<String, CSTTree>> cstForLangs = CSTBuilder.buildCST();

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
        patternLang = Language.valueOfLabel("." + pattern.lang.toLowerCase());
      }
      if (patternLang != Language.ANY)
        instances = getInstancesFromSingleTree(patternLang, pattern, uriTrees.get(patternLang));
      else {
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
      if (pattern.elementRoot == null) return fileNodes;
      for (ElementInstance fileNode : fileNodes) {
        res.addAll(
            matchPatternLayer(fileNode, pattern.elementRoot, fileNode.file, MatchedType.ELEMENT));
        wildCardEdge.child = pattern.elementRoot;
        res.addAll(matchPatternLayer(fileNode, wildCardRoot, fileNode.file, MatchedType.ELEMENT));
      }
    } catch (Exception e) {
      e.printStackTrace();
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
    SegmentPattern curPattern = (SegmentPattern) (pattern.text.text.equals("**")? pattern.child.child: pattern);

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
            childInst = checkEdge((SegmentPattern) curPattern.parent, child.edgeToParent, childInst);
          }

          SegmentPattern nextNodePattern = (SegmentPattern) curPattern.child;
          if (curPattern.child != null && curPattern.child.segType == SegmentType.EDGE) {
            nextNodePattern = (SegmentPattern) nextNodePattern.child;
          }

          if (childInst != null) {
            // find all possible children
            Set<ElementInstance> childSet =
                matchPatternLayer(
                    childInst, curPattern.child == null ? null : nextNodePattern, child, checkObject);

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
      String regex = replaceCapture(text, instance);
      try {
        return Pattern.compile("^" + regex + "$");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private static String replaceCapture(IdentifierPattern text, ElementInstance instance) {
    String regex = StringUtil.escapedForRegex(text.text);
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

    return regex;
  }

  private static ElementInstance checkAttr(
      SegmentPattern pattern, URISegment node, ElementInstance instance)
      throws CloneNotSupportedException {
    // should get new instance if updated
    ElementInstance finalInst = instance;

    // check attributes
    for (String attributeName : pattern.attributes.keySet()) {
      if (!node.containsKey(attributeName)) return null;
      IdentifierPattern attrVal = pattern.attributes.get(attributeName);
      String regex = replaceCapture(attrVal, instance);
      Pattern regexPattern = Pattern.compile(regex);
      Matcher matcher = regexPattern.matcher(node.get(attributeName));
      if (!matcher.find()) return null;

      // check captures in attr val
      for (String capName : attrVal.captures) {
        if (!instance.hasCapture(capName)) {
          if (finalInst == instance) {
            finalInst = instance.clone();
          }
          finalInst.addCapture(capName, matcher.group(capName));
        }
      }
    }

    return finalInst;
  }

  private static ElementInstance checkEdge(
      SegmentPattern pattern, URIEdge node, ElementInstance instance)
      throws CloneNotSupportedException {
    if (checkType(pattern.text, node.type)) return checkAttr(pattern, node, instance);
    return null;
  }

  private static Set<ElementInstance> checkBranches(
      SegmentPattern pattern, URINode node, ElementInstance instance)
      throws CloneNotSupportedException {
    Set<ElementInstance> res = Set.of(instance);
    for (SegmentPattern branchPattern : pattern.branches) {
      Set<ElementInstance> curBranch = new HashSet<>();
      for (ElementInstance iterateInst : res) {
        curBranch.addAll(
            matchPatternLayer(
                iterateInst, (SegmentPattern) branchPattern.child, node, MatchedType.BRANCH));
        if (curBranch.isEmpty()) return null;
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
