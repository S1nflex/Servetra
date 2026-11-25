package ua.edu.nupp.servetra.client.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public final class I18n {
    private static final Preferences PREFS = Preferences.userNodeForPackage(I18n.class);
    private Locale locale;
    private ResourceBundle bundle;

    public I18n() {
        String language = PREFS.get("locale", "uk");
        if ("en".equals(language)) {
            setLocale(Locale.ENGLISH);
        } else {
            setLocale(Locale.forLanguageTag("uk"));
        }
    }

    public String text(String key, Object... args) {
        String pattern = bundle.getString(key);
        return MessageFormat.format(pattern, args);
    }

    public Locale locale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        bundle = ResourceBundle.getBundle("i18n.messages", locale);
        PREFS.put("locale", locale.getLanguage());
    }
}

