import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;

class ModelGuiServerTest {

    ModelGuiServer modelGuiServer;
    Connection connection;
    @BeforeEach
    public void newModel() {
        modelGuiServer = new ModelGuiServer();
        connection = Mockito.mock(Connection.class);
        modelGuiServer.addUser("Максим", connection);
    }

    @Test
    void getAllUsersMultiChat() {
        assertThat(modelGuiServer.getAllUsersMultiChat(), hasEntry("Максим", connection));
    }

    @Test
    void addUser() {
        connection = Mockito.mock(Connection.class);
        modelGuiServer.addUser("Сергей", connection);
        assertThat(modelGuiServer.getAllUsersMultiChat(), hasEntry("Сергей", connection));
    }

    @Test
    void removeUser() {
        modelGuiServer.removeUser("Максим");
        assertThat(modelGuiServer.getAllUsersMultiChat(), not(hasEntry("Сергей", connection)));
    }
}