@file:Suppress("NoWildcardImports")
package websockets

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.util.concurrent.CountDownLatch
import javax.websocket.*

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ElizaServerTest {

    private lateinit var container: WebSocketContainer

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setup() {
        container = ContainerProvider.getWebSocketContainer()
    }

    @Test
    fun onOpen() {
        val latch = CountDownLatch(3)
        val list = mutableListOf<String>()

        val client = ElizaOnOpenMessageHandler(list, latch)
        container.connectToServer(client, URI("ws://localhost:$port/eliza"))
        latch.await()
        assertEquals(3, list.size)
        assertEquals("The doctor is in.", list[0])
    }

    @Test
    fun onChat() {
        val latch = CountDownLatch(4)
        val list = mutableListOf<String>()

        val client = ElizaOnOpenMessageHandlerToComplete(list, latch)
        container.connectToServer(client, URI("ws://localhost:$port/eliza"))
        latch.await()
        // Mirar si hay exactamente 4 mensajes (3 open + 1 Eliza)
        // assertEquals(4, list.size)
        // No sabes si vas a tener 4 o 5 mensajes puede haber mandado más de un mensaje
        assertTrue(list.size > 3)
        // Llega el mensaje con 'allways' miro si el cuarto coincide con lo que debe responder
        assertEquals("Can you think of a specific example?", list[3])
    }
}

@ClientEndpoint
class ElizaOnOpenMessageHandler(private val list: MutableList<String>, private val latch: CountDownLatch) {
    @OnMessage
    fun onMessage(message: String) {
        list.add(message)
        latch.countDown()
    }
}

@ClientEndpoint
class ElizaOnOpenMessageHandlerToComplete(private val list: MutableList<String>, private val latch: CountDownLatch) {

    @OnMessage
    fun onMessage(message: String, session: Session) {
        list.add(message)
        latch.countDown()
        // Si hay 3 mensajes (open) mando un mensaje
        if (list.size == 3) {
            // Mando un mensaje con el substring allways para comporobar si responde lo que debe
            session.basicRemote.sendText("I'm always happy")
        }
    }
}
