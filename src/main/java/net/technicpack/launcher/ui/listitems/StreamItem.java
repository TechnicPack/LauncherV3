package net.technicpack.launcher.ui.listitems;

public class StreamItem  {
    private String text;
    private String stream;

    public StreamItem(String text, String stream) {
        this.text = text;
        this.stream = stream;
    }

    public String getStream() { return stream; }
    public String toString() { return text; }
}
