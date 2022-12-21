import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static utils.MatcherUtils.*;

public class MatcherTest {
  @Test
  public void testAttrsMatch(){
    String attrs = "abc=1,bcd=2,asd=a\\,b";
    Map<String, String> res = matchAttributes(attrs);
    System.out.println(res);
  }

  @Test
  public void testSegment(){
    String seg = "This is a test::key1#func[val1=\\[abc\\],val2=123,val3=\\:def\\:]";
    Map<String, String> res = splitSegment(seg);
    System.out.println(res);
  }

  @Test
  public void testURIAttr(){
    String uriAttr = "def[widgetID]";
    Pair<String, Pair<Boolean, String>> res = parseURIRefAttrInConfig(uriAttr);
    assertEquals(res.getLeft(), "def");
    uriAttr = "ref.file";
    res = parseURIRefAttrInConfig(uriAttr);
    assertEquals(res.getLeft(), "ref");
  }
}
