package org.example.expr;


import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.example.expr.parser.ExprBaseVisitor;
import org.example.expr.parser.ExprParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.javacpp.LLVM.*;


public class ExprVistor extends ExprBaseVisitor<LLVMValueRef> {

  private final LLVMBasicBlockRef entry;

  private Map<String, LLVMValueRef> id=new HashMap<>();

  public ExprVistor() {
    LLVMModuleRef mod = LLVMModuleCreateWithName("fac_module");
    LLVMTypeRef[] args={LLVMInt32Type()};
    LLVMTypeRef type = LLVMFunctionType(LLVMInt32Type(), args[0], 1, 0);
    LLVMValueRef global = LLVMAddFunction(mod, "global", type);
    entry = LLVMAppendBasicBlock(global, "entry");


  }

  @Override
  public LLVMValueRef visitProg(ExprParser.ProgContext ctx) {
    List<ExprParser.StatContext> stats = ctx.stat();
    for (ExprParser.StatContext stat : stats) {
      visit(stat);
    }
    return null;
  }

  @Override
  public LLVMValueRef visitPrintExpr(ExprParser.PrintExprContext ctx) {
    visit(ctx.expr());
    System.out.println("visitPrintExpr");
    return null;
  }

  @Override
  public LLVMValueRef visitAssign(ExprParser.AssignContext ctx) {
    LLVMValueRef id = visit(ctx.ID());
    LLVMValueRef value = visit(ctx.expr());

    System.out.println("visitAssign");
    return null;
  }

  @Override
  public LLVMValueRef visitBlank(ExprParser.BlankContext ctx) {
    return null;
  }

  @Override
  public LLVMValueRef visitParens(ExprParser.ParensContext ctx) {
    return super.visitParens(ctx);
  }

  @Override
  public LLVMValueRef visitMulDiv(ExprParser.MulDivContext ctx) {
    return super.visitMulDiv(ctx);
  }

  @Override
  public LLVMValueRef visitAddSub(ExprParser.AddSubContext ctx) {
    return super.visitAddSub(ctx);
  }

  @Override
  public LLVMValueRef visitId(ExprParser.IdContext ctx) {
    TerminalNode id = ctx.ID();
    Token symbol = id.getSymbol();
    return super.visitId(ctx);
  }

  @Override
  public LLVMValueRef visitInt(ExprParser.IntContext ctx) {
    return super.visitInt(ctx);
  }
}

