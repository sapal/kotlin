// "Change method signature to 'override fun bar(a: Int)'" "true"
trait A {
    fun bar(a: Int);
}

trait B : A {
    <caret>override fun bar(a: String);
}