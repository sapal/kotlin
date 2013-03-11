// "Change method signature to 'override fun <T>bar(y: T, x: Int)'" "true"
open class A {
    open fun <T>bar(a: T, b: Int) {}
}

class B : A(){
    <caret>override fun <T>bar(y: T, x: Int) {}
}