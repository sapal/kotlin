class A {
    fun foo(k: Int) = k

    fun result() = this.(::foo)(111)
}

fun box(): String {
    val result = A().result()
    if (result != 111) return "Fail $result"
    return "OK"
}
