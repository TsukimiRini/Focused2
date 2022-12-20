import model.souffle.ADT;
import model.souffle.PredicateDecl;
import model.souffle.Record;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TranslateTest {
  @Test
  public void testADT() {
    ADT adt = new ADT("Capture", "Default");
    List<Pair<String, String>> field1 = parseFields("layoutFile: symbol");
    adt.addBranch("MenuFileStandardCap", field1);
    List<Pair<String, String>> field2 = parseFields("javaFile: symbol, layoutFile: symbol");
    adt.addBranch("LayoutIDCap", field2);
    System.out.println(adt);
  }

  @Test
  public void testPredicateDecl() {
    PredicateDecl decl = new PredicateDecl("MenuFileStandard", "uri", "URI");
    decl.addParam("file", "Symbol");
    System.out.println(decl);
  }

  @Test
  public void testRecord() {
    Record decl = new Record("MenuFileStandard");
    decl.parseFields(
        "lang: symbol,\n"
            + "    file: symbol,\n"
            + "    element: symbol,\n"
            + "    branches: Branch,\n"
            + "    caps: Capture");
    System.out.println(decl);
  }

  private List<Pair<String, String>> parseFields(String source) {
    List<Pair<String, String>> res = new ArrayList<>();
    String[] fieldsSplit = source.split(",");
    for (String fieldPair : fieldsSplit) {
      String[] nameAndType = fieldPair.split(":");
      res.add(Pair.of(nameAndType[0].strip(), nameAndType[1].strip()));
    }
    return res;
  }
}
