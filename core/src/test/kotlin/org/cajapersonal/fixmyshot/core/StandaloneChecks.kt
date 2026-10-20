package org.cajapersonal.fixmyshot.core

fun main() {
    contractCases().forEach { (name, case) -> case(); println("PASS: $name") }
    println("${contractCases().size} contract cases passed")
}
