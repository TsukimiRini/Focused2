import java.util.HashMap;

public class CaptureHolder extends HashMap<String, String> {
  public CaptureHolder() {
    super();
  }

  public CaptureHolder(CaptureHolder other) {
    super(other);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof CaptureHolder) {
      CaptureHolder other = (CaptureHolder) o;
      for (String key : this.keySet()) {
        if (!other.containsKey(key) || other.get(key).equals(get(key))) return false;
      }
    } else {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return entrySet().stream()
        .map((pair -> pair.getKey() + pair.getValue()))
        .reduce("", (value, s) -> value + s)
        .hashCode();
  }
}
