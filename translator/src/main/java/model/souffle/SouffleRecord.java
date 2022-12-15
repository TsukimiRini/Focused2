package model.souffle;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class SouffleRecord extends SouffleStmt {
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
    for (int i = 0; i < fields.size(); i++) {
      Pair<String, String> field = fields.get(i);
      if (i != 0) souffleCode.append(",\n");
      souffleCode.append(formatBlank).append(field.getKey()).append(": ").append(field.getValue());
    }
    souffleCode.append("\n]\n");
    return souffleCode.toString();
  }
}
