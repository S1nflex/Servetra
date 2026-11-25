package ua.edu.nupp.servetra.client.i18n;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalizationTest {
    @Test void ukAndEnglishHaveIdenticalKeys() {
        ResourceBundle uk = ResourceBundle.getBundle("i18n.messages", Locale.forLanguageTag("uk"));
        ResourceBundle en = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        assertEquals(new TreeSet<>(uk.keySet()), new TreeSet<>(en.keySet()));
    }
}

