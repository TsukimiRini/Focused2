import model.URIPattern;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class ConfigLoadTest {
  @Test
  public void testLoad() throws IOException {
    String fileName = System.getProperty("user.dir") + "/src/test/resources/config.fcs";
    List<URIPattern> patterns = ConfigLoader.load(fileName);
    System.out.println("aaa");
  }
}
