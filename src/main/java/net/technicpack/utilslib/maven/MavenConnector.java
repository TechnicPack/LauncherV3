/**
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

package net.technicpack.utilslib.maven;

import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.launchercore.util.DownloadListener;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.util.ArrayList;
import java.util.List;

public class MavenConnector {
    private List<RemoteRepository> defaultRepositories;
    private RepositorySystem system;
    private DefaultRepositorySystemSession session;

    public MavenConnector(LauncherDirectories directories, String... defaultRepositories) {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                exception.printStackTrace();
            }
        });

        locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );
        this.system = locator.getService( RepositorySystem.class );
        this.session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepository = new LocalRepository(directories.getCacheDirectory());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepository));

        this.defaultRepositories = new ArrayList<RemoteRepository>(defaultRepositories.length/2);

        for (int i = 0; i < defaultRepositories.length/2; i++) {
            String key = defaultRepositories[i*2];
            String url = defaultRepositories[i*2+1];
            this.defaultRepositories.add(new RemoteRepository.Builder(key, "default", url).build());
        }
    }

    public boolean attemptLibraryDownload(String name) {
        return attemptLibraryDownload(name, null, null);
    }

    public boolean attemptLibraryDownload(String name, DownloadListener listener) {
        return attemptLibraryDownload(name, null, listener);
    }

    public boolean attemptLibraryDownload(String name, String url, DownloadListener listener) {
        Dependency dependency = new Dependency(new DefaultArtifact(name), "compile");

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);

        if (url != null)
            collectRequest.addRepository(new RemoteRepository.Builder("targetted", "default", url).build());

        for (RemoteRepository repo : defaultRepositories) {
            collectRequest.addRepository(repo);
        }

        try {
            DependencyNode node = system.collectDependencies(session, collectRequest).getRoot();

            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setRoot(node);

            if (listener != null)
                session.setTransferListener(new MavenListenerAdapter(listener));
            system.resolveDependencies(session, dependencyRequest);
            return true;
        } catch (DependencyCollectionException ex) {
            return false;
        } catch (DependencyResolutionException ex) {
            return false;
        }
    }
}
