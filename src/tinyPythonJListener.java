import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.HashMap;
import java.util.Stack;

public class tinyPythonJListener extends tinyPythonBaseListener{

    ParseTreeProperty<String> newTexts = new ParseTreeProperty<>();
    public StringBuilder formattedText = new StringBuilder();
    Stack<String> varStack = new Stack<>();
    HashMap<String, String> fun_label = new HashMap<>();
    int label = 0;

    @Override
    public void exitFile_input(tinyPythonParser.File_inputContext ctx) {
        String body = newTexts.get(ctx.defs());
        body += "\n.method public static main([Ljava/lang/String;)V\n" +
                ".limit stack 32\n" +
                ".limit locals 32\n";
        int stmt_cnt = 0;
        for (int i = 1; i < ctx.getChildCount(); i++){
            if (ctx.getChild(i) == ctx.EOF()){
                break;
            }
            if (ctx.getChild(i).getText().equals("\n")) {
                // NEWLINE
//                body += "\n";
            }else{
                //stmt
                body += newTexts.get(ctx.stmt(stmt_cnt));
                stmt_cnt += 1;
            }
        }
        newTexts.put(ctx, body+"return\n.end method");
    }

    @Override
    public void exitDefs(tinyPythonParser.DefsContext ctx) {
        String def_stmt = "";
        int stmt_count = 0;
        for (int i = 0; i < ctx.getChildCount(); i++){
            if (!ctx.getChild(i).getText().equals("\n")){
                def_stmt += newTexts.get(ctx.def_stmt(stmt_count));
                stmt_count += 1;
            }
        }
        newTexts.put(ctx, def_stmt +
                ".end method\n");
        varStack.clear();

    }

    @Override
    public void exitStmt(tinyPythonParser.StmtContext ctx) {
        if (ctx.simple_stmt() != null){
            //simple_stmt
            newTexts.put(ctx, newTexts.get(ctx.simple_stmt()));

        }else{
            //compound_stmt
            newTexts.put(ctx, newTexts.get(ctx.compound_stmt()));
        }
    }

    @Override
    public void exitSimple_stmt(tinyPythonParser.Simple_stmtContext ctx) {
        newTexts.put(ctx, newTexts.get(ctx.small_stmt()));
    }

    @Override
    public void exitSmall_stmt(tinyPythonParser.Small_stmtContext ctx) {
        if (ctx.assignment_stmt() != null){
            //assignmnet_stmt
            newTexts.put(ctx, newTexts.get(ctx.assignment_stmt()));
        }else if (ctx.flow_stmt() != null) {
            //flow_stmt
            newTexts.put(ctx, newTexts.get(ctx.flow_stmt()));
        }else if (ctx.print_stmt() != null){
            //print_stmt
            newTexts.put(ctx, newTexts.get(ctx.print_stmt()));
        }else {
            // return_stmt
            newTexts.put(ctx, newTexts.get(ctx.return_stmt()));
        }
    }

    @Override
    public void exitAssignment_stmt(tinyPythonParser.Assignment_stmtContext ctx) {
        if (!varStack.contains(ctx.NAME().getText())){
            newTexts.put(ctx, newTexts.get(ctx.expr())+"istore "+(varStack.size())+"\n");
            varStack.push(ctx.NAME().getText());
        }else{
            newTexts.put(ctx, newTexts.get(ctx.expr())+"istore " + varStack.indexOf(ctx.NAME().getText())+ "\n");
        }
    }

    @Override
    public void exitFlow_stmt(tinyPythonParser.Flow_stmtContext ctx) {
        if (ctx.break_stmt() != null){
            //break_stmt
            newTexts.put(ctx, newTexts.get(ctx.break_stmt()));
        }else{
            //continue_stmt
            newTexts.put(ctx, newTexts.get(ctx.continue_stmt()));
        }
    }

    @Override
    public void exitBreak_stmt(tinyPythonParser.Break_stmtContext ctx) {
        // the line number of the end of the loop (for ‘break’)
        newTexts.put(ctx, "goto label"+(label)+"\n");
    }

    @Override
    public void exitContinue_stmt(tinyPythonParser.Continue_stmtContext ctx) {
        // the line number of the loop head (for ‘continue’)
        newTexts.put(ctx, "goto label"+(label)+"\n");
    }

    @Override
    public void exitCompound_stmt(tinyPythonParser.Compound_stmtContext ctx) {
        if (ctx.if_stmt() != null){
            //if_stmt
            newTexts.put(ctx, newTexts.get(ctx.if_stmt()));
        }else{
            // while_stmt
            newTexts.put(ctx, newTexts.get(ctx.while_stmt()));
        }
    }

    @Override
    public void exitIf_stmt(tinyPythonParser.If_stmtContext ctx) {
        int suite_cnt = 1;
        int first_label = label;
        String test = newTexts.get(ctx.test(0))+ "label" +label+ "\n";
        String suite = newTexts.get(ctx.suite(0));
        String elif_body = "";
        for (int test_cnt = 1; test_cnt < ctx.test().size(); test_cnt++, suite_cnt++){
            //elif
            elif_body += "goto label" + (first_label+ ctx.suite().size()-1) + "\n" +
                    "label" + label + ": \n" +
                    newTexts.get(ctx.test(test_cnt))+ "label" +(label+1)+ "\n" +
                    newTexts.get(ctx.suite(suite_cnt));
            label += 1;
        }
        if (suite_cnt < ctx.suite().size()){
            //else
            elif_body += "goto label" + (first_label+ ctx.suite().size()-1) + "\n"+ "label" +label+ ": \n" +
                    newTexts.get(ctx.suite(ctx.suite().size()-1))+
                    "label"+(label+1)+":\n";
        } else{
            elif_body += "label" +label+ ": \n";
        }
        label += 1;
//

        newTexts.put(ctx,  test + suite + elif_body);
    }

    @Override
    public void exitWhile_stmt(tinyPythonParser.While_stmtContext ctx) {

        String test = "label_while:\n"+newTexts.get(ctx.test()) + "label_end\n";
        String suite = newTexts.get(ctx.suite()) +
                "goto label_while\n"+
                "label_end:\n";
        label += 1;
        newTexts.put(ctx, test + suite);

    }

    @Override
    public void exitDef_stmt(tinyPythonParser.Def_stmtContext ctx) {
        String name = ctx.NAME().getText();
        String args = newTexts.get(ctx.args());
        String suite = newTexts.get(ctx.suite());
        String fun_name = name + "(" + args + ")" + "I";
        fun_label.put(name, "Test/"+fun_name);
        newTexts.put(ctx,
                ".method public static " +
                        fun_name +
                        "\n" +
                        ".limit stack 32\n" +
                        ".limit locals 32\n" +
                        suite
        );
    }

    @Override
    public void exitSuite(tinyPythonParser.SuiteContext ctx) {
        if (ctx.getChildCount() == 1){
            //simple_stmt
            newTexts.put(ctx, newTexts.get(ctx.simple_stmt()));
        }else{
            //NEWLINE stmt+
            String stmt = "";
            for (int i = 0; i<ctx.getChildCount()-1; i++){
                stmt += newTexts.get(ctx.stmt(i));
            }
            newTexts.put(ctx, stmt);
        }
    }

    @Override
    public void exitArgs(tinyPythonParser.ArgsContext ctx) {
        for (int i = 0; i<ctx.NAME().size(); i++){
            varStack.push(ctx.NAME(i).getText());
        }
        newTexts.put(ctx, "I".repeat(ctx.NAME().size()));
    }

    @Override
    public void exitReturn_stmt(tinyPythonParser.Return_stmtContext ctx) {
        newTexts.put(ctx, newTexts.get(ctx.expr()) +
                "ireturn\n");
    }

    @Override
    public void exitTest(tinyPythonParser.TestContext ctx) {
        String expr1 = newTexts.get(ctx.expr(0));
        String expr2 = newTexts.get(ctx.expr(1));
        String comp_op = newTexts.get(ctx.comp_op());
        newTexts.put(ctx, expr1 + expr2 + comp_op );
    }

    @Override
    public void exitPrint_stmt(tinyPythonParser.Print_stmtContext ctx) {
        newTexts.put(ctx, "getstatic java/lang/System/out Ljava/io/PrintStream;\n" +
                newTexts.get(ctx.print_arg()) +
                "invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V\n");
    }

    @Override
    public void exitPrint_arg(tinyPythonParser.Print_argContext ctx) {
        if (ctx.STRING() != null){
            // STRING
            newTexts.put(ctx, "ldc " + ctx.STRING().getText() + "\n");
        }else{
            //expr
            newTexts.put(ctx, newTexts.get(ctx.expr()) +
                    "invokestatic java/lang/String/valueOf(I)Ljava/lang/String;\n");
        }
    }

    @Override
    public void exitComp_op(tinyPythonParser.Comp_opContext ctx) {
        switch (ctx.getText()){
            case ">":
                newTexts.put(ctx, "if_icmple ");
                break;
            case "<":
                newTexts.put(ctx, "if_icmpge ");
                break;
            case "==":
                newTexts.put(ctx, "if_icmpne ");
                break;
            case ">=":
                newTexts.put(ctx, "if_icmplt ");
                break;
            case "<=":
                newTexts.put(ctx, "if_icmpgt ");
                break;
            case "!=":
                newTexts.put(ctx, "if_icmpeq ");
                break;
        }
    }

    @Override
    public void exitExpr(tinyPythonParser.ExprContext ctx) {
        if (ctx.getChildCount() == 1){
            // NUMBER
            newTexts.put(ctx, "ldc "+ctx.NUMBER()+"\n");
        }else if (ctx.getChildCount() == 2) {
            //NAME opt_paren
            String name = ctx.NAME().getText();
            String opt_paren = newTexts.get(ctx.opt_paren());
            if (opt_paren.isEmpty()) {
                newTexts.put(ctx, "iload " + varStack.indexOf(name) + "\n");
            }else{
                newTexts.put(ctx, opt_paren + "invokestatic " + fun_label.get(name) + "\n");
            }
        }else if (ctx.getChildCount() == 3 &&
                ctx.getChild(0).getText().equals("(")){
            //'(' expr ')'
            String s = newTexts.get(ctx.expr(0));
            newTexts.put(ctx, s);
        }else if (ctx.getChildCount() >= 3 &&
                ctx.getChild(1) != ctx.expr()){
            //expr (( '+' | '-' ) expr)+
            String s1 = null, s2 = null, op=null;
            s1 = newTexts.get(ctx.expr(0));
            for (int i = 1; i <ctx.expr().size(); i++){
                s1 += newTexts.get(ctx.expr(1));
            }
            op = ctx.getChild(1).getText();
            if(op.equals("+")){
                newTexts.put(ctx, s1+
                        "iadd \n");
            }else{
                newTexts.put(ctx, s1 +
                        "isub \n");
            }
        }
    }

    @Override
    public void exitOpt_paren(tinyPythonParser.Opt_parenContext ctx) {
        if (ctx.getChildCount() == 0){
            // null
            newTexts.put(ctx, "");
        }else if (ctx.getChildCount() == 2){
            // '(' ')'
            newTexts.put(ctx, "()");
        }else{
            //'(' expr (',' expr)* ')'
            String expr = newTexts.get(ctx.expr(0));
            for (int i = 1; i<ctx.expr().size(); i++){
                expr += newTexts.get(ctx.expr(i));
            }
            newTexts.put(ctx, expr);
        }
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        super.exitEveryRule(ctx);
    }

    @Override
    public void exitProgram(tinyPythonParser.ProgramContext ctx)
    {
//        newTexts.put(ctx, ctx.file_input().getText());
        newTexts.put(ctx,
                ".class public Test\n" +
                        ".super java/lang/Object\n" +
                        ".method public <init>()V\n" +
                        "aload_0\n" +
                        "invokenonvirtual java/lang/Object/<init>()V\n" +
                        "return\n" +
                        ".end method\n\n" +
                        newTexts.get(ctx.file_input()));
        formattedText.append(newTexts.get(ctx));
        System.out.println(formattedText);
    }
}
