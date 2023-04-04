/*
 * Copyright 2021 The Error Prone Authors.
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
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.fixByAddingNullableAnnotationToType;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.isAlreadyAnnotatedNullable;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.isInNullMarkedScope;
import static com.google.errorprone.bugpatterns.nullness.NullnessUtils.nullnessChecksShouldBeConservative;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Method overrides Object.equals but does not have @Nullable on its parameter",
    severity = SUGGESTION)
public class EqualsMissingNullable extends BugChecker implements MethodTreeMatcher {
  private final boolean beingConservative;

  @Inject
  EqualsMissingNullable(ErrorProneFlags flags) {
    this.beingConservative = nullnessChecksShouldBeConservative(flags);
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (beingConservative && state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH;
    }

    if (!equalsMethodDeclaration().matches(methodTree, state)) {
      return NO_MATCH;
    }

    VariableTree parameterTree = getOnlyElement(methodTree.getParameters());
    VarSymbol parameter = getSymbol(parameterTree);
    if (isAlreadyAnnotatedNullable(parameter)) {
      return NO_MATCH;
    }

    if (beingConservative && !isInNullMarkedScope(parameter, state)) {
      return NO_MATCH;
    }

    SuggestedFix fix = fixByAddingNullableAnnotationToType(state, parameterTree);
    if (fix.isEmpty()) {
      return NO_MATCH;
    }
    return describeMatch(parameterTree, fix);
  }
}
