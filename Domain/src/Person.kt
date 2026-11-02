import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
class Person (
    val id: Uuid,
    val name: String,
    val age: Int,
    val details: PersonDetails
)

@Serializable
class PersonDetails (
    val address: String,
    val phoneNumber: String
)

