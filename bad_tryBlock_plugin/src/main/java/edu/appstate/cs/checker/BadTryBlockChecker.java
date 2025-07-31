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
import java.util.ArrayList;

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
        List<Description> descriptions = new ArrayList<>();

        if (methodTree.getBody() != null) {
            for (StatementTree statement : methodTree.getBody().getStatements()) {
                if (statement instanceof TryTree tryTree) {
                    List<Description> catchDescriptions = checkCatches(tryTree, state);
                    descriptions.addAll(catchDescriptions);
                    checkCatchBlocks(tryTree, state, descriptions);
                }
            }
        }
        for (Description d : descriptions) {
            state.reportMatch(d);
        }
        return Description.NO_MATCH;
    }

    private List<Description> checkCatches(TryTree tryTree, VisitorState state) {
        List<Description> descriptions = new java.util.ArrayList<>();
        List<? extends CatchTree> catches = tryTree.getCatches();
         Types types = state.getTypes();

        if (catches.isEmpty()) {
            descriptions.add(
                buildDescription(tryTree)
                .setMessage("Try block must have at least one catch clause")
                .build()
            );
            return descriptions;
        }

        for (CatchTree catchTree : catches) {
            String typeStr = getCatchType(catchTree, state).toString();
            if (typeStr.equals("java.lang.Exception")) {
                 descriptions.add(
                    buildDescription(catchTree)
                    .setMessage("Must use a more specific exception type than Exception")
                    .build()
             );
            } else if (typeStr.equals("java.lang.Throwable")) {
                descriptions.add(
                    buildDescription(catchTree)
                        .setMessage("Must use a more specific exception type than Throwable")
                        .build()
                );
            }
        }

        return descriptions;
    }

    private void checkCatchBlocks(TryTree tryTree, VisitorState state, List<Description> descriptions) {
        for (CatchTree catchTree : tryTree.getCatches()) {
            BlockTree catchBlock = catchTree.getBlock();
            if (catchBlock.getStatements().isEmpty()) {
                descriptions.add(
                    buildDescription(catchTree)
                        .setMessage("Catch block must have at least one statement.")
                        .build()
                );
            }
        }
    }
    private Type getCatchType(CatchTree catchTree, VisitorState state) {
        return (Type) ((JCTree.JCVariableDecl) catchTree.getParameter()).sym.type;
    }
}