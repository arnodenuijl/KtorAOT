import kotlinx.serialization.Serializable
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection
import kotlin.collections.forEach
import kotlin.reflect.full.companionObject
//
//class ReflectionFeature : Feature {
//    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess?) {
//        listOf(Greeting::class, Greeting2::class).forEach {
//            val clazz = it.java
//
//            // Register class and members for reflection
//            RuntimeReflection.register(clazz)
//            it.companionObject?.java?.let { companionClass ->
//                RuntimeReflection.register(companionClass)
//                companionClass.declaredConstructors.forEach { RuntimeReflection.register(it) }
//                companionClass.declaredFields.forEach { RuntimeReflection.register(it) }
//                companionClass.declaredMethods.forEach { RuntimeReflection.register(it) }
//            }
//            clazz.declaredConstructors.forEach { RuntimeReflection.register(it) }
//            clazz.declaredFields.forEach { RuntimeReflection.register(it) }
//            clazz.declaredMethods.forEach { RuntimeReflection.register(it) }
//        }
//        RuntimeReflection.registerAllDeclaredConstructors(Class.forName("kotlin.reflect.jvm.internal.KClassImpl"))
//        RuntimeReflection.registerAllDeclaredMethods(Class.forName("kotlin.reflect.jvm.internal.KClassImpl"))
//    }
//}
//

@Serializable
data class Greeting(val message: String)


@Serializable
data class Greeting2(val otherMessage: String)