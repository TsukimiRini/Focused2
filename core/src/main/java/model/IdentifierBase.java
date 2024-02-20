package model;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentifierBase {
  protected static final Logger logger = LoggerFactory.getLogger(IdentifierBase.class);
  public String text;

  static {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
  }

  public IdentifierBase(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return text;
  }
}
