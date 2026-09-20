import kotlinx.serialization.Serializable

@Serializable
class Person (
    val id: Int,
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
    fun getPersonById(id: Int): Person?
    fun getAllPersons(): List<Person>
    fun addPerson(person: Person): Int
    fun updatePerson(id: Int, person: Person)
    fun deletePerson(id: Int)
    fun deleteAll()
}