package net.technicpack.launchercore.install.user.skins;

import net.technicpack.launchercore.install.user.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SkinRepository {

    private ISkinStore mSkinStore;
    private ISkinMapper mSkinMapper;
    private List<ISkinListener> mListeners = new LinkedList<ISkinListener>();

    public SkinRepository(ISkinMapper mapper, ISkinStore skinStore) {
        this.mSkinMapper = mapper;
        this.mSkinStore = skinStore;
    }

    public void addListener(ISkinListener listener) {
        this.mListeners.add(listener);
    }

    protected void triggerFaceReady(User user) {
        for (ISkinListener listener : mListeners) {
            listener.faceReady(user);
        }
    }

    protected void triggerSkinReady(User user) {
        for (ISkinListener listener : mListeners) {
            listener.skinReady(user);
        }
    }

    public BufferedImage getFaceImage(User user) {
        String expectedFilename = mSkinMapper.getFaceFilename(user);
        File asset = new File(expectedFilename);

        BufferedImage output = null;

        try {
            asset.mkdirs();
            output = ImageIO.read(asset);
        } catch (IOException ex) {
            //It almost certainly just doesn't exist and that's OK
        }

        if (output == null) {
            final User outputUser = user;
            Thread faceThread = new Thread("Face Download: " + user.getDisplayName()) {
                @Override
                public void run() {
                    downloadFaceImage(outputUser);
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            triggerFaceReady(outputUser);
                        }
                    });
                }
            };
            faceThread.start();
            return mSkinMapper.getDefaultFaceImage();
        }

        return output;
    }

    public BufferedImage getSkinImage(User user) {
        String expectedFilename = mSkinMapper.getSkinFilename(user);
        File asset = new File(expectedFilename);

        BufferedImage output = null;

        try {
            asset.mkdirs();
            output = ImageIO.read(asset);
        } catch (IOException ex) {
            //It almost certainly just doesn't exist and that's OK
        }

        if (output == null) {
            final User outputUser = user;
            Thread skinThread = new Thread("Skin Download: " + user.getDisplayName()) {
                @Override
                public void run() {
                    downloadSkinImage(outputUser);
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            triggerSkinReady(outputUser);
                        }
                    });
                }
            };
            skinThread.start();
            return mSkinMapper.getDefaultSkinImage();
        }

        return output;
    }

    public BufferedImage getDefaultFace() {
        return mSkinMapper.getDefaultFaceImage();
    }

    public BufferedImage getDefaultSkin() {
        return mSkinMapper.getDefaultSkinImage();
    }

    protected void downloadFaceImage(User user) {
        String target = this.mSkinMapper.getFaceFilename(user);

        File targetFile = new File(target);
        targetFile.mkdirs();

        this.mSkinStore.downloadUserFace(user, target);
    }

    protected void downloadSkinImage(User user) {
        String target = this.mSkinMapper.getSkinFilename(user);

        File targetFile = new File(target);
        targetFile.mkdirs();

        this.mSkinStore.downloadUserSkin(user, target);
    }
}
