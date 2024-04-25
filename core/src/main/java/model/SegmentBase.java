package model;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SegmentBase<T extends IdentifierBase> {
  protected static final Logger logger = LoggerFactory.getLogger(SegmentBase.class);
  public SegmentType segType;
  public T text = null;
  public T type = null;
  public String anchor = null;
  public Map<String, T> attributes = new HashMap<>();
  public SegmentBase<T> parent = null;
  public SegmentBase<T> child = null;

  static {
    BasicConfigurator.configure();
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
  }

  public SegmentBase(SegmentType segType) {
    this.segType = segType;
  }

  public void setParent(SegmentBase<T> p) {
    this.parent = p;
    if (p != null) p.child = this;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(text.toString());
    if (type != null) sb.append("::").append(type);
    if (anchor != null && anchor.length() > 0) sb.append("#").append(anchor);
    if (!attributes.isEmpty()) {
      sb.append("[");
      boolean flag = true;
      for (String name : attributes.keySet()) {
        if (flag) {
          flag = false;
        } else {
          sb.append(",");
        }
        sb.append(name).append("=").append(attributes.get(name));
      }
      sb.append("]");
    }
    String childStr = (child == null) ? "" : "/" + child;
    if (segType == SegmentType.EDGE && sb.length() > 0) return "~" + sb + "~" + childStr;
    else return sb.append(childStr).toString();
  }

  public SegmentBase<T> getTail() {
    SegmentBase<T> cur = this;
    while (cur.child != null) {
      cur = cur.child;
    }
    return cur;
  }

  public Boolean isLeafType() {
    if (type == null) return false;
    if (type.toString().equals("identifier") || type.toString().endsWith("literal")) return true;
    return false;
  }
}
