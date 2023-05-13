/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import javax.lang.model.element.Element;

/**
 * Matches errors in Javadoc {@literal @}throws tags.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    summary = "The documented method doesn't actually throw this checked exception.",
    severity = WARNING,
    documentSuppression = false)
public final class InvalidThrows extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path != null) {
      new ThrowsChecker(state, methodTree).scan(path, null);
    }
    return Description.NO_MATCH;
  }

  private final class ThrowsChecker extends DocTreePathScanner<Void, Void> {
    private final VisitorState state;
    private final MethodTree methodTree;

    private ThrowsChecker(VisitorState state, MethodTree methodTree) {
      this.state = state;
      this.methodTree = methodTree;
    }

    @Override
    public Void visitThrows(ThrowsTree throwsTree, Void unused) {
      ReferenceTree exName = throwsTree.getExceptionName();
      Element element =
          JavacTrees.instance(state.context).getElement(new DocTreePath(getCurrentPath(), exName));
      if (element != null) {
        Type type = (Type) element.asType();
        if (isCheckedException(type)) {
          if (methodTree.getThrows().stream().noneMatch(t -> isSubtype(type, getType(t), state))) {
            state.reportMatch(
                describeMatch(
                    diagnosticPosition(getCurrentPath(), state),
                    Utils.replace(throwsTree, "", state)));
          }
        }
      }
      return super.visitThrows(throwsTree, null);
    }

    private boolean isCheckedException(Type type) {
      return type.hasTag(TypeTag.CLASS)
          && !state.getTypes().isAssignable(type, state.getSymtab().errorType)
          && !state.getTypes().isAssignable(type, state.getSymtab().runtimeExceptionType);
    }
  }
}
