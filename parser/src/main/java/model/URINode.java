package model;

import java.util.List;
import java.util.Map;

public class URINode {
  public String layerName;
  public URIEdge egdeToParent;
  public Map<String, List<URINode>> children;
}
