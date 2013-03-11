// "Change method signature to 'override fun bar(a: Int): Int'" "true"
open class A {
    open fun bar(a: Int): Int {
        return 0
    }
}

class B : A(){
    <caret>override fun bar(a: Int): Int = 7
}