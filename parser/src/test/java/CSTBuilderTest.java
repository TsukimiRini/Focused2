import model.CSTTree;
import model.Language;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class CSTBuilderTest {
  @Test
  public void testCSTBuilder() throws UnsupportedEncodingException {
    SharedStatus.initProjectInfo(
        "android", System.getProperty("user.home") + "/coding/xll/android/CloudReader");
    Map<Language, Map<String, CSTTree>> cstTrees = CSTBuilder.buildCST();
    System.out.println(cstTrees);
  }
}
