package model;

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

  public SegmentBase(SegmentType segType) {
    this.segType = segType;
  }

  public void setParent(SegmentBase<T> p) {
    this.parent = p;
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
          sb.append(",");
          flag = false;
        }
        sb.append(name).append(":").append(attributes.get(name));
      }
      sb.append("]");
    }
    String parentStr = (parent == null) ? "" : parent + "/";
    if (segType == SegmentType.EDGE && sb.length() > 0) return parentStr + "~" + sb + "~";
    else return parentStr + sb;
  }

  public SegmentBase<T> getHead() {
    SegmentBase<T> cur = this;
    while (cur.parent != null) {
      cur = cur.parent;
    }
    return cur;
  }
}
