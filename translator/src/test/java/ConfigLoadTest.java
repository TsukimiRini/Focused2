import model.URIPattern;
import model.config.ConfigLink;
import model.config.ConfigLinkBlock;
import model.config.ConfigPredicate;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigLoadTest {
  @Test
  public void testLoad() throws IOException {
    String fileName = System.getProperty("user.dir") + "/src/test/resources/config.fcs";
    Pair<List<URIPattern>, List<ConfigLinkBlock>> pair = ConfigLoader.load(fileName);
    List<URIPattern> patterns = pair.getLeft();
    List<ConfigLinkBlock> blocks = pair.getRight();
    System.out.println(patterns);
  }

  @Test
  public void testConfigLink() {
    ConfigLink link = new ConfigLink("WidgetXLL(def, ref)");
    assertEquals(link.decl.predicateName, "WidgetXLL");
    System.out.println(link.decl.params);
  }

  @Test
  public void testConfigPredicate() {
    ConfigPredicate link = new ConfigPredicate("EndsWith(def.file, cat(include.includeLayout, \".xml\"))");
    assertEquals(link.predicateName, "EndsWith");
    System.out.println(link.params);
  }
}
