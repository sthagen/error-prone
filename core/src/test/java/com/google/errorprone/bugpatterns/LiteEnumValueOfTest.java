/*
 * Copyright 2019 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link LiteEnumValueOf}.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public final class LiteEnumValueOfTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(LiteEnumValueOf.class, getClass())
          .addSourceFile("android/testdata/stubs/android/os/Parcel.java")
          .addSourceFile("android/testdata/stubs/android/os/Parcelable.java")
          .addSourceLines(
              "FakeLiteEnum.java",
              "enum FakeLiteEnum implements com.google.protobuf.Internal.EnumLite {",
              "  FOO;",
              "  @Override public int getNumber() {",
              "    return 0;",
              "  }",
              "}");

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    // BUG: Diagnostic contains:",
            "    FakeLiteEnum.valueOf(\"FOO\");",
            "    // BUG: Diagnostic contains:",
            "    FakeLiteEnum.FOO.valueOf(\"FOO\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "Usage.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestEnum;",
            "class Usage {",
            "  private TestEnum testMethod() {",
            "    return TestEnum.valueOf(\"FOO\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCaseJDK9OrAbove() {
    compilationHelper
        .addSourceLines(
            "ProtoLiteEnum.java",
            "enum ProtoLiteEnum {",
            "  FOO(1),",
            "  BAR(2);",
            "  private final int number;",
            "  private ProtoLiteEnum(int number) {",
            "    this.number = number;",
            "  }",
            "  public int getNumber() {",
            "    return number;",
            "  }",
            "}")
        .addSourceLines("TestData.java", "class TestData {}")
        .addSourceLines(
            "$AutoValue_TestData.java",
            "import javax.annotation.processing.Generated;",
            "@Generated(\"com.google.auto.value.processor.AutoValueProcessor\")",
            "class $AutoValue_TestData extends TestData {}")
        .addSourceLines(
            "AutoValue_TestData.java",
            "import android.os.Parcel;",
            "import android.os.Parcelable;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestEnum;",
            "import javax.annotation.processing.Generated;",
            "@Generated(\"com.ryanharter.auto.value.parcel.AutoValueParcelExtension\")",
            "class AutoValue_TestData extends $AutoValue_TestData {",
            "    AutoValue_TestData(ProtoLiteEnum protoLiteEnum) {}",
            "    public static final Parcelable.Creator<AutoValue_TestData> CREATOR =",
            "        new Parcelable.Creator<AutoValue_TestData>() {",
            "          @Override",
            "          public AutoValue_TestData createFromParcel(Parcel in) {",
            "            return new AutoValue_TestData(ProtoLiteEnum.valueOf(\"FOO\"));",
            "          }",
            "          @Override",
            "          public AutoValue_TestData[] newArray(int size) {",
            "            return null;",
            "          }",
            "        };",
            "}")
        .doTest();
  }
}
