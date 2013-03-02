// "Change method signature to 'bar(a : B)'" "true"
open class A {
    open fun bar(a: B) {}
}

class B : A(){
    <caret>override fun bar(a: B) {}
}