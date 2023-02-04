package Client;

import java.io.IOException;
import java.io.*;
import java.util.*;
import java.nio.file.*;

public class FileMonitor {
    private WatchService service;
    public Map<WatchKey, Path> keyMap;
    String notify;

    public String getMonitorNotify() {
        return notify;
    }

    public FileMonitor(Path path) throws IOException, InterruptedException {
        service = FileSystems.getDefault().newWatchService();
        keyMap = new HashMap<>();
        keyMap.put(path.register(service, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE), path);

        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    WatchKey watchKey;
                    try {
                        watchKey = service.take();
                    } catch (InterruptedException i) {
                        // TODO: handle exception
                        return;
                    }

                    Path evenDir = keyMap.get(watchKey);
                    for (WatchEvent<?> event : watchKey.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path evenPath = (Path) event.context();
                        notify = evenDir + ": " + kind + ": " + evenPath;
                    }
                }
            }
        }).start();
    }
}
