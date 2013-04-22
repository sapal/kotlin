// "Change function signature to 'override fun f(b: Y, z: Int)'" "true"

open class X {}
open class Y : X() {}

open class A {
    open fun f(y: Y, z: Int) {}
}

open class B : A() {
    <caret>override fun f(a: Double, b: X) {}
}