package com.owomeb.backend._5gbemowobackend


import com.owomeb.backend._5gbemowobackend.appControllers.BaseController
import com.owomeb.backend._5gbemowobackend.appControllers.BaseEntity
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*



@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BaseControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private fun getBaseUrl() = "http://localhost:$port/api/bases"

    @Test
    fun `test createBase, getAllBases and getBase`() {
        // 🔹 1. Tworzenie nowej bazy
        val sourceUrl = "https://example.com/my-3gpp-standard.pdf"
        val createRequest = BaseController.CreateBaseRequest(sourceUrl)
        val responseCreate = restTemplate.postForEntity(getBaseUrl(), createRequest, BaseEntity::class.java)

        Assertions.assertEquals(HttpStatus.OK, responseCreate.statusCode)
        val createdBase = responseCreate.body!!
        println("🔧 Utworzono bazę: $createdBase")

        // 🔹 2. Pobranie listy wszystkich baz
        val responseList = restTemplate.getForEntity(getBaseUrl(), Array<BaseEntity>::class.java)
        Assertions.assertEquals(HttpStatus.OK, responseList.statusCode)
        println("📚 Lista baz: ${responseList.body?.joinToString()}")

        // 🔹 3. Pobranie jednej konkretnej bazy
        val responseGet = restTemplate.getForEntity("${getBaseUrl()}/${createdBase.id}", BaseEntity::class.java)
        Assertions.assertEquals(HttpStatus.OK, responseGet.statusCode)
        println("🔍 Szczegóły bazy: ${responseGet.body}")
    }
}
