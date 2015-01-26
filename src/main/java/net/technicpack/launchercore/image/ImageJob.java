/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

public class ImageJob<T> {
    protected IImageMapper<T> mapper;
    protected IImageStore<T> store;

    private T lastJobData;

    protected boolean canRetry = true;
    private AtomicReference<BufferedImage> imageReference;

    private Collection<IImageJobListener<T>> jobListeners = new LinkedList<IImageJobListener<T>>();

    public ImageJob(IImageMapper<T> mapper, IImageStore<T> store) {
        this.mapper = mapper;
        this.store = store;

        imageReference = new AtomicReference<BufferedImage>();
        imageReference.set(mapper.getDefaultImage());
    }

    public BufferedImage getImage() {
        return imageReference.get();
    }

    public void addJobListener(IImageJobListener listener) {
        synchronized (jobListeners) {
            jobListeners.add(listener);
        }
    }

    public void removeJobListener(IImageJobListener listener) {
        synchronized (jobListeners) {
            jobListeners.remove(listener);
        }
    }

    public boolean canRetry() {
        return canRetry;
    }

    public void refreshRetry() {
        canRetry = true;
    }

    public T getJobData() {
        return lastJobData;
    }

    protected void setImage(BufferedImage image) {
        canRetry = false;
        imageReference.set(image);

        notifyComplete();
    }

    protected void notifyComplete() {
        if (EventQueue.isDispatchThread()) {
            synchronized (jobListeners) {
                for (IImageJobListener listener : jobListeners) {
                    listener.jobComplete(this);
                }
            }
        } else {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    notifyComplete();
                }
            });
        }
    }

    public void start(final T jobData) {
        lastJobData = jobData;

        Thread imageThread = new Thread("Image Download: " + store.getJobKey(jobData)) {
            @Override
            public void run() {
                try {
                    File imageLocation = mapper.getImageLocation(jobData);

                    BufferedImage existingImage = null;

                    if (imageLocation != null && imageLocation.exists()) {
                        try {
                            existingImage = ImageIO.read(imageLocation);
                        } catch (IOException ex) {
                            //Corrupt or missing- that's fine, just redownload it
                        }
                    }

                    if (existingImage != null)
                        setImage(existingImage);

                    if (store.canDownloadImage(jobData, imageLocation) && (existingImage == null || mapper.shouldDownloadImage(jobData))) {
                        if (imageLocation != null && !imageLocation.getParentFile().exists())
                            imageLocation.getParentFile().mkdirs();

                        store.downloadImage(jobData, imageLocation);

                        try {
                            BufferedImage newImage = ImageIO.read(imageLocation);

                            if (newImage != null)
                                setImage(newImage);
                        } catch (IOException ex) {
                            //Again- probably something wrong with the image, so we'll just show the default
                        }
                    }
                } finally {
                    if (canRetry)
                        canRetry = store.canRetry(jobData);
                }
            }
        };
        imageThread.start();
    }
}
