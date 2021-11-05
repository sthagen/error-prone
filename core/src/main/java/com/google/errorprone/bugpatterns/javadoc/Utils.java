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

import static com.google.errorprone.names.LevenshteinEditDistance.getEditDistance;

import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.FixedPosition;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Position;
import java.util.Optional;
import javax.annotation.Nullable;

/** Common utility methods for fixing Javadocs. */
final class Utils {
  static Optional<String> getBestMatch(String to, int maxEditDistance, Iterable<String> choices) {
    String bestMatch = null;
    int minDistance = Integer.MAX_VALUE;
    for (String choice : choices) {
      int distance = getEditDistance(to, choice);
      if (distance < minDistance && distance < maxEditDistance) {
        bestMatch = choice;
        minDistance = distance;
      }
    }
    return Optional.ofNullable(bestMatch);
  }

  static DCDocComment getDocComment(VisitorState state, Tree tree) {
    return ((JCCompilationUnit) state.getPath().getCompilationUnit())
        .docComments.getCommentTree((JCTree) tree);
  }

  static SuggestedFix replace(DocTree docTree, String replacement, VisitorState state) {
    DocSourcePositions positions = JavacTrees.instance(state.context).getSourcePositions();
    CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();
    int startPos = getStartPosition(docTree, state);
    int endPos =
        (int) positions.getEndPosition(compilationUnitTree, getDocCommentTree(state), docTree);
    if (endPos == Position.NOPOS) {
      return SuggestedFix.emptyFix();
    }
    return SuggestedFix.replace(startPos, endPos, replacement);
  }

  static int getStartPosition(DocTree docTree, VisitorState state) {
    DocSourcePositions positions = JavacTrees.instance(state.context).getSourcePositions();
    CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();
    return (int) positions.getStartPosition(compilationUnitTree, getDocCommentTree(state), docTree);
  }

  static int getEndPosition(DocTree docTree, VisitorState state) {
    DocSourcePositions positions = JavacTrees.instance(state.context).getSourcePositions();
    CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();
    return (int) positions.getEndPosition(compilationUnitTree, getDocCommentTree(state), docTree);
  }

  /**
   * Gets a {@link DiagnosticPosition} for the {@link DocTree} pointed to by {@code path}, attached
   * to the {@link Tree} which it documents.
   */
  static DiagnosticPosition diagnosticPosition(DocTreePath path, VisitorState state) {
    int startPosition = getStartPosition(path.getLeaf(), state);
    Tree tree = path.getTreePath().getLeaf();
    if (startPosition == Position.NOPOS) {
      // javac doesn't seem to store positions for e.g. trivial empty javadoc like `/** */`
      // see: https://github.com/google/error-prone/issues/1981
      startPosition = ASTHelpers.getStartPosition(tree);
    }
    return getDiagnosticPosition(startPosition, tree);
  }

  static DiagnosticPosition getDiagnosticPosition(int startPosition, Tree tree) {
    return new FixedPosition(tree, startPosition);
  }

  @Nullable
  static DocTreePath getDocTreePath(VisitorState state) {
    DocCommentTree docCommentTree = getDocCommentTree(state);
    if (docCommentTree == null) {
      return null;
    }
    return new DocTreePath(state.getPath(), docCommentTree);
  }

  @Nullable
  private static DocCommentTree getDocCommentTree(VisitorState state) {
    return JavacTrees.instance(state.context).getDocCommentTree(state.getPath());
  }

  private Utils() {}
}
