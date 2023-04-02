package model.URI;

import java.util.HashMap;

public class URISegment extends HashMap<String, String> {
  public String type;

  public String getAttrStr(){
    StringBuilder attributeStr = new StringBuilder("[");
    for (String key : this.keySet()) {
      if (key.equals("name")) continue;
      attributeStr
              .append(attributeStr.length() == 1 ? "" : ",")
              .append(key)
              .append("=")
              .append(get(key));
    }
    attributeStr.append("]");
    return attributeStr.toString();
  }
}
