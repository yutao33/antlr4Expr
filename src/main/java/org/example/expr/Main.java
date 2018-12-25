package org.example.expr;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.example.expr.parser.ExprLexer;
import org.example.expr.parser.ExprParser;

public class Main {

  public static void main(String[] args){
    CharStream input = CharStreams.fromString("a=(1+2)*3\nb=a+3\n");
    ExprLexer lexer = new ExprLexer(input);
    ExprParser parser = new ExprParser(new CommonTokenStream(lexer));
    ParseTree tree = parser.prog();
    new ExprVistor().visit(tree);
  }

}
