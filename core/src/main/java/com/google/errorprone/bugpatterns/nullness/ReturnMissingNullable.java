/*
 * Copyright 2016 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.suppliers.Suppliers.JAVA_LANG_VOID_TYPE;
import static com.google.errorprone.util.ASTHelpers.findEnclosingMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;
import static javax.lang.model.type.TypeKind.TYPEVAR;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "ReturnMissingNullable",
    summary = "Method returns a definitely null value but is not annotated @Nullable",
    severity = SUGGESTION)
public class ReturnMissingNullable extends BugChecker implements CompilationUnitTreeMatcher {
  private final boolean beingConservative;

  public ReturnMissingNullable(ErrorProneFlags flags) {
    this.beingConservative = flags.getBoolean("ReturnMissingNullable:Conservative").orElse(true);
  }

  @Override
  public Description matchCompilationUnit(
      CompilationUnitTree tree, VisitorState stateForCompilationUnit) {
    if (beingConservative && stateForCompilationUnit.errorProneOptions().isTestOnlyTarget()) {
      // Annotating test code for nullness can be useful, but it's not our primary focus.
      return NO_MATCH;
    }

    /*
     * In each scanner below, we define helper methods so that we can return early without the
     * verbosity of `return super.visitFoo(...)`.
     */

    ImmutableSet.Builder<VarSymbol> definitelyNullVarsBuilder = ImmutableSet.builder();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        doVisitVariable(tree);
        return super.visitVariable(tree, unused);
      }

      void doVisitVariable(VariableTree tree) {
        VarSymbol symbol = getSymbol(tree);
        if (!isConsideredFinal(symbol)) {
          return;
        }

        ExpressionTree initializer = tree.getInitializer();
        if (initializer == null) {
          return;
        }

        if (initializer.getKind() != NULL_LITERAL) {
          return;
        }

        definitelyNullVarsBuilder.add(symbol);
      }
    }.scan(tree, null);
    ImmutableSet<VarSymbol> definitelyNullVars = definitelyNullVarsBuilder.build();

    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitReturn(ReturnTree tree, Void unused) {
        doVisitReturn(tree);
        return super.visitReturn(tree, unused);
      }

      void doVisitReturn(ReturnTree returnTree) {
        /*
         * We need the the VisitorState to have the correct TreePath for (a) the call to
         * findEnclosingMethod and (b) the call to NullnessFixes (which looks up identifiers).
         */
        VisitorState state = stateForCompilationUnit.withPath(getCurrentPath());

        ExpressionTree returnExpression = returnTree.getExpression();
        if (returnExpression == null) {
          return;
        }

        MethodTree methodTree = findEnclosingMethod(state);
        if (methodTree == null) {
          return;
        }

        List<? extends StatementTree> statements = methodTree.getBody().getStatements();
        if (beingConservative
            && statements.size() == 1
            && getOnlyElement(statements) == returnTree
            && returnExpression.getKind() == NULL_LITERAL) {
          /*
           * When the entire method body is `return null`, I worry that this may be a stub
           * implementation that all "real" implementations are meant to override. Ideally such
           * stubs would use implementation like `throw new UnsupportedOperationException()`, but
           * let's assume the worst.
           */
          return;
        }

        MethodSymbol method = getSymbol(methodTree);
        Type returnType = method.getReturnType();
        if (beingConservative && isVoid(returnType, state)) {
          // `@Nullable Void` is accurate but noisy, so some users won't want it.
          return;
        }
        if (returnType.isPrimitive()) {
          // Buggy code, but adding @Nullable just makes it worse.
          return;
        }
        if (beingConservative && state.getTypes().isArray(returnType)) {
          /*
           * Type-annotation syntax on arrays can be confusing, and this refactoring doesn't get it
           * right yet.
           */
          return;
        }
        if (beingConservative && returnType.getKind() == TYPEVAR) {
          /*
           * Consider AbstractFuture.getDoneValue: It returns a literal `null`, but it shouldn't be
           * annotated @Nullable because it returns null *only* if the AbstractFuture's type
           * argument permits that.
           */
          return;
        }

        if (NullnessAnnotations.fromAnnotationsOn(method).orElse(null) == Nullness.NULLABLE) {
          return;
        }

        /*
         * TODO(cpovirk): Consider reporting only one finding per method? Our patching
         * infrastructure is smart enough not to mind duplicate suggested fixes, but users might be
         * annoyed by multiple robocomments with the same fix.
         */
        if (hasDefinitelyNullBranch(
            returnExpression, definitelyNullVars, stateForCompilationUnit)) {
          state.reportMatch(
              describeMatch(
                  returnTree, NullnessFixes.makeFix(state.withPath(getCurrentPath()), methodTree)));
        }
      }
    }.scan(tree, null);

    return NO_MATCH; // Any reports were made through state.reportMatch.
  }

  // TODO(cpovirk): Move this somewhere sensible, maybe into a renamed NullnessFixes?
  static boolean hasDefinitelyNullBranch(
      ExpressionTree tree,
      ImmutableSet<VarSymbol> definitelyNullVars,
      VisitorState stateForCompilationUnit) {
    return new SimpleTreeVisitor<Boolean, Void>() {
      @Override
      public Boolean visitAssignment(AssignmentTree tree, Void unused) {
        return visit(tree.getExpression(), unused);
      }

      @Override
      public Boolean visitConditionalExpression(ConditionalExpressionTree tree, Void unused) {
        return visit(tree.getTrueExpression(), unused) || visit(tree.getFalseExpression(), unused);
      }

      @Override
      public Boolean visitParenthesized(ParenthesizedTree tree, Void unused) {
        return visit(tree.getExpression(), unused);
      }

      @Override
      public Boolean visitTypeCast(TypeCastTree tree, Void unused) {
        return visit(tree.getExpression(), unused);
      }

      @Override
      protected Boolean defaultAction(Tree tree, Void unused) {
        /*
         * This covers not only "Void" and "CAP#1 extends Void" but also the null literal. (It
         * covers the null literal even through parenthesized expressions. Still, we end up
         * needing special handling for parenthesized expressions for cases like `(foo ? bar :
         * null)`.)
         */
        return isVoid(getType(tree), stateForCompilationUnit)
            || definitelyNullVars.contains(getSymbol(tree));
      }
    }.visit(tree, null);
  }

  // TODO(cpovirk): Move this somewhere sensible, maybe into a renamed NullnessFixes?
  static boolean isVoid(Type type, VisitorState state) {
    return type != null && state.getTypes().isSubtype(type, JAVA_LANG_VOID_TYPE.get(state));
  }
}
