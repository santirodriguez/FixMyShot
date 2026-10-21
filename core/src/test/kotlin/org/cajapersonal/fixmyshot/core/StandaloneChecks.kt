package org.cajapersonal.fixmyshot.core

fun main() {
    contractCases().forEach { (name, case) -> case(); println("PASS: $name") }
    geometryCases().forEach { (name, case) -> case(); println("PASS: $name") }
    println("${contractCases().size} contract cases passed")
    println("${geometryCases().size} geometry cases passed")
}
