import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID


// 1. Define the custom UUID Serializer
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

@Serializable
class Person (
    @Serializable(with = UUIDSerializer::class)  val id: UUID,
    val name: String,
    val age: Int,
    val details: PersonDetails
)

@Serializable
class PersonDetails (
    val address: String,
    val phoneNumber: String
)

interface IPersonRepository {
    fun getPersonById(id: UUID): Person?
    fun getAllPersons(): List<Person>
    fun addPerson(person: Person): UUID
    fun updatePerson(id: UUID, person: Person)
    fun deletePerson(id: UUID)
    fun deleteAll()
}