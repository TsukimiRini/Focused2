import model.Language;
import model.URI.URINode;
import model.URIPattern;
import utils.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class ElementInstance implements Cloneable {
  public CaptureHolder capVal = new CaptureHolder();
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

  public boolean hasConflictWith(ElementInstance other){
    Set<String> sharedKeys = new HashSet<>(capVal.keySet());
    sharedKeys.retainAll(other.capVal.keySet());
    for (String key : sharedKeys) {
      if (!capVal.get(key).equals(other.capVal.get(key))) return true;
    }
    return false;
  }

  public void setElement(URINode ele) {
    element = ele;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ElementInstance)) return false;
    ElementInstance ele = (ElementInstance) obj;
    return filePath.equals(ele.filePath)
        && (element == ele.element || element.toString().equals(ele.element.toString()))
        && capVal.toString().equals(ele.capVal.toString());
  }

  public int hashCode() {
    CaptureHolder tmp = new CaptureHolder(capVal);
    return (filePath
            + (element == null ? "" : element.toString())
            + (capVal == null ? 0 : capVal.toString()))
        .hashCode();
  }

  @Override
  public ElementInstance clone() throws CloneNotSupportedException {
    ElementInstance cloned = (ElementInstance) super.clone();
    cloned.capVal = new CaptureHolder(capVal);
    cloned.branches = new ArrayList<>(branches);
    return cloned;
  }

  public String toOutputString(URIPattern pattern) {
    StringBuilder builder = new StringBuilder("[");
    builder.append(lang.toString()).append(", ");
    builder.append(filePath == null ? "" : StringUtil.quoteStr(filePath)).append(", ");
    builder.append(element == null ? "" : StringUtil.quoteStr(element.toString())).append(", ");

    if (branches.isEmpty()) builder.append("$ZeroBranch, ");
    else {
      List<String> branchStrs =
          branches.stream().map(URINode::toString).collect(Collectors.toList());
      builder
          .append("$")
          .append(pattern.label)
          .append("Br(")
          .append(branchStrs.stream().map(StringUtil::quoteStr).collect(Collectors.joining(", ")))
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
          .append(capValStrs.stream().map(StringUtil::quoteStr).collect(Collectors.joining(", ")))
          .append(")");
    }

    builder.append("]");
    return builder.toString();
  }
}
