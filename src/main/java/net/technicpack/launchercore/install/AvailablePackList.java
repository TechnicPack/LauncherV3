package net.technicpack.launchercore.install;

import net.technicpack.launchercore.exception.RestfulAPIException;
import net.technicpack.launchercore.install.user.IAuthListener;
import net.technicpack.launchercore.install.user.User;
import net.technicpack.launchercore.mirror.MirrorStore;
import net.technicpack.launchercore.restful.PackInfo;
import net.technicpack.launchercore.restful.RestObject;
import net.technicpack.launchercore.restful.platform.PlatformPackInfo;
import net.technicpack.launchercore.restful.solder.FullModpacks;
import net.technicpack.launchercore.restful.solder.Solder;
import net.technicpack.launchercore.restful.solder.SolderConstants;
import net.technicpack.launchercore.restful.solder.SolderPackInfo;
import net.technicpack.launchercore.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class AvailablePackList implements IAuthListener, PackRefreshListener {
    private IPackStore mPackStore;
    private Collection<String> mForcedSolderPacks = new ArrayList<String>();
    private List<IPackListener> mPackListeners = new LinkedList<IPackListener>();
    private final MirrorStore mirrorStore;

    public AvailablePackList(IPackStore packStore, MirrorStore mirrorStore) {
        this.mPackStore = packStore;
        this.mirrorStore = mirrorStore;
        this.mPackStore.put(new AddPack());
    }

    public void addForcedSolderPack(String solderLocation) {
        mForcedSolderPacks.add(solderLocation);
    }

    @Override
    public void userChanged(User user) {
        if (user == null) {
            return;
        }

        reloadAllPacks(user);
    }

    public void addPackListener(IPackListener listener) {
        mPackListeners.add(listener);
    }

    public void removePackListener(IPackListener listener) {
        mPackListeners.remove(listener);
    }

    public void triggerUpdateListeners(InstalledPack pack) {
        for (IPackListener listener : mPackListeners) {
            listener.updatePack(pack);
        }
    }

    @Override
    public void refreshPack(InstalledPack pack) {
        final InstalledPack threadPack = pack;
        final AvailablePackList packList = this;
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                packList.triggerUpdateListeners(threadPack);
            }
        });
    }

    public void add(InstalledPack pack) {
        mPackStore.add(pack);
        mPackStore.save();
        pack.setRefreshListener(this);
        triggerUpdateListeners(pack);
    }

    public void put(InstalledPack pack) {
        mPackStore.put(pack);
        mPackStore.save();
        pack.setRefreshListener(this);
        triggerUpdateListeners(pack);
    }

    public void remove(InstalledPack pack) {
        mPackStore.remove(pack.getName());
        mPackStore.save();
    }

    public InstalledPack getOffsetPack(int offset) {
        int index = mPackStore.getSelectedIndex();

        index += offset;

        int size = mPackStore.getPackNames().size();

        while (index < 0) {
            index += size;
        }
        while (index >= size) {
            index -= size;
        }

        return mPackStore.getInstalledPacks().get(mPackStore.getPackNames().get(index));
    }

    public void setPack(InstalledPack pack) {

        int index = mPackStore.getPackNames().indexOf(pack.getName());

        if (index >= 0) {
            mPackStore.setSelectedIndex(index);
            mPackStore.save();
        }
    }

    public void save() {
        mPackStore.save();
    }

    public void reloadAllPacks(User user) {
        final User threadUser = user;
        final AvailablePackList packList = this;

        for (final String packName : mPackStore.getPackNames()) {
            final InstalledPack pack = mPackStore.getInstalledPacks().get(packName);
            if (pack.isPlatform()) {
                Thread thread = new Thread(pack.getName() + " Info Loading Thread") {
                    @Override
                    public void run() {
                        try {
                            String name = pack.getName();
                            PlatformPackInfo platformPackInfo = PlatformPackInfo.getPlatformPackInfo(name);
                            PackInfo info = platformPackInfo;
                            if (platformPackInfo.hasSolder()) {
                                info = SolderPackInfo.getSolderPackInfo(platformPackInfo.getSolder(), name, threadUser);
                            }

                            info.getLogo();
                            info.getIcon();
                            info.getBackground();
                            pack.setInfo(info);
                            pack.setRefreshListener(packList);
                        } catch (RestfulAPIException e) {
                            Utils.getLogger().log(Level.WARNING, "Unable to load platform pack " + pack.getName(), e);
                            pack.setLocalOnly();
                            pack.setRefreshListener(packList);
                        }

                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                packList.triggerUpdateListeners(pack);
                            }
                        });
                    }
                };

                thread.start();
            }
        }

        Thread thread = new Thread("Technic Solder Defaults") {
            @Override
            public void run() {
                int index = 0;

                try {
                    FullModpacks technic = RestObject.getRestObject(FullModpacks.class, SolderConstants.getFullSolderUrl(SolderConstants.TECHNIC, threadUser.getProfile().getName()));
                    Solder solder = new Solder(SolderConstants.TECHNIC, technic.getMirrorUrl());
                    for (SolderPackInfo info : technic.getModpacks().values()) {
                        String name = info.getName();
                        info.setSolder(solder);

                        InstalledPack pack = null;
                        if (mPackStore.getInstalledPacks().containsKey(name)) {
                            pack = mPackStore.getInstalledPacks().get(info.getName());
                            pack.setRefreshListener(packList);
                            pack.setInfo(info);

                            final InstalledPack deferredPack = pack;
                            final int deferredIndex = index;
                            EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    packList.triggerUpdateListeners(deferredPack);
                                    mPackStore.reorder(deferredIndex, deferredPack.getName());
                                }
                            });
                        } else {
                            pack = new InstalledPack(mirrorStore, name, false);
                            pack.setRefreshListener(packList);
                            pack.setInfo(info);

                            final InstalledPack deferredPack = pack;
                            final int deferredIndex = index;
                            EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    mPackStore.add(deferredPack);
                                    packList.triggerUpdateListeners(deferredPack);
                                    mPackStore.reorder(deferredIndex, deferredPack.getName());
                                }
                            });
                        }


                        index++;
                    }
                } catch (RestfulAPIException e) {
                    Utils.getLogger().log(Level.WARNING, "Unable to load technic modpacks", e);

                    for (String packName : mPackStore.getPackNames()) {
                        InstalledPack pack = mPackStore.getInstalledPacks().get(packName);
                        if (!pack.isPlatform() && pack.getInfo() == null && pack.getName() != null)
                            pack.setLocalOnly();
                    }
                }
            }
        };
        thread.start();

        thread = new Thread("Forced Solder Thread") {

            @Override
            public void run() {
                for (String solder : mForcedSolderPacks) {
                    try {
                        SolderPackInfo info = SolderPackInfo.getSolderPackInfo(solder);
                        if (info == null) {
                            throw new RestfulAPIException();
                        }

                        info.getLogo();
                        info.getIcon();
                        info.getBackground();

                        InstalledPack pack = null;
                        if (mPackStore.getInstalledPacks().containsKey(info.getName())) {
                            pack = mPackStore.getInstalledPacks().get(info.getName());
                            pack.setInfo(info);

                            final InstalledPack deferredPack = pack;
                            EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    packList.triggerUpdateListeners(deferredPack);
                                }
                            });
                        } else {
                            pack = new InstalledPack(mirrorStore, info.getName(), true);
                            pack.setRefreshListener(packList);
                            pack.setInfo(info);

                            final InstalledPack deferredPack = pack;
                            EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    mPackStore.add(deferredPack);
                                    packList.triggerUpdateListeners(deferredPack);
                                }
                            });
                        }

                    } catch (RestfulAPIException e) {
                        Utils.getLogger().log(Level.WARNING, "Unable to load forced solder pack " + solder, e);
                    }
                }
            }
        };

        thread.start();
    }
}
