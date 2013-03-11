// "Change method signature to 'override fun bar(y: Int, x: String)'" "true"
open class A {
    open fun bar(a: Int, b: String) {}
}

class B : A(){
    <caret>override fun bar(y: Int, x: String) {}
}