package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.MatcherUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SegmentPattern extends SegmentBase<IdentifierPattern> {
  public Set<String> captures = new HashSet<>();

  public SegmentPattern(SegmentType segType, String text, String type) {
    super(segType);
    this.text = new IdentifierPattern(text);
    this.type = new IdentifierPattern(type);
    captures.addAll(this.text.captures);
    captures.addAll(this.type.captures);
  }

  public SegmentPattern(SegmentType segType, String source) {
    super(segType);
    if (segType == SegmentType.FILE) {
      this.text = new IdentifierPattern(source);
      captures.addAll(this.text.captures);
    } else if (segType == SegmentType.NODE | segType == SegmentType.EDGE) {
      Map<String, String> parts = MatcherUtils.splitSegment(source);
      if (parts == null) {
        logger.error("Invalid Segment {}", source);
        throw new IllegalArgumentException("invalid config");
      } else {
        for (String label : parts.keySet()) {
          switch (label) {
            case "content":
              this.text = new IdentifierPattern(parts.get(label));
              captures.addAll(text.captures);
              break;
            case "type":
              this.type = new IdentifierPattern(parts.get(label));
              captures.addAll(type.captures);
              break;
            case "anchor":
              this.anchor = parts.get(label);
              break;
            case "attrs":
              addAttributes(parts.get(label));
              break;
          }
        }
      }
    }
  }

  public void addAttribute(String attrName, String val) {
    IdentifierPattern attrValIdentifier = new IdentifierPattern(val);
    attributes.put(attrName, attrValIdentifier);
    captures.addAll(attrValIdentifier.captures);
  }

  public void addAttributes(String attrs) {
    Map<String, String> kvPairs = MatcherUtils.matchAttributes(attrs);
    for (String key : kvPairs.keySet()) {
      addAttribute(key, kvPairs.get(key));
    }
  }
}
