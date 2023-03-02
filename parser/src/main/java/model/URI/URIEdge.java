package model.URI;

public class URIEdge extends URISegment {
  public URINode from;

  public URIEdge(String type, URINode parent) {
    this.type = type;
    this.from = parent;
  }

  public String toString() {
    return containsKey("type")
        ? get("type") + ":"
        : ""
            + entrySet().stream()
                .filter(x -> !x.getKey().equals("type"))
                .map(x -> x.getKey() + "=" + x.getValue())
                .reduce((acc, x) -> acc + ", " + x);
  }
}
