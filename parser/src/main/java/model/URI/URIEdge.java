package model.URI;

public class URIEdge extends URISegment {
  public URINode from;

  public URIEdge(String type, URINode parent){
    this.type = type;
    this.from = parent;
  }
}
