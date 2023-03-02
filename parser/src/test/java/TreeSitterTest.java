import ai.serenade.treesitter.Languages;
import ai.serenade.treesitter.Parser;
import ai.serenade.treesitter.Tree;
import ai.serenade.treesitter.TreeCursor;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TreeSitterTest {
  static {
    System.load(
            System.getProperty("user.home")
                    + "/Projects/tree-sitter/java-tree-sitter/libjava-tree-sitter.dylib");
  }

  @Test
  void testWalk() throws UnsupportedEncodingException {
    try (Parser parser = new Parser()) {
      parser.setLanguage(Languages.python());
      try (Tree tree = parser.parseString("def foo(bar, baz):\n  print(bar)\n  print(baz)")) {
        try (TreeCursor cursor = tree.getRootNode().walk()) {
          assertEquals("module", cursor.getCurrentTreeCursorNode().getType());
          assertEquals("module", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoFirstChild());
          assertEquals("function_definition", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoFirstChild());

          assertEquals("def", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoNextSibling());
          assertEquals("identifier", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoNextSibling());
          assertEquals("parameters", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoNextSibling());
          assertEquals(":", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoNextSibling());
          assertEquals("block", cursor.getCurrentNode().getType());
          assertEquals("body", cursor.getCurrentFieldName());
          assertEquals(false, cursor.gotoNextSibling());

          assertEquals(true, cursor.gotoParent());
          assertEquals("function_definition", cursor.getCurrentNode().getType());
          assertEquals(true, cursor.gotoFirstChild());
        }
      }
    }
  }
}
