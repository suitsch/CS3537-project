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
                    Description result = checkCatchOrder(tryTree.getCatches(), state);
                    if (result != Description.NO_MATCH) {
                        return result;
                    }
                }
            }
        }
        return Description.NO_MATCH;
    }

    private Description checkCatchOrder(List<? extends CatchTree> catches, VisitorState state) {
        Types types = state.getTypes();


        if (catches.size() == 1) {
            if (getCatchType(catches.get(0), state).toString().equals("java.lang.Exception")) {
                return buildDescription(catches.get(0))
                        .setMessage("Must use a more specific exception type than Exception")
                        .build();
            }
            return Description.NO_MATCH; 
        }

        for (int i = 0; i < catches.size(); i++) {
            CatchTree earlier = catches.get(i);
            Type earlierType = getCatchType(earlier, state);

            for (int j = i + 1; j < catches.size(); j++) {
                CatchTree later = catches.get(j);
                Type laterType = getCatchType(later, state);

                // laterType is assignable to earlierType = more specific caught after more general = error
                if (types.isAssignable( laterType, earlierType)) {
                    return buildDescription(later)
                            .setMessage("Catch block for more specific exception (" + laterType + ") appears after general one (" + earlierType + ")")
                            .build();
                }
            }
        }

        return Description.NO_MATCH;
    }

    private Type getCatchType(CatchTree catchTree, VisitorState state) {
        return (Type) ((JCTree.JCVariableDecl) catchTree.getParameter()).sym.type;
    }
}