package model;

import java.util.Arrays;

public enum Language {
  CLike(".c.h.cpp.cc.hpp", "CLike"),
  C(".c", "C"),
  CPP(".cpp", "CPP"),
  CC(".cc", "CC"),
  H(".h", "H"),
  HPP(".h", "HPP"),
  Python(".py", "Python"),
  JAVA(".java", "JAVA"),
  XML(".xml", "XML"),
  HTML(".html", "HTML"),
  JSP(".jsp", "JSP"),
  FTL(".ftl", "FTL"),
  SQL(".sql", "SQL"),
  CSS(".css", "CSS"),
  FILE(".file", "FILE"),
  Rust(".rs", "Rust"),
  ANY("*", "ANY"),
  OTHER("?", "?");

  public String extension;
  public String identifier;

  Language(String extension, String identifier) {
    this.extension = extension;
    this.identifier = identifier;
  }

  public static Language valueOfLabel(String s) {
    return Arrays.stream(Language.values())
        .filter(
            l ->
                (l.extension.equals(s)
                    || ("*" + l.extension).equals(s)
                    || l.extension.endsWith(s)
                    || l.identifier.toLowerCase().equals(s.toLowerCase())))
        .findFirst()
        .orElse(Language.FILE);
  }

  public String getIdentifier() {
    return identifier;
  }
}
