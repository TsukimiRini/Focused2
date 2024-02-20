package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentifierBase {
  protected static final Logger logger = LoggerFactory.getLogger(IdentifierBase.class);
  public String text;

  public IdentifierBase(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return text;
  }
}
