// "Change method signature to 'override fun bar()'" "true"
trait A {
    fun bar();
}
trait B {
    fun bar();
}

class C : A, B {
    <caret>override fun bar() {}
}