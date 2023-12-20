import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.FileWriter;
import java.io.PrintWriter;


// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) throws Exception {
        tinyPythonLexer lexer = new tinyPythonLexer(CharStreams.fromFileName("test.tpy"));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tinyPythonParser parser = new tinyPythonParser(tokens);
        ParseTree tree = parser.program();

        ParseTreeWalker walker = new ParseTreeWalker();
        tinyPythonJListener toJavaBytecode = new tinyPythonJListener();
        walker.walk(toJavaBytecode, tree);

        try (PrintWriter writer = new PrintWriter(new FileWriter("Test.j"))) {
            writer.print(toJavaBytecode.formattedText);
        }
    }
}