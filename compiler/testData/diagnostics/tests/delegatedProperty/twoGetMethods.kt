class A {
    val c: Int by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>Delegate()<!>
}

class Delegate {
    fun get(t: Int, p: String): Int {
        t.equals(p) // to avoid UNUSED_PARAMETER warning
        return 1
    }

    fun get(t: String, p: String): Int {
        t.equals(p) // to avoid UNUSED_PARAMETER warning
        return 1
    }
}
