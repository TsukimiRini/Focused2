import model.config.ConfigElement;
import model.URIPattern;
import model.config.ConfigLink;
import model.config.ConfigLinkBlock;
import model.enums.LogicRelationType;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ConfigLoader {
  public static final List<String> attributes =
      Arrays.asList("file", "lang", "element", "branches");
  public static List<String> elementsContent = new ArrayList<>();
  public static List<String> linksContent = new ArrayList<>();

  public static Pair<List<URIPattern>, List<ConfigLinkBlock>> load(String config_path)
      throws IOException {
    readContent(config_path);
    Map<String, ConfigElement> rules = parseElements();
    Map<String, ConfigLinkBlock> blocks = parseLinks(new ArrayList<>(rules.keySet()));
    List<URIPattern> patterns = new ArrayList<>();
    for (String label : rules.keySet()) {
      ConfigElement curRule = rules.get(label);
      patterns.add(
          new URIPattern(
              label,
              curRule.get("lang"),
              curRule.get("file"),
              curRule.get("element"),
              curRule.branches));
    }

    return Pair.of(patterns, new ArrayList<>(blocks.values()));
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

  private static Map<String, ConfigElement> parseElements() {
    String curLabel = null;
    Map<String, ConfigElement> res = new HashMap<>();
    for (String line : elementsContent) {
      line = line.strip();
      if (line.startsWith("-")) {
        if (curLabel == null) {
          throw new IllegalArgumentException("invalid config");
        } else {
          res.get(curLabel).addBranch(line.substring(2));
        }
      } else {
        int idx = line.indexOf(':');
        String labelOrAttr = line.substring(0, idx);
        if (attributes.contains(labelOrAttr)) {
          if (labelOrAttr.equals(attributes.get(attributes.size() - 1))) continue;
          String val = line.substring(idx + 1);
          if (curLabel == null) {
            throw new IllegalArgumentException("invalid config");
          } else {
            res.get(curLabel).put(labelOrAttr, val);
          }
        } else {
          curLabel = labelOrAttr;
          res.put(curLabel, new ConfigElement());
        }
      }
    }
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
            line.substring(i), and ? LogicRelationType.AND : LogicRelationType.OR, (depth - 8) / 4, opCnt);
      }
    }
    return blocks;
  }
}
