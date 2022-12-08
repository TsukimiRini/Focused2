import model.ConfigRule;
import model.URIPattern;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ConfigLoader {
  public static final List<String> attributes =
      Arrays.asList("file", "lang", "element", "branches");
  public static List<String> configContent = new ArrayList<>();

  public static List<URIPattern> load(String config_path) throws IOException {
    readContent(config_path);
    Map<String, ConfigRule> rules = parse();
    List<URIPattern> patterns = new ArrayList<>();
    for (String label : rules.keySet()) {
      ConfigRule curRule = rules.get(label);
      patterns.add(
          new URIPattern(
              label,
              curRule.get("lang"),
              curRule.get("file"),
              curRule.get("element"),
              curRule.branches));
    }

    return patterns;
  }

  private static void readContent(String config_path) throws IOException {
    configContent.clear();
    BufferedReader reader = new BufferedReader(new FileReader(config_path));
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.strip();
      if (line.length() > 0) configContent.add(line);
    }
  }

  private static Map<String, ConfigRule> parse() {
    String curLabel = null;
    Map<String, ConfigRule> res = new HashMap<>();
    for (String line : configContent) {
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
          res.put(curLabel, new ConfigRule());
        }
      }
    }
    return res;
  }
}
