import model.Language;
import model.URI.URINode;
import model.URIPattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ElementInstance implements Cloneable {
  public Map<String, String> capVal = new HashMap<>();
  public URINode element = null;
  public URINode file = null;
  public List<URINode> branches = new ArrayList<>();
  public Language lang;
  public String filePath;

  public ElementInstance(Language lang) {
    this.lang = lang;
  }

  public boolean addCapture(String key, String value) {
    if (capVal.containsKey(key)) return capVal.get(key).equals(value);
    capVal.put(key, value);
    return true;
  }

  public boolean hasCapture(String key) {
    return capVal.containsKey(key);
  }

  public void addBranch(URINode branch) {
    branches.add(branch);
  }

  public void setFilePath(URINode file) {
    this.file = file;
    filePath = file.toString();
  }

  public void setElement(URINode ele) {
    element = ele;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof ElementInstance)) return false;
    ElementInstance ele = (ElementInstance) obj;
    return element == ele.element && capVal.equals(ele.capVal);
  }

  @Override
  public ElementInstance clone() throws CloneNotSupportedException {
    ElementInstance cloned = (ElementInstance) super.clone();
    cloned.capVal = new HashMap<>(capVal);
    cloned.branches = new ArrayList<>(branches);
    return cloned;
  }

  public String toOutputString(URIPattern pattern) {
    StringBuilder builder = new StringBuilder("[");
    builder.append(lang.toString()).append(", ");
    builder.append(filePath).append(", ");
    builder.append(element.toString()).append(", ");

    if (branches.isEmpty()) builder.append("$None, ");
    else {
      List<String> branchStrs =
          branches.stream().map(URINode::toString).collect(Collectors.toList());
      builder
          .append("$")
          .append(pattern.label)
          .append("Br(")
          .append(String.join(", ", branchStrs))
          .append("), ");
    }

    if (capVal.isEmpty()) builder.append("$ZeroCap");
    else {
      List<String> capValStrs =
          pattern.captures.stream().map(cap -> capVal.get(cap)).collect(Collectors.toList());
      builder
          .append("$")
          .append(pattern.label)
          .append("Cap(")
          .append(String.join(", ", capValStrs))
          .append(")");
    }

    builder.append("]");
    return builder.toString();
  }
}
