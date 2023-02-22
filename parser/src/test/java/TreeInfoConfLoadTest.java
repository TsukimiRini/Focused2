import model.TreeInfoConf;
import org.junit.jupiter.api.Test;

public class TreeInfoConfLoadTest {
  @Test
  public void testConfLoad(){
    TreeInfoConf conf = new TreeInfoConf(System.getProperty("user.dir")+"/src/test/resources/androidTreeConf.tree");
    conf.load();
    System.out.println("aaa");
  }
}
