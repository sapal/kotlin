// "Change method signature to 'override fun bar(a: Int): Int'" "true"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>jet.Int</td></tr><tr><td>Found:</td><td>jet.String</td></tr></table></html>
open class A {
    open fun bar(a: Int): Int = 0
}

class B : A(){
    // Note that when parameter types match, RETURN_TYPE_MISMATCH_ON_OVERRIDE error is reported
    // and "Change method signature" quickfix is not present.
    <caret>override fun bar(a: Int): Int = "BAR"
}