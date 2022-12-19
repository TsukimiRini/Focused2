package utils;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

public class MatcherUtils {
  private static final String escapedPattern = "(?:[^\\\\\\[\\]:=,#/]|\\\\[\\\\\\[\\]:=,#/])";
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
  private static final Pattern parasPattern = Pattern.compile("(?<paraName>\\w+?)(?:,\\s*|$)");

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
      res.add("**");
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
}
