package bar

class S<T> {
    fun foo() {
        <!TYPE_PARAMETER_IS_NOT_AN_EXPRESSION, UNUSED_EXPRESSION!>T<!>
        <!TYPE_PARAMETER_ON_LHS_OF_DOT!>T<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>create<!>()
    }
}
