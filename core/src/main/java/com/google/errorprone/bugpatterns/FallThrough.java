/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SwitchTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.Reachability;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Position;
import java.util.List;
import java.util.regex.Pattern;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(altNames = "fallthrough", summary = "Switch case may fall through", severity = WARNING)
public class FallThrough extends BugChecker implements SwitchTreeMatcher {

  private static final Pattern FALL_THROUGH_PATTERN =
      Pattern.compile("\\bfalls?.?through\\b", Pattern.CASE_INSENSITIVE);

  @Override
  public Description matchSwitch(SwitchTree tree, VisitorState state) {
    PeekingIterator<CaseTree> it = Iterators.peekingIterator(tree.getCases().iterator());
    while (it.hasNext()) {
      CaseTree caseTree = it.next();
      if (!it.hasNext()) {
        break;
      }
      CaseTree next = it.peek();
      List<? extends StatementTree> statements = caseTree.getStatements();
      if (statements == null || statements.isEmpty()) {
        continue;
      }
      // We only care whether the last statement completes; javac would have already
      // reported an error if that statement wasn't reachable, and the answer is
      // independent of any preceding statements.
      boolean completes = Reachability.canCompleteNormally(getLast(statements));
      int endPos = caseEndPosition(state, caseTree);
      if (endPos == Position.NOPOS) {
        break;
      }
      String comments =
          state
              .getSourceCode()
              .subSequence(endPos, ((JCTree) next).getStartPosition())
              .toString()
              .trim();
      if (completes && !FALL_THROUGH_PATTERN.matcher(comments).find()) {
        state.reportMatch(
            buildDescription(next)
                .setMessage(
                    "Execution may fall through from the previous case; add a `// fall through`"
                        + " comment before this line if it was deliberate")
                .build());
      } else if (!completes && FALL_THROUGH_PATTERN.matcher(comments).find()) {
        state.reportMatch(
            buildDescription(next)
                .setMessage(
                    "Switch case has 'fall through' comment, but execution cannot fall through"
                        + " from the previous case")
                .build());
      }
    }
    return NO_MATCH;
  }

  private static int caseEndPosition(VisitorState state, CaseTree caseTree) {
    // if the statement group is a single block statement, handle fall through comments at the
    // end of the block
    if (caseTree.getStatements().size() == 1) {
      StatementTree only = getOnlyElement(caseTree.getStatements());
      if (only instanceof BlockTree) {
        BlockTree blockTree = (BlockTree) only;
        return blockTree.getStatements().isEmpty()
            ? getStartPosition(blockTree)
            : state.getEndPosition(getLast(blockTree.getStatements()));
      }
    }
    return state.getEndPosition(caseTree);
  }
}
