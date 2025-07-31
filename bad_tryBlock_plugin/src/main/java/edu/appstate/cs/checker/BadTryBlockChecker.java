package edu.appstate.cs.checker;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.*;

import javax.lang.model.type.TypeMirror;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.BlockTree;
import java.util.List;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

@AutoService(BugChecker.class)
@BugPattern(
        name = "BadTryBlockChecker",
        summary = "Catches must be ordered from most specific to most general",
        severity = WARNING
)
public class BadTryBlockChecker extends BugChecker implements 
        BugChecker.MethodTreeMatcher {

    @Override
    public Description matchMethod(MethodTree methodTree, VisitorState state) {
        if (methodTree.getBody() != null) {
            for (StatementTree statement : methodTree.getBody().getStatements()) {
                if (statement instanceof TryTree tryTree) {
                    Description result = checkCatches(tryTree, state);
                    if (result != Description.NO_MATCH) {
                        return result;
                    }
                    result = checkCatchBlocks(tryTree);
                    if (result != Description.NO_MATCH) {
                        return result;
                    }
                }
            }
        }
        return Description.NO_MATCH;
    }

    private Description checkCatches(TryTree tryTree, VisitorState state) {

        List<? extends CatchTree> catches = tryTree.getCatches();
        Types types = state.getTypes();

        if (catches.isEmpty()) {
        return buildDescription(tryTree)
                .setMessage("Try block must have at least one catch clause")
                .build();
        }

        if (catches.size() == 1 && getCatchType(catches.get(0), state).toString().equals("java.lang.Exception")) {
                return buildDescription(catches.get(0))
                        .setMessage("Must use a more specific exception type than Exception")
                        .build();
        }

        if (catches.size() == 1 && getCatchType(catches.get(0), state).toString().equals("java.lang.Throwable")) {
                return buildDescription(catches.get(0))
                        .setMessage("Must use a more specific exception type than Throwable")
                        .build();
        }

        return Description.NO_MATCH;
    }

    private Description checkCatchBlocks(TryTree tryTree) {
        for (CatchTree catchTree : tryTree.getCatches()) {
            BlockTree catchBlock = catchTree.getBlock();
            if (catchBlock.getStatements().isEmpty()) {
                return buildDescription(catchTree)
                    .setMessage("Catch block must have at least one statement.")
                    .build();
            }
        }
        return Description.NO_MATCH;
    }
    private Type getCatchType(CatchTree catchTree, VisitorState state) {
        return (Type) ((JCTree.JCVariableDecl) catchTree.getParameter()).sym.type;
    }
}