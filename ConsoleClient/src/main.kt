import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

object ApplicationKt {
    private val logger = LoggerFactory.getLogger(ApplicationKt::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Application Kt")
        runBlocking {
            while (true) {
                try {
                    print("Enter person ID: ")
                    val input = readlnOrNull()?.trim()
                    if (input.isNullOrEmpty()) break


                    val personId = Uuid.parse(input)
                    val personApiClient = PersonApiClient("http://localhost:9000")
                    val person = personApiClient.getPersonById(personId)
                    if (person != null) {
                        println("Person found: $person")
                    } else {
                        println("Person not found for ID: $personId")
                    }
                } catch (e: Exception) {
                    println("Error occurred: ${e.message}")
                }
            }
            println("Exiting application.")
        }
    }
}


@Serializable
data class Person(
    val id: Uuid,
    val name: String,
    val age: Int,
    val address: String,
    val phoneNumber: String
)