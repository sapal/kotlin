package foo

class X {}

val s = <!EXPRESSION_EXPECTED_NAMESPACE_FOUND!>java<!>
val ss = <!INVISIBLE_MEMBER, FUNCTION_CALL_EXPECTED!>System<!>
val sss = <!FUNCTION_CALL_EXPECTED!>X<!>
val xs = java.<!EXPRESSION_EXPECTED_NAMESPACE_FOUND!>lang<!>
val xss = java.lang.<!INVISIBLE_MEMBER, FUNCTION_CALL_EXPECTED!>System<!>
val xsss = foo.<!FUNCTION_CALL_EXPECTED!>X<!>
val xssss = <!EXPRESSION_EXPECTED_NAMESPACE_FOUND!>foo<!>