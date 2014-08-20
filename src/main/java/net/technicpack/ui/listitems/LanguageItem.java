package net.technicpack.ui.listitems;

public class LanguageItem {
    private String langCode;
    private String text;

    public LanguageItem(String code, String defaultText) {
        this.langCode = code;
        this.text = defaultText;
    }

    public String getLangCode() { return langCode; }
    public String toString() { return text; }
}
