@file:OptIn(ExperimentalKtorApi::class)

import KtorApplication.rootModule
import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database

object ApplicationKt {
    @JvmStatic
    fun main(args: Array<String>) {
        val database = Database.connect(
            url = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1?oracle.jdbc.jsonDefaultGetObjectType=java.lang.String",
            driver = "oracle.jdbc.OracleDriver",
            user = "ktor",
            password = "ktor"
        )

        val repos = PersonRepository(database).apply {
            // Example usage of the repository
            val newPersonId = addPerson(
                Person(
                    0, "John Doe", 30, PersonDetails(
                        "Some details",
                        phoneNumber = "06-12345678"
                    )
                )
            )
            println("Added person with ID: $newPersonId")
            // Example usage of the repository
            val newPersonId2 = addPerson(
                Person(
                    0, "Mary Doe", 30, PersonDetails(
                        "Some details",
                        phoneNumber = "06-12345678"
                    )
                )
            )
            println("Added person with ID: $newPersonId2")
            val person = getPersonById(newPersonId)
            println("Retrieved person: $person")
        }

        embeddedServer(
            factory = CIO,
            port = 8080
        ) {

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            rootModule(repos)
        }.start(wait = true)
    }
}

object KtorApplication {
    fun Application.rootModule(repos: PersonRepository) {
        configureRouting(repos)
    }
}


fun Application.configureRouting(repos: PersonRepository) {
    routing {
        swaggerUI(path = "swagger") {
            info = OpenApiInfo("My API", "1.0")
            source = OpenApiDocSource.Routing(ContentType.Application.Json) {
                routingRoot.descendants()
            }

        }
        personRoutes(repos)

        get("/") {
            call.respond(Greeting("Hello World!"))
        }
        get("/listDir") {
            val directoryListing = try {
                getDirectoryListing()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error listing directory: ${e.message}")
                return@get
            }
            call.respond(directoryListing)
        }
        get("/greet/{name}") {
            val name = call.parameters["name"] ?: "World"
            when (name) {
                "arno" -> call.respond(HttpStatusCode.OK, Greeting("Hello Arno!"))
                "" -> call.respond(HttpStatusCode.BadRequest, "Name parameter is empty")
                else -> call.respond(HttpStatusCode.Created, Greeting2("Hello $name!"))
            }
        }

    }
//            .describe {
//            responses {
//                HttpStatusCode.OK {
//                    description = "Returns a greeting message"
//                    schema = jsonSchema<Greeting>()
//                }
//            }
//        }
}
