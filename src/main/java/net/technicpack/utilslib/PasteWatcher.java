package net.technicpack.utilslib;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PasteWatcher implements ClipboardOwner {

    private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    private ActionListener pasteListener;

    public PasteWatcher(ActionListener listener) {
        this.pasteListener = listener;

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                takeControlAndProcess();
            }
        });
    }

    protected void takeControlAndProcess() {
        Transferable contents;

        try {
            contents = clipboard.getContents(this);
            clipboard.setContents(contents, this);
        } catch (Exception ex) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    takeControlAndProcess();
                }
            });

            return;
        }

        pasteListener.actionPerformed(new ActionEvent(contents, 0, ""));
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                takeControlAndProcess();
            }
        });
    }
}
