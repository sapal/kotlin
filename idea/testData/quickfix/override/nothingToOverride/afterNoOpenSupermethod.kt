// "Change method signature to 'bar(a : B)'" "false"
// ERROR: 'bar' overrides nothing
// ACTION: Remove 'override' modifier
open class A {
    fun bar(a: B) {}
}

class B : A(){
    <caret>override fun bar(a: A) {}
}