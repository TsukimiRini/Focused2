package model;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class URIBase<T extends SegmentBase> {
  protected static final Logger logger = LoggerFactory.getLogger(URIBase.class);
  public String label;
  public String lang;
  public T file;
  public T elementRoot;
  public Map<String, List<T>> branches = new HashMap<>();
  public List<T> defaultBranches = new ArrayList<>();

  static {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
  }

  public URIBase(String label) {
    this.label = label;
  }
}
