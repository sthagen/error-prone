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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
@RunWith(JUnit4.class)
public class FragmentNotInstantiableTest {
  /** Used for testing a custom FragmentNotInstantiable. */
  @BugPattern(
      summary =
          "Subclasses of CustomFragment must be instantiable via Class#newInstance():"
              + " the class must be public, static and have a public nullary constructor",
      severity = WARNING)
  public static class CustomFragmentNotInstantiable extends FragmentNotInstantiable {
    public CustomFragmentNotInstantiable() {
      super(ImmutableSet.of("com.google.errorprone.bugpatterns.android.testdata.CustomFragment"));
    }
  }

  @Test
  public void positiveCases() {
    createCompilationTestHelper(FragmentNotInstantiable.class)
        .addSourceLines(
            "FragmentNotInstantiablePositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            import android.app.Fragment;

            /**
             * @author avenet@google.com (Arnaud J. Venet)
             */
            public class FragmentNotInstantiablePositiveCases {
              // BUG: Diagnostic contains: public
              static class PrivateFragment extends Fragment {
                public PrivateFragment() {}
              }

              // BUG: Diagnostic contains: public
              static class PrivateV4Fragment extends android.support.v4.app.Fragment {
                public PrivateV4Fragment() {}
              }

              public static class PrivateConstructor extends Fragment {
                // BUG: Diagnostic contains: public
                PrivateConstructor() {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class NoConstructor extends Fragment {
                public NoConstructor(int x) {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class NoConstructorV4 extends android.support.v4.app.Fragment {
                public NoConstructorV4(int x) {}
              }

              public static class ParentFragment extends Fragment {
                public ParentFragment() {}
              }

              public static class ParentFragmentV4 extends android.support.v4.app.Fragment {
                public ParentFragmentV4() {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class DerivedFragmentNoConstructor extends ParentFragment {
                public DerivedFragmentNoConstructor(int x) {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class DerivedFragmentNoConstructorV4 extends ParentFragmentV4 {
                public DerivedFragmentNoConstructorV4(boolean b) {}
              }

              public class EnclosingClass {
                // BUG: Diagnostic contains: static
                public class InnerFragment extends Fragment {
                  public InnerFragment() {}
                }

                public Fragment create1() {
                  // BUG: Diagnostic contains: public
                  return new Fragment() {};
                }

                public Fragment create2() {
                  // BUG: Diagnostic contains: public
                  class LocalFragment extends Fragment {}
                  return new LocalFragment();
                }
              }
            }""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    createCompilationTestHelper(FragmentNotInstantiable.class)
        .addSourceLines(
            "FragmentNotInstantiableNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            import android.app.Fragment;

            /**
             * @author avenet@google.com (Arnaud J. Venet)
             */
            public class FragmentNotInstantiableNegativeCases {
              public static class NotAFragment1 {
                public NotAFragment1(int x) {}
              }

              public static class NotAFragment2 {
                private NotAFragment2() {}
              }

              private static class NotAFragment3 {}

              public class NotAFragment4 {}

              private abstract class AbstractFragment extends Fragment {
                public AbstractFragment(int x) {}
              }

              private abstract class AbstractV4Fragment extends android.support.v4.app.Fragment {
                private int a;

                public int value() {
                  return a;
                }
              }

              public static class MyFragment extends Fragment {
                private int a;

                public int value() {
                  return a;
                }
              }

              public static class DerivedFragment extends MyFragment {}

              public static class MyV4Fragment extends android.support.v4.app.Fragment {}

              public static class DerivedV4Fragment extends MyV4Fragment {
                private int a;

                public int value() {
                  return a;
                }
              }

              public static class MyFragment2 extends Fragment {
                public MyFragment2() {}

                public MyFragment2(int x) {}
              }

              public static class DerivedFragment2 extends MyFragment2 {
                public DerivedFragment2() {}

                public DerivedFragment2(boolean b) {}
              }

              public static class EnclosingClass {
                public static class InnerFragment extends Fragment {
                  public InnerFragment() {}
                }
              }

              interface AnInterface {
                public class ImplicitlyStaticInnerFragment extends Fragment {}

                class ImplicitlyStaticAndPublicInnerFragment extends Fragment {}
              }
            }""")
        .doTest();
  }

  @Test
  public void positiveCases_custom() {
    createCompilationTestHelper(CustomFragmentNotInstantiable.class)
        .addSourceLines(
            "FragmentNotInstantiablePositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            import android.app.Fragment;

            /**
             * @author avenet@google.com (Arnaud J. Venet)
             */
            public class FragmentNotInstantiablePositiveCases {
              // BUG: Diagnostic contains: public
              static class PrivateFragment extends Fragment {
                public PrivateFragment() {}
              }

              // BUG: Diagnostic contains: public
              static class PrivateV4Fragment extends android.support.v4.app.Fragment {
                public PrivateV4Fragment() {}
              }

              public static class PrivateConstructor extends Fragment {
                // BUG: Diagnostic contains: public
                PrivateConstructor() {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class NoConstructor extends Fragment {
                public NoConstructor(int x) {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class NoConstructorV4 extends android.support.v4.app.Fragment {
                public NoConstructorV4(int x) {}
              }

              public static class ParentFragment extends Fragment {
                public ParentFragment() {}
              }

              public static class ParentFragmentV4 extends android.support.v4.app.Fragment {
                public ParentFragmentV4() {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class DerivedFragmentNoConstructor extends ParentFragment {
                public DerivedFragmentNoConstructor(int x) {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class DerivedFragmentNoConstructorV4 extends ParentFragmentV4 {
                public DerivedFragmentNoConstructorV4(boolean b) {}
              }

              public class EnclosingClass {
                // BUG: Diagnostic contains: static
                public class InnerFragment extends Fragment {
                  public InnerFragment() {}
                }

                public Fragment create1() {
                  // BUG: Diagnostic contains: public
                  return new Fragment() {};
                }

                public Fragment create2() {
                  // BUG: Diagnostic contains: public
                  class LocalFragment extends Fragment {}
                  return new LocalFragment();
                }
              }
            }""")
        .addSourceLines(
            "CustomFragment.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            /**
             * @author jasonlong@google.com (Jason Long)
             */
            public class CustomFragment {}""")
        .addSourceLines(
            "CustomFragmentNotInstantiablePositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            /**
             * @author jasonlong@google.com (Jason Long)
             */
            public class CustomFragmentNotInstantiablePositiveCases {
              // BUG: Diagnostic contains: public
              static class PrivateFragment extends CustomFragment {
                public PrivateFragment() {}
              }

              public static class PrivateConstructor extends CustomFragment {
                // BUG: Diagnostic contains: public
                PrivateConstructor() {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class NoConstructor extends CustomFragment {
                public NoConstructor(int x) {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class NoConstructorV4 extends android.support.v4.app.Fragment {
                public NoConstructorV4(int x) {}
              }

              public static class ParentFragment extends CustomFragment {
                public ParentFragment() {}
              }

              public static class ParentFragmentV4 extends android.support.v4.app.Fragment {
                public ParentFragmentV4() {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class DerivedFragmentNoConstructor extends ParentFragment {
                public DerivedFragmentNoConstructor(int x) {}
              }

              // BUG: Diagnostic contains: nullary constructor
              public static class DerivedFragmentNoConstructorV4 extends ParentFragmentV4 {
                public DerivedFragmentNoConstructorV4(boolean b) {}
              }

              public class EnclosingClass {
                // BUG: Diagnostic contains: static
                public class InnerFragment extends CustomFragment {
                  public InnerFragment() {}
                }

                public CustomFragment create1() {
                  // BUG: Diagnostic contains: public
                  return new CustomFragment() {};
                }

                public CustomFragment create2() {
                  // BUG: Diagnostic contains: public
                  class LocalFragment extends CustomFragment {}
                  return new LocalFragment();
                }
              }
            }""")
        .doTest();
  }

  @Test
  public void negativeCase_custom() {
    createCompilationTestHelper(CustomFragmentNotInstantiable.class)
        .addSourceLines(
            "FragmentNotInstantiableNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            import android.app.Fragment;

            /**
             * @author avenet@google.com (Arnaud J. Venet)
             */
            public class FragmentNotInstantiableNegativeCases {
              public static class NotAFragment1 {
                public NotAFragment1(int x) {}
              }

              public static class NotAFragment2 {
                private NotAFragment2() {}
              }

              private static class NotAFragment3 {}

              public class NotAFragment4 {}

              private abstract class AbstractFragment extends Fragment {
                public AbstractFragment(int x) {}
              }

              private abstract class AbstractV4Fragment extends android.support.v4.app.Fragment {
                private int a;

                public int value() {
                  return a;
                }
              }

              public static class MyFragment extends Fragment {
                private int a;

                public int value() {
                  return a;
                }
              }

              public static class DerivedFragment extends MyFragment {}

              public static class MyV4Fragment extends android.support.v4.app.Fragment {}

              public static class DerivedV4Fragment extends MyV4Fragment {
                private int a;

                public int value() {
                  return a;
                }
              }

              public static class MyFragment2 extends Fragment {
                public MyFragment2() {}

                public MyFragment2(int x) {}
              }

              public static class DerivedFragment2 extends MyFragment2 {
                public DerivedFragment2() {}

                public DerivedFragment2(boolean b) {}
              }

              public static class EnclosingClass {
                public static class InnerFragment extends Fragment {
                  public InnerFragment() {}
                }
              }

              interface AnInterface {
                public class ImplicitlyStaticInnerFragment extends Fragment {}

                class ImplicitlyStaticAndPublicInnerFragment extends Fragment {}
              }
            }""")
        .addSourceLines(
            "CustomFragment.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            /**
             * @author jasonlong@google.com (Jason Long)
             */
            public class CustomFragment {}""")
        .addSourceLines(
            "CustomFragmentNotInstantiableNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.android.testdata;

            /**
             * @author jasonlong@google.com (Jason Long)
             */
            public class CustomFragmentNotInstantiableNegativeCases {
              public static class NotAFragment1 {
                public NotAFragment1(int x) {}
              }

              public static class NotAFragment2 {
                private NotAFragment2() {}
              }

              private static class NotAFragment3 {}

              public class NotAFragment4 {}

              private abstract class AbstractFragment extends CustomFragment {
                public AbstractFragment(int x) {}
              }

              public static class MyFragment extends CustomFragment {
                private int a;

                public int value() {
                  return a;
                }
              }

              public static class DerivedFragment extends MyFragment {}

              public static class MyFragment2 extends CustomFragment {
                public MyFragment2() {}

                public MyFragment2(int x) {}
              }

              public static class DerivedFragment2 extends MyFragment2 {
                public DerivedFragment2() {}

                public DerivedFragment2(boolean b) {}
              }

              public static class EnclosingClass {
                public static class InnerFragment extends CustomFragment {
                  public InnerFragment() {}
                }
              }

              interface AnInterface {
                public class ImplicitlyStaticInnerFragment extends CustomFragment {}

                class ImplicitlyStaticAndPublicInnerFragment extends CustomFragment {}
              }
            }""")
        .doTest();
  }

  private CompilationTestHelper createCompilationTestHelper(
      Class<? extends FragmentNotInstantiable> bugCheckerClass) {
    return CompilationTestHelper.newInstance(bugCheckerClass, getClass())
        .addSourceLines(
            "Fragment.java",
            """
            package android.app;

            public class Fragment {}""")
        .addSourceLines(
            "Fragment.java",
            """
            package android.support.v4.app;

            public class Fragment {}""")
        .setArgs(ImmutableList.of("-XDandroidCompatible=true"));
  }
}
