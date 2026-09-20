import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class SerializableScannerProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val namespace: String
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val targetAnnotation = "kotlinx.serialization.Serializable"
        val symbols = resolver.getSymbolsWithAnnotation(targetAnnotation)
        logger.info("SerializableProcessor: scanning symbols annotated with $targetAnnotation")

        val validClasses = symbols
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .toList()
        val deferredSymbols = symbols.filter { !it.validate() }.toList()

        logger.info(
            "SerializableProcessor: found ${validClasses.size} valid serializable classes" +
                if (deferredSymbols.isNotEmpty()) {
                    "; deferring ${deferredSymbols.size} symbols"
                } else {
                    ""
                }
        )

        if (validClasses.isNotEmpty()) {
            generateReflectionFeature(validClasses)
        } else {
            logger.info("SerializableProcessor: no valid serializable classes found; skipping generation")
        }

        return deferredSymbols
    }

    private fun generateReflectionFeature(classes: List<KSClassDeclaration>) {
        val packageName = namespace
        val fileName = "ReflectionFeature"

        // TypeNames for external dependencies
        val featureClass = ClassName("org.graalvm.nativeimage.hosted", "Feature")
        val beforeAnalysisAccessClass = ClassName("org.graalvm.nativeimage.hosted.Feature", "BeforeAnalysisAccess")
        val runtimeReflectionClass = ClassName("org.graalvm.nativeimage.hosted", "RuntimeReflection")
        val companionObjectFunc = MemberName("kotlin.reflect.full", "companionObject")

        // Format the class reference list (e.g. listOf(Greeting::class, Greeting2::class))
        val classListCode = classes.joinToString(", ") { "%T::class" }
        val classTypeArgs = classes.map { it.toClassName() }.toTypedArray()

        val beforeAnalysisFunction = FunSpec.builder("beforeAnalysis")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("access", beforeAnalysisAccessClass.copy(nullable = true))
            .beginControlFlow("listOf($classListCode).forEach { it ->", *classTypeArgs)
            .addStatement("val clazz = it.java")
            .addComment("Register class and members for reflection")
            .addStatement("%T.register(clazz)", runtimeReflectionClass)
            .beginControlFlow("it.%M?.java?.let { companionClass ->", companionObjectFunc)
            .addStatement("%T.register(companionClass)", runtimeReflectionClass)
            .addStatement("companionClass.declaredConstructors.forEach { %T.register(it) }", runtimeReflectionClass)
            .addStatement("companionClass.declaredFields.forEach { %T.register(it) }", runtimeReflectionClass)
            .addStatement("companionClass.declaredMethods.forEach { %T.register(it) }", runtimeReflectionClass)
            .endControlFlow()
            .addStatement("clazz.declaredConstructors.forEach { %T.register(it) }", runtimeReflectionClass)
            .addStatement("clazz.declaredFields.forEach { %T.register(it) }", runtimeReflectionClass)
            .addStatement("clazz.declaredMethods.forEach { %T.register(it) }", runtimeReflectionClass)
            .addStatement("%T.registerAllDeclaredConstructors(Class.forName(%S))", runtimeReflectionClass, "kotlin.reflect.jvm.internal.KClassImpl")
            .addStatement("%T.registerAllDeclaredMethods(Class.forName(%S))", runtimeReflectionClass, "kotlin.reflect.jvm.internal.KClassImpl")
            .endControlFlow()
            .build()

        val featureType = TypeSpec.classBuilder(fileName)
            .addSuperinterface(featureClass)
            .addFunction(beforeAnalysisFunction)
            .build()

        val fileSpec = FileSpec.builder(packageName, fileName)
            .addType(featureType)
            .build()

        // Write file with KSP dependencies attached for incremental compilation
        fileSpec.writeTo(
            codeGenerator,
            dependencies = Dependencies(true, *classes.mapNotNull { it.containingFile }.toTypedArray())
        )
        logger.info(
            "SerializableProcessor: generated $packageName.$fileName for " +
                classes.joinToString { it.qualifiedName?.asString() ?: it.simpleName.asString() }
        )
    }
}

class SerializableScannerProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SerializableScannerProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            namespace = environment.options[OPTION_NAMESPACE] ?: DEFAULT_NAMESPACE
        )
    }

    private companion object {
        const val OPTION_NAMESPACE = "serializable.processor.namespace"
        const val DEFAULT_NAMESPACE = "generated.graalvm"
    }
}