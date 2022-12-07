package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SegmentBase<T extends IdentifierBase> {
  protected static final Logger logger = LoggerFactory.getLogger(SegmentBase.class);
  public SegmentType segType;
  public T text;
  public T type;
  public String anchor;
  public Map<String, T> attributes = new HashMap<>();
  public SegmentBase parent;

  public SegmentBase(SegmentType segType) {
    this.segType = segType;
  }

  public void setParent(SegmentBase p) {
    this.parent = p;
  }
}
