package model;

import utils.MatcherUtils;

import java.util.HashSet;
import java.util.Set;

public class IdentifierPattern extends IdentifierBase {
  public Set<String> captures = new HashSet<>();

  public IdentifierPattern(String text) {
    super(text.replaceAll("\\\\([\\\\\\[\\]:#/])", "$1"));
    captures.addAll(MatcherUtils.matchCapturesInPattern(text));
  }

  @Override
  public String toString() {
    return text.replaceAll("([\\\\\\[\\]:#/])", "\\\\$1");
  }
}
