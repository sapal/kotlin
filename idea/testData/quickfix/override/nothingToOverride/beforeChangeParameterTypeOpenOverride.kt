// "Change method signature to 'override fun bar()'" "true"
open class A {
    open fun bar() {}
}

open class B : A() {
    open override fun bar() {}
}

class C : B() {
    <caret>override fun bar(a : Int) {}
}