import model.config.ConfigElement;
import model.URIPattern;
import model.config.ConfigLink;
import model.config.ConfigLinkBlock;
import model.enums.LogicRelationType;
import org.apache.commons.lang3.tuple.Pair;
import utils.MatcherUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigLoader {
  public static final List<String> attributes =
      Arrays.asList("file", "lang", "element", "branches");
  public static List<String> elementsContent = new ArrayList<>();
  public static List<String> linksContent = new ArrayList<>();

  // TODO: separate element parsing and block parsing
  public static Pair<List<URIPattern>, List<ConfigLinkBlock>> load(String config_path)
      throws IOException {
    readContent(config_path);
    List<Pair<String, ConfigElement>> rules = parseElements();
    Map<String, ConfigLinkBlock> blocks =
        parseLinks(rules.stream().map(Pair::getKey).collect(Collectors.toList()));
    Map<String, URIPattern> templates = new HashMap<>();
    Map<String, URIPattern> patterns = new HashMap<>();
    for (Pair<String, ConfigElement> rule : rules) {
      String label = rule.getKey();
      ConfigElement curRule = rule.getValue();
      URIPattern curPattern;
      if (curRule.template != null) {
        curPattern =
            new URIPattern(
                templates.get(curRule.template.getKey()),
                curRule.template.getValue(),
                label,
                curRule.get("lang"),
                curRule.get("file"),
                curRule.get("element"),
                curRule.params,
                curRule.branches);
      } else {
        curPattern =
            new URIPattern(
                label,
                curRule.get("lang"),
                curRule.get("file"),
                curRule.get("element"),
                curRule.params,
                curRule.branches);
      }

      if (curRule.params != null) {
        templates.put(label, curPattern);
      } else {
        templates.put(label, curPattern);
        patterns.put(label, curPattern);
      }
    }

    return Pair.of(new ArrayList<>(patterns.values()), new ArrayList<>(blocks.values()));
  }

  private static void readContent(String config_path) throws IOException {
    elementsContent.clear();
    BufferedReader reader = new BufferedReader(new FileReader(config_path));
    String line;
    List<String> content = elementsContent;
    while ((line = reader.readLine()) != null) {
      if (line.equals("[Elements]")) continue;
      if (line.equals("[Links]")) {
        content = linksContent;
        continue;
      }
      if (line.length() > 0) content.add(line);
    }
  }

  private static List<Pair<String, ConfigElement>> parseElements() {
    String curLabel = null;
    ConfigElement curRule = null;
    List<Pair<String, ConfigElement>> res = new ArrayList<>();
    for (String line : elementsContent) {
      line = line.strip();
      if (line.startsWith("-")) {
        if (curLabel == null) {
          throw new IllegalArgumentException("invalid config");
        } else {
          curRule.addBranch(line.substring(2));
        }
      } else {
        int idx = line.indexOf(':');
        String labelOrAttr = line.substring(0, idx);
        if (attributes.contains(labelOrAttr)) {
          if (labelOrAttr.equals(attributes.get(attributes.size() - 1))) continue;
          String val = line.substring(idx + 1).strip();
          if (curLabel == null) {
            throw new IllegalArgumentException("invalid config");
          } else {
            curRule.put(labelOrAttr, val);
          }
        } else {
          Pair<String, List<String>> labelObj = MatcherUtils.parseURIPatternLabel(labelOrAttr);
          Pair<String, List<String>> template = null;
          if (line.length() > idx + 1)
            template = MatcherUtils.parseURIPatternTemplate(line.substring(idx + 1).strip());
          if (labelObj == null) {
            throw new IllegalArgumentException("invalid config");
          } else {
            if (curLabel != null) res.add(Pair.of(curLabel, curRule));
            curLabel = labelObj.getKey();
            curRule = new ConfigElement(labelObj.getValue(), template);
          }
        }
      }
    }
    if (curLabel != null) res.add(Pair.of(curLabel, curRule));
    return res;
  }

  private static Map<String, ConfigLinkBlock> parseLinks(List<String> elementPredicates) {
    String curBlockLabel = null;
    Map<String, ConfigLinkBlock> blocks = new HashMap<>();
    ConfigLinkBlock curBlock = null;
    ConfigLink curLink = null;
    boolean isPublic = false;
    for (String line : linksContent) {
      String lineStrip = line.strip();
      if (lineStrip.endsWith("{")) {
        lineStrip = lineStrip.replace(",", " ");
        String[] idtfs = lineStrip.substring(0, lineStrip.length() - 1).split(" ");
        curBlockLabel = idtfs[0];
        curBlock = new ConfigLinkBlock(curBlockLabel);
        for (int i = 1; i < idtfs.length; i++) {
          if (idtfs[i].length() != 0 && !idtfs[i].equals("<<")) {
            if (!blocks.containsKey(idtfs[i])) {
              throw new IllegalArgumentException("illegal config");
            }
            curBlock.addBaseBlock(blocks.get(idtfs[i]));
          }
        }
      } else if (lineStrip.equals("}")) {
        if (curBlock == null | curBlockLabel == null) {
          throw new IllegalArgumentException("illegal config");
        }
        curBlock.getParaTypes(elementPredicates);
        blocks.put(curBlockLabel, curBlock);
      } else if (lineStrip.equals("public:")) {
        isPublic = true;
      } else if (lineStrip.equals("private:")) {
        isPublic = false;
      } else if (lineStrip.endsWith(":")) {
        curLink = new ConfigLink(lineStrip.substring(0, lineStrip.length() - 1).strip());
        if (curBlock == null) {
          throw new IllegalArgumentException("illegal config");
        }
        curBlock.addPredicateDecl(curLink, isPublic);
      } else if (lineStrip.startsWith("&") || lineStrip.startsWith("|")) {
        if (curLink == null) {
          throw new IllegalArgumentException("illegal config");
        }
        boolean and = true;
        int i, depth = 0, opCnt = 0;
        for (i = 0; i < line.length(); i++) {
          if (line.charAt(i) == '&') {
            and = true;
            depth = i;
            opCnt++;
          } else if (line.charAt(i) == '|') {
            and = false;
            depth = i;
            opCnt++;
          } else if (line.charAt(i) != ' ') break;
        }
        curLink.addCondition(
            line.substring(i),
            and ? LogicRelationType.AND : LogicRelationType.OR,
            (depth - 8) / 4,
            opCnt);
      }
    }
    return blocks;
  }
}
