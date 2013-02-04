/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.jvm.compiler;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.util.regex.Pattern;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.test.InnerTestClasses;
import org.jetbrains.jet.test.TestMetadata;

import org.jetbrains.jet.jvm.compiler.AbstractCompileJavaAgainstKotlinTest;

/** This class is generated by {@link org.jetbrains.jet.generators.tests.GenerateTests}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/compileJavaAgainstKotlin")
@InnerTestClasses({CompileJavaAgainstKotlinTestGenerated.Class.class, CompileJavaAgainstKotlinTestGenerated.Method.class})
public class CompileJavaAgainstKotlinTestGenerated extends AbstractCompileJavaAgainstKotlinTest {
    public void testAllFilesPresentInCompileJavaAgainstKotlin() throws Exception {
        JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), "org.jetbrains.jet.generators.tests.GenerateTests", new File("compiler/testData/compileJavaAgainstKotlin"), Pattern.compile("^(.+)\\.kt$"), true);
    }
    
    @TestMetadata("compiler/testData/compileJavaAgainstKotlin/class")
    public static class Class extends AbstractCompileJavaAgainstKotlinTest {
        public void testAllFilesPresentInClass() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), "org.jetbrains.jet.generators.tests.GenerateTests", new File("compiler/testData/compileJavaAgainstKotlin/class"), Pattern.compile("^(.+)\\.kt$"), true);
        }
        
        @TestMetadata("DefaultConstructor.kt")
        public void testDefaultConstructor() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/class/DefaultConstructor.kt");
        }
        
        @TestMetadata("DefaultConstructorWithTwoArgs.kt")
        public void testDefaultConstructorWithTwoArgs() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/class/DefaultConstructorWithTwoArgs.kt");
        }
        
        @TestMetadata("ExtendsAbstractListT.kt")
        public void testExtendsAbstractListT() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/class/ExtendsAbstractListT.kt");
        }
        
        @TestMetadata("ImplementsListString.kt")
        public void testImplementsListString() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/class/ImplementsListString.kt");
        }
        
        @TestMetadata("ImplementsMapPP.kt")
        public void testImplementsMapPP() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/class/ImplementsMapPP.kt");
        }
        
        @TestMetadata("Simple.kt")
        public void testSimple() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/class/Simple.kt");
        }
        
    }
    
    @TestMetadata("compiler/testData/compileJavaAgainstKotlin/method")
    public static class Method extends AbstractCompileJavaAgainstKotlinTest {
        public void testAllFilesPresentInMethod() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), "org.jetbrains.jet.generators.tests.GenerateTests", new File("compiler/testData/compileJavaAgainstKotlin/method"), Pattern.compile("^(.+)\\.kt$"), true);
        }
        
        @TestMetadata("Any.kt")
        public void testAny() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/Any.kt");
        }
        
        @TestMetadata("ArrayOfIntArray.kt")
        public void testArrayOfIntArray() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/ArrayOfIntArray.kt");
        }
        
        @TestMetadata("ArrayOfIntegerArray.kt")
        public void testArrayOfIntegerArray() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/ArrayOfIntegerArray.kt");
        }
        
        @TestMetadata("GenericArray.kt")
        public void testGenericArray() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/GenericArray.kt");
        }
        
        @TestMetadata("Hello.kt")
        public void testHello() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/Hello.kt");
        }
        
        @TestMetadata("Int.kt")
        public void testInt() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/Int.kt");
        }
        
        @TestMetadata("IntArray.kt")
        public void testIntArray() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/IntArray.kt");
        }
        
        @TestMetadata("IntWithDefault.kt")
        public void testIntWithDefault() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/IntWithDefault.kt");
        }
        
        @TestMetadata("IntegerArray.kt")
        public void testIntegerArray() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/IntegerArray.kt");
        }
        
        @TestMetadata("ListOfInt.kt")
        public void testListOfInt() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/ListOfInt.kt");
        }
        
        @TestMetadata("ListOfString.kt")
        public void testListOfString() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/ListOfString.kt");
        }
        
        @TestMetadata("ListOfT.kt")
        public void testListOfT() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/ListOfT.kt");
        }
        
        @TestMetadata("MapOfKString.kt")
        public void testMapOfKString() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/MapOfKString.kt");
        }
        
        @TestMetadata("MapOfStringIntQ.kt")
        public void testMapOfStringIntQ() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/MapOfStringIntQ.kt");
        }
        
        @TestMetadata("QExtendsListString.kt")
        public void testQExtendsListString() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/QExtendsListString.kt");
        }
        
        @TestMetadata("QExtendsString.kt")
        public void testQExtendsString() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/QExtendsString.kt");
        }
        
        @TestMetadata("Vararg.kt")
        public void testVararg() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/Vararg.kt");
        }
        
        @TestMetadata("Void.kt")
        public void testVoid() throws Exception {
            doTest("compiler/testData/compileJavaAgainstKotlin/method/Void.kt");
        }
        
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite("CompileJavaAgainstKotlinTestGenerated");
        suite.addTestSuite(CompileJavaAgainstKotlinTestGenerated.class);
        suite.addTestSuite(Class.class);
        suite.addTestSuite(Method.class);
        return suite;
    }
}
