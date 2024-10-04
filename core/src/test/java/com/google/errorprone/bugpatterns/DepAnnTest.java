/*
 * Copyright 2013 The Error Prone Authors.
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DepAnnTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DepAnn.class, getClass());

  private static final ImmutableList<String> JAVACOPTS = ImmutableList.of("-Xlint:-dep-ann");

  @Test
  public void positiveCase() {
    compilationHelper
        .setArgs(JAVACOPTS)
        .addSourceLines(
            "DepAnnPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @deprecated
             */
            // BUG: Diagnostic contains: @Deprecated
            public class DepAnnPositiveCases {

              /**
               * @deprecated
               */
              // BUG: Diagnostic contains: @Deprecated
              public DepAnnPositiveCases() {}

              /**
               * @deprecated
               */
              // BUG: Diagnostic contains: @Deprecated
              int myField;

              /**
               * @deprecated
               */
              // BUG: Diagnostic contains: @Deprecated
              enum Enum {
                VALUE,
              }

              /**
               * @deprecated
               */
              // BUG: Diagnostic contains: @Deprecated
              interface Interface {}

              /**
               * @deprecated
               */
              // BUG: Diagnostic contains: @Deprecated
              public void deprecatedMethood() {}
            }""")
        .doTest();
  }

  @Test
  public void negativeCase1() {
    compilationHelper
        .setArgs(JAVACOPTS)
        .addSourceLines(
            "DepAnnNegativeCase1.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @deprecated
             */
            @Deprecated
            public class DepAnnNegativeCase1 {

              /**
               * @deprecated
               */
              @Deprecated
              public DepAnnNegativeCase1() {}

              /**
               * @deprecated
               */
              @Deprecated int myField;

              /**
               * @deprecated
               */
              @Deprecated
              enum Enum {
                VALUE,
              }

              /**
               * @deprecated
               */
              @Deprecated
              interface Interface {}

              /**
               * @deprecated
               */
              @Deprecated
              public void deprecatedMethood() {}

              @Deprecated
              public void deprecatedMethoodWithoutComment() {}

              /** deprecated */
              public void deprecatedMethodWithMalformedComment() {}

              /**
               * @deprecated
               */
              @SuppressWarnings("dep-ann")
              public void suppressed() {}

              public void newMethod() {}
            }""")
        .doTest();
  }

  @Test
  public void negativeCase2() {
    compilationHelper
        .setArgs(JAVACOPTS)
        .addSourceLines(
            "DepAnnNegativeCase2.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @deprecated
             */
            @Deprecated
            public class DepAnnNegativeCase2 {

              abstract class Builder2<P> {
                class SummaryRowKey<P> {}

                @Deprecated
                /**
                 * @deprecated use {@link Selector.Builder#withSummary()}
                 */
                public abstract void withSummaryRowKeys(int summaryRowKeys);

                /**
                 * @deprecated use {@link Selector.Builder#withSummary()}
                 */
                @Deprecated
                public abstract void m1();

                public abstract void m2();
              }
            }""")
        .doTest();
  }

  @Test
  public void disableable() {
    compilationHelper
        .setArgs(ImmutableList.of("-Xlint:-dep-ann", "-Xep:DepAnn:OFF"))
        .expectNoDiagnostics()
        .addSourceLines(
            "DepAnnPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @deprecated
             */
            // BUG: Diagnostic contains: @Deprecated
            public class DepAnnPositiveCases {

              /**
               * @deprecated
               */
              // BUG: Diagnostic contains: @Deprecated
              public DepAnnPositiveCases() {}

              /**
               * @deprecated
               */
              // BUG: Diagnostic contains: @Deprecated
              int myField;

              /**
               * @deprecated
               */
              // BUG: Diagnostic contains: @Deprecated
              enum Enum {
                VALUE,
              }

              /**
               * @deprecated
               */
              // BUG: Diagnostic contains: @Deprecated
              interface Interface {}

              /**
               * @deprecated
               */
              // BUG: Diagnostic contains: @Deprecated
              public void deprecatedMethood() {}
            }""")
        .doTest();
  }
}
