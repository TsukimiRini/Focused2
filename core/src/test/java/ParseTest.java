import model.SegmentPattern;
import model.SegmentType;
import model.URIPattern;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParseTest {
  @Test
  public void testSegmentParsing(){
    String seg = "test(cap1)::key1#anchor[val1=(cap2),val2=123,val3=\\:def\\:]";
    SegmentPattern p = new SegmentPattern(SegmentType.NODE, seg);
    assertEquals(p.text.text, "test(cap1)");
    assertEquals(p.type.text, "key1");
    assertEquals(p.anchor, "anchor");
    assertEquals(p.attributes.size(), 3);
    assertEquals(p.captures.size(), 2);
    System.out.println(p.attributes);
    System.out.println(p.captures);
    System.out.println(p);
  }

  @Test
  public void testURIParsing(){
    String lang = "JAVA";
    String file = "root//(layoutFile).xml";
    String element = "(elementTag)::XMLTag#func[idx=1]/~attr~/android\\:id//\"@id\\/(widgetID)\"";
    String branch1 = "@func//**//(bindingVar)::VarDecl/~varType~/(layoutName)Binding";
    String branch2 = "(function)::FuncDecl//**//(bindingVar).(widgetID)";
    URIPattern uriPattern = new URIPattern("test", lang, file, element, null, Arrays.asList(branch1, branch2));
    System.out.println(uriPattern.captures);
  }
}
