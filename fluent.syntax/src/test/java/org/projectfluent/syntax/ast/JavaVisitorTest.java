package org.projectfluent.syntax.ast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.projectfluent.syntax.parser.FluentParser;

import java.util.regex.Pattern;

class JavaWordCounter extends Visitor {
    private static final java.util.regex.Pattern WORD_BOUNDARY = Pattern.compile("\\W+");

    private int words;
    public JavaWordCounter() {
        words = 0;
    }

    public int getWords() {
        return words;
    }

    void visit_Resource(Resource node) {
        System.out.println("resource");
        this.generic_visit(node);
    }

    public void visit_TextElement(TextElement node) {
        String val = node.getValue();
        words += WORD_BOUNDARY.split(val).length;
    }
}

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
