package org.example.expr;


import org.antlr.v4.runtime.ParserRuleContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.example.expr.parser.ExprBaseVisitor;
import org.example.expr.parser.ExprParser;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bytedeco.javacpp.LLVM.*;


public class ExprVistor extends ExprBaseVisitor<LLVMValueRef> {

  private final LLVMBasicBlockRef entry;

  private Map<String, LLVMValueRef> allocedID =new HashMap<>();
  private final LLVMModuleRef mod;
  private final LLVMValueRef global_func;
  private final LLVMBuilderRef builder;
  private int count=0;

  public ExprVistor() {
    LLVMLinkInMCJIT();
    LLVMInitializeNativeAsmPrinter();
    LLVMInitializeNativeAsmParser();
    LLVMInitializeNativeDisassembler();
    LLVMInitializeNativeTarget();

    mod = LLVMModuleCreateWithName("fac_module");
    LLVMTypeRef[] args={LLVMInt32Type()};
    LLVMTypeRef type = LLVMFunctionType(LLVMInt32Type(), args[0], 1, 0);
    global_func = LLVMAddFunction(mod, "global_func", type);
    entry = LLVMAppendBasicBlock(global_func, "entry");
    builder = LLVMCreateBuilder();
    LLVMPositionBuilderAtEnd(builder, entry);
  }

  @Override
  public LLVMValueRef visitProg(ExprParser.ProgContext ctx) {
    debug("visitProg");
    List<ExprParser.StatContext> stats = ctx.stat();
    for (ExprParser.StatContext stat : stats) {
      visit(stat);
    }
    LLVMBuildRet(builder, LLVMConstInt(LLVMInt32Type(),0,1));
    return null;
  }

  @Override
  public LLVMValueRef visitPrintExpr(ExprParser.PrintExprContext ctx) {
    debug("visitPrintExpr");
    visit(ctx.expr());
    return null;
  }

  @Override
  public LLVMValueRef visitAssign(ExprParser.AssignContext ctx) {
    debug("visitAssign");
    String str = ctx.ID().getSymbol().getText();
    LLVMValueRef addr = allocedID.get(str);
    if(addr==null){
        addr = LLVMBuildAlloca(builder,LLVMInt32Type(), "str");
        allocedID.put(str, addr);
    }
    LLVMValueRef value = visit(ctx.expr()); // value
    LLVMBuildStore(builder, value, addr);
    return null;
  }

  @Override
  public LLVMValueRef visitBlank(ExprParser.BlankContext ctx) {
    debug("visitBlank");
    return null;
  }

  @Override
  public LLVMValueRef visitParens(ExprParser.ParensContext ctx) {
    debug("visitParens");
    return visit(ctx.expr());
  }

  @Override
  public LLVMValueRef visitMulDiv(ExprParser.MulDivContext ctx) {
    debug("visitMulDiv");
    LLVMValueRef leftvalue = visit(ctx.expr(0));
    String op = ctx.op.getText();
    LLVMValueRef rightvalue = visit(ctx.expr(1));
    LLVMValueRef ret = null;
    switch(op){
      case "*":
        ret = LLVMBuildMul(builder, leftvalue, rightvalue, "mul"+count++);
        break;
      case "/":
        ret = LLVMBuildSDiv(builder, leftvalue, rightvalue, "div"+count++);
        break;
      default:
        throw new Error("unknown op");
    }
    return ret;
  }

  @Override
  public LLVMValueRef visitAddSub(ExprParser.AddSubContext ctx) {
    debug("visitAddSub");
    LLVMValueRef leftvalue = visit(ctx.expr(0));
    String op = ctx.op.getText();
    LLVMValueRef rightvalue = visit(ctx.expr(1));
    LLVMValueRef ret = null;
    switch(op){
      case "+":
        ret = LLVMBuildAdd(builder, leftvalue, rightvalue, "add"+count++);
        break;
      case "-":
        ret = LLVMBuildSub(builder, leftvalue, rightvalue, "sub"+count++);
        break;
      default:
        throw new Error("unknown op");
    }
    return ret;
  }

  @Override
  public LLVMValueRef visitId(ExprParser.IdContext ctx) {
    debug("visitId");
    ParserRuleContext parent = ctx.getParent();
    String str = ctx.ID().getSymbol().getText();
    LLVMValueRef value = allocedID.get(str);
    if (parent instanceof ExprParser.ExprContext){
      if(value==null){
        throw new Error("value is unalloced");
      }
      value = LLVMBuildLoad(builder,value,"load"+count++);
    } else {
      throw new Error("unknown type");
    }
    return value;
  }

  @Override
  public LLVMValueRef visitInt(ExprParser.IntContext ctx) {
    debug("visitInt");
    String str = ctx.INT().getSymbol().getText();
    long value = Integer.valueOf(str);
    return LLVMConstInt(LLVMInt32Type(), value, 1);
  }

  public void debug(String str){
    System.out.println(str);
  }

  public void verify() {
    BytePointer error = new BytePointer((Pointer)null); // Used to retrieve messages from functions
    LLVMVerifyModule(mod, LLVMAbortProcessAction, error);
    LLVMDisposeMessage(error);
    LLVMDumpModule(mod);
  }

  public void optimize(){
    LLVMPassManagerRef pass = LLVMCreatePassManager();
    LLVMAddConstantPropagationPass(pass);
    LLVMAddInstructionCombiningPass(pass);
    LLVMAddPromoteMemoryToRegisterPass(pass);
    // LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory
    LLVMAddGVNPass(pass);
    LLVMAddCFGSimplificationPass(pass);
    LLVMRunPassManager(pass, mod);
    LLVMDumpModule(mod);
    LLVMDisposePassManager(pass);
  }

  public void exec(){
    BytePointer error = new BytePointer((Pointer)null); // Used to retrieve messages from functions
    LLVMExecutionEngineRef engine = new LLVMExecutionEngineRef();
    if(LLVMCreateJITCompilerForModule(engine, mod, 2, error) != 0) {
      String err = error.getString();
      LLVMDisposeMessage(error);
      throw new Error(err);
    }
//    if(LLVMCreateExecutionEngineForModule(engine, mod, error) != 0) {
//      String err = error.getString();
//      LLVMDisposeMessage(error);
//      throw new Error(err);
//    }

    LLVMGenericValueRef exec_args = LLVMCreateGenericValueOfInt(LLVMInt32Type(), 10, 1);
    LLVMGenericValueRef exec_res = LLVMRunFunction(engine, global_func, 1, exec_args);
    System.out.println("Result: " + LLVMGenericValueToInt(exec_res, 1));
    LLVMDisposeExecutionEngine(engine);
  }
}

