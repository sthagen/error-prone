/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.MoreAnnotations.getValue;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import java.util.Optional;

/** See the {@code summary}. */
@BugPattern(
    summary = "Giving BugPatterns a name different to the enclosing class can be confusing",
    severity = WARNING)
public final class BugPatternNaming extends BugChecker implements ClassTreeMatcher {
  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!isSubtype(getType(tree), state.getTypeFromString(BugChecker.class.getName()), state)) {
      return NO_MATCH;
    }
    var classSymbol = getSymbol(tree);
    var attribute = classSymbol.attribute(state.getSymbolFromString(BugPattern.class.getName()));
    if (attribute == null) {
      // e.g. abstract subtypes of BugChecker
      return NO_MATCH;
    }
    return getValue(attribute, "name")
        .flatMap(MoreAnnotations::asStringValue)
        .filter(name -> !name.isEmpty())
        .flatMap(
            name -> {
              if (!classSymbol.name.contentEquals(name)) {
                return Optional.of(describeMatch(tree));
              }
              return removeName(tree, state);
            })
        .orElse(NO_MATCH);
  }

  private Optional<Description> removeName(ClassTree tree, VisitorState state) {
    return tree.getModifiers().getAnnotations().stream()
        .filter(
            anno ->
                isSameType(
                    getType(anno.getAnnotationType()),
                    state.getTypeFromString(BugPattern.class.getName()),
                    state))
        .findFirst()
        .flatMap(
            anno ->
                anno.getArguments().stream()
                    .filter(
                        t ->
                            t instanceof AssignmentTree
                                && ((IdentifierTree) ((AssignmentTree) t).getVariable())
                                    .getName()
                                    .contentEquals("name"))
                    .findFirst()
                    .map(
                        ele ->
                            buildDescription(anno)
                                .setMessage(
                                    "Setting @BugPattern.name to the class name of the check is"
                                        + " redundant")
                                .addFix(
                                    SuggestedFixes.removeElement(ele, anno.getArguments(), state))
                                .build()));
  }
}
