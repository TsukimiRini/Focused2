package model;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class SouffleRecord {
  public String typeName;
  List<Pair<String, String>> fields = new ArrayList<>();

  public SouffleRecord(String typeName) {
    this.typeName = typeName;
  }

  public void parseFields(String source) {
    String[] fieldsSplit = source.split(",");
    for (String fieldPair : fieldsSplit) {
      String[] nameAndType = fieldPair.split(":");
      fields.add(Pair.of(nameAndType[0].strip(), nameAndType[1].strip()));
    }
  }

  public String toString() {
    StringBuilder souffleCode = new StringBuilder(String.format(".type %s = [\n", typeName));
    String formatBlank = new String(new char[4]).replace('\0', ' ');
    for (Pair<String, String> field : fields) {
      souffleCode
          .append(formatBlank)
          .append(field.getKey())
          .append(": ")
          .append(field.getValue())
          .append(",\n");
    }
    souffleCode.append("]\n");
    return souffleCode.toString();
  }
}
