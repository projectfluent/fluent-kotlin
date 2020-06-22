package org.projectfluent.testing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.projectfluent.syntax.ast.Resource;
import org.projectfluent.syntax.ast.TextElement;
import org.projectfluent.syntax.parser.FluentParser;
import org.projectfluent.syntax.visitor.Visitor;

import java.util.regex.Pattern;

class JavaVisitorTest {
    private final FluentParser parser = new FluentParser(false);

    @Test
    void test_wordcounter() {
        Resource res = parser.parse("msg = value with words");
        JavaWordCounter counter = new JavaWordCounter();
        counter.visit(res);
        assertEquals(3, counter.getWords());
    }
}
