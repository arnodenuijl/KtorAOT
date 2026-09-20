import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection
import kotlin.reflect.full.companionObject


object AllSerializableClasses {
    val classes =
        domain.generated.KotlinXSerializableClasses.classes +
                webapi.generated.KotlinXSerializableClasses.classes
}

class ReflectionFeature : Feature {
    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess?) {
        AllSerializableClasses.classes.forEach {
            val clazz = it.java
            // Register class and members for reflection
            RuntimeReflection.register(clazz)
            it.companionObject?.java?.let { companionClass ->
                RuntimeReflection.register(companionClass)
                companionClass.declaredConstructors.forEach { RuntimeReflection.register(it) }
                companionClass.declaredFields.forEach { RuntimeReflection.register(it) }
                companionClass.declaredMethods.forEach { RuntimeReflection.register(it) }
            }
            clazz.declaredConstructors.forEach { RuntimeReflection.register(it) }
            clazz.declaredFields.forEach { RuntimeReflection.register(it) }
            clazz.declaredMethods.forEach { RuntimeReflection.register(it) }
        }
        RuntimeReflection.registerAllDeclaredConstructors(Class.forName("kotlin.reflect.jvm.internal.KClassImpl"))
        RuntimeReflection.registerAllDeclaredMethods(Class.forName("kotlin.reflect.jvm.internal.KClassImpl"))
    }
}