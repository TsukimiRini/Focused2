package models;

import model.Language;
import model.Range;

public class CodeSource {
  public Language lang;
  public Range range;
  public String identifier;

  public CodeSource(Language lang, Range range, String identifier){
    this.lang = lang;
    this.range = range;
    this.identifier = identifier;
  }
}
