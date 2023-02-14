package model;

import java.util.Arrays;

public enum Language {
  C(".c"),
  CPP(".cpp"),
  HPP(".h"),
  JAVA(".java"),
  XML(".xml"),
  HTML(".html"),
  JSP(".jsp"),
  FTL(".ftl"),
  SQL(".sql"),
  FILE(".file"),
  ANY("*"),
  OTHER("?");

  public String extension;

  Language(String extension) {
    this.extension = extension;
  }

  public static Language valueOfLabel(String s) {
    return Arrays.stream(Language.values())
            .filter(
                    l ->
                            (l.extension.equals(s)
                                    || ("*" + l.extension).equals(s)
                                    || l.extension.endsWith(s)))
            .findFirst()
            .orElse(Language.FILE);
  }
}
