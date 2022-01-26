/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberSelectTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.ElementKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Type parameter used as type qualifier",
    severity = ERROR,
    suppressionAnnotations = {})
public class TypeParameterQualifier extends BugChecker implements MemberSelectTreeMatcher {

  @Override
  public Description matchMemberSelect(MemberSelectTree tree, VisitorState state) {
    Symbol baseSym = ASTHelpers.getSymbol(tree.getExpression());
    if (baseSym == null || baseSym.getKind() != ElementKind.TYPE_PARAMETER) {
      return Description.NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(tree, SuggestedFixes.qualifyType(state, fix, ASTHelpers.getSymbol(tree)));
    return describeMatch(tree, fix.build());
  }
}
