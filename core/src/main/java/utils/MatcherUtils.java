package utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import model.Language;
import org.apache.commons.lang3.tuple.Pair;

public class MatcherUtils {
  private static final Pattern labelPattern =
      Pattern.compile("(?<name>\\w+)(?:<(?<params>\\w+(?:,\\s*\\w+\\s*)?)>)?");
  private static final String quotePattern = "(?:[^\"])";
  private static final String singleQuotePattern = "(?:[^`])";
  private static final Pattern templatePattern =
      Pattern.compile(
          String.format(
              "(?<name>\\w+)(?:\\<(?<params>(?:\\w+|\"%s+\"|`%s+`)(?:,\\s*(?:\\w+|\"%s+\"|`%s+`)\\s*)?)\\>)?",
              quotePattern, singleQuotePattern, quotePattern, singleQuotePattern));
  private static final String escapedPattern = "(?:[^\\\\\\[\\]:#/]|\\\\[\\\\\\[\\]\\(\\):#/])";
  private static final String attrPattern =
      String.format("(?<key>\\w+)=(?<val>%s+)", escapedPattern);
  private static final Pattern uriPattern = Pattern.compile("(?<seg>.*?)(?:((?<!\\\\)/)|$)");
  private static final Pattern capPattern = Pattern.compile("\\((?<cap>\\w+)\\)");
  private static final Pattern attrsPattern =
      Pattern.compile(String.format("%s(?:,\\s*|$)", attrPattern));
  private static final Pattern edgePattern = Pattern.compile("^~.*~$");
  private static final Pattern segPattern =
      Pattern.compile(
          String.format(
              "^(?<content>%s*)(?:::(?<type>\\w+))?(?:#(?<anchor>\\w+))?(?:\\[(?<attrs>.+)\\])?$",
              escapedPattern));

  private static final Pattern predicatePattern =
      Pattern.compile("^(?<name>\\w+)\\((?<paras>.+)\\)$");

  private static final Pattern dollarFunctionPattern =
      Pattern.compile("\\$(?<funcName>\\w+)\\((?<args>.*)\\)");
  private static final Pattern parasPattern = Pattern.compile("(?<paraName>.+?)(?:,\\s*|$)");

  private static final Pattern configURIattr =
      Pattern.compile("^(?<uri>\\w+)((?:\\[(?<capName>.+)\\])|(?:\\.(?<layerName>.+)))$");

  private static final Pattern varPattern = Pattern.compile("^\\w+$");

  public static Set<String> matchCapturesInPattern(String identifier) {
    Set<String> matched = new HashSet<>();
    Matcher matcher = capPattern.matcher(identifier);
    while (matcher.find()) {
      matched.add(matcher.group("cap"));
    }
    return matched;
  }

  public static boolean isEdge(String seg) {
    Matcher matcher = edgePattern.matcher(seg);
    return matcher.find();
  }

  public static List<String> matchSegments(String uri) {
    List<String> res = new ArrayList<>();
    if (uri.length() == 0) {
      return res;
    }
    Matcher matcher = uriPattern.matcher(uri);
    while (matcher.find()) {
      res.add(matcher.group("seg"));
    }
    if (res.isEmpty()) return null;
    if (res.get(res.size() - 1).length() == 0) res.remove(res.size() - 1);
    return res;
  }

  public static Map<String, String> splitSegment(String seg) {
    Matcher matcher = segPattern.matcher(seg);
    Map<String, String> res = new HashMap<>();
    if (matcher.find()) {
      for (String label : new String[] {"content", "type", "anchor", "attrs"}) {
        String content = matcher.group(label);
        if (content != null) res.put(label, content);
      }
    } else {
      return null;
    }
    if (!res.containsKey("content")) return null;
    return res;
  }

  public static Map<String, String> matchAttributes(String attrs) {
    Matcher matcher = attrsPattern.matcher(attrs);
    Map<String, String> res = new HashMap<>();
    while (matcher.find()) {
      res.put(matcher.group("key"), matcher.group("val"));
    }
    if (res.isEmpty()) return null;
    return res;
  }

  public static Pair<String, List<String>> parsePredicate(String predicate) {
    Matcher nameMatcher = predicatePattern.matcher(predicate);
    String predicateName, params;
    List<String> paraNames = new ArrayList<>();
    if (nameMatcher.find()) {
      predicateName = nameMatcher.group("name");
      params = nameMatcher.group("paras");
    } else return null;

    int parenDepth = 0, start = 0;
    for (int i = 0; i < params.length(); i++) {
      if (params.charAt(i) == ',' && parenDepth == 0) {
        paraNames.add(params.substring(start, i).strip());
        start = i + 1;
      } else if (params.charAt(i) == '(') {
        parenDepth++;
      } else if (params.charAt(i) == ')') {
        parenDepth--;
      }
    }
    paraNames.add(params.substring(start).strip());
    return Pair.of(predicateName, paraNames);
  }

  public static Pair<String, Pair<Boolean, String>> parseURIRefAttrInConfig(String variable) {
    Matcher uriMatcher = configURIattr.matcher(variable);
    if (uriMatcher.find()) {
      return Pair.of(
          uriMatcher.group("uri"),
          uriMatcher.group("layerName") != null
              ? Pair.of(false, uriMatcher.group("layerName"))
              : Pair.of(true, uriMatcher.group("capName")));
    }
    return null;
  }

  public static boolean matchVariable(String variable) {
    Matcher matcher = varPattern.matcher(variable);
    return matcher.find();
  }

  public static Pair<String, List<String>> parseURIPatternLabel(String label) {
    Matcher labelMatcher = labelPattern.matcher(label);
    if (labelMatcher.find()) {
      String labelName = labelMatcher.group("name");
      String params = labelMatcher.group("params");
      if (params == null) return Pair.of(labelName, null);
      else {
        List<String> paramList =
            Arrays.stream(params.split(",")).map(String::strip).collect(Collectors.toList());
        return Pair.of(labelName, paramList);
      }
    } else {
      return null;
    }
  }

  public static Pair<String, List<String>> parseURIPatternTemplate(String template) {
    Matcher tempMatcher = templatePattern.matcher(template);
    if (tempMatcher.find()) {
      String labelName = tempMatcher.group("name");
      String params = tempMatcher.group("params");
      if (params == null) return Pair.of(labelName, null);
      else {
        List<String> paramList =
            Arrays.stream(params.split(",")).map(String::strip).collect(Collectors.toList());
        return Pair.of(labelName, paramList);
      }
    } else {
      return null;
    }
  }

  public static Pair<String, List<String>> parseDollarFunction(String source) {
    Matcher funcMatcher = dollarFunctionPattern.matcher(source);
    String funcName = null;
    List<String> argList = null;
    if (funcMatcher.find()) {
      funcName = funcMatcher.group("funcName");
      String args = funcMatcher.group("args");
      argList = new ArrayList<>();
      if (!args.isBlank()) {
        Matcher argMatcher = parasPattern.matcher(args);
        while (argMatcher.find()) {
          argList.add(argMatcher.group("paraName"));
        }
      }
    }
    return Pair.of(funcName, argList);
  }

  public static String cleanCommentsAndSpecSym(Language lang, String code) {
    String commentsRegex = "";
    switch (lang) {
      case JAVA:
      case XML:
        commentsRegex = "(\\\\\\\\.*\n|/\\*.*\\*/)";
    }
    code = code.replaceAll(commentsRegex, " ");
    code = code.replaceAll("[^\\w]", " ");
    code = code.replaceAll("\\s+", " ");
    return code;
  }
}
