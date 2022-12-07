package model;

import utils.MatcherUtils;

import java.util.HashSet;
import java.util.Set;

public class IdentifierPattern extends IdentifierBase {
  public Set<String> captures = new HashSet<>();

  public IdentifierPattern(String text) {
    super(text);
    captures.addAll(MatcherUtils.matchCapturesInPattern(text));
  }
}
