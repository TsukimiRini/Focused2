package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class URIBase<T extends SegmentBase> {
  protected static final Logger logger = LoggerFactory.getLogger(URIBase.class);
  public String label;
  public String lang;
  public T file;
  public T elementRoot;
  public List<T> branches = new ArrayList<>();

  public URIBase(String label) {
    this.label = label;
  }
}
