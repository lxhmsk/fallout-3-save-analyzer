package ui;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DirectoryWatcher {

  public enum EventType {
    CREATED, MODIFIED
  }
  
  public interface FileWatchListener {
    void onEvent(Path p, EventType type);
  }

  private final WatchService watchService;
  private final Map<Path, FileWatchListener> listeners = new HashMap<>();

  private Thread watcherThread;
  private boolean running = false;

  public DirectoryWatcher() throws IOException {
    watchService = FileSystems.getDefault().newWatchService();
  }

  public void add(Path p, FileWatchListener listener) throws IOException {
    p.register(
        watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY);
    listeners.put(p, listener);
  }

  private class WatcherThread extends Thread {
    
    WatcherThread() {
      setDaemon(true);
    }
    
    public void run() {
      
      while (true) {

        WatchKey key;
        try {
          key = watchService.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          continue;
        }

        if (!running) {
          return;
        }

        if (key == null) {
          continue;
        }

        for (WatchEvent<?> event : key.pollEvents()) {

          @SuppressWarnings("unchecked")
          WatchEvent<Path> e = (WatchEvent<Path>)event;

          Kind<?> kind = e.kind();
          if (kind == StandardWatchEventKinds.OVERFLOW) {
            continue;
          }

          Path p = ((Path) key.watchable()).resolve(e.context());
          FileWatchListener listener = listeners.get(key.watchable());
          if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            listener.onEvent(p, EventType.CREATED);
          } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            listener.onEvent(p, EventType.MODIFIED);
          }
        }
        key.reset();
      }
    }
  }

  public void start() {
    if (watcherThread == null) {
      running = true;
      watcherThread = new WatcherThread();
      watcherThread.start();
    }
  }
  
  public void stop() {
    running = false;
    watcherThread = null;
  }
}