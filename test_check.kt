fun main() {
    // Check NavHost methods
    println("NavHost methods:")
    androidx.navigation.NavHost::class.java.declaredMethods.forEach { println("  ${it.name}") }
    
    // Check Modifier methods
    println("\nModifier methods (first 20):")
    androidx.compose.ui.Modifier::class.java.declaredMethods.take(20).forEach { println("  ${it.name}") }
}
