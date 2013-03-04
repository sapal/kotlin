// "Change method signature to 'override fun bar(a: B)'" "true"
abstract class A {
    abstract fun bar(a: B);
}

class B : A(){
    <caret>override fun bar(a: B) {}
}