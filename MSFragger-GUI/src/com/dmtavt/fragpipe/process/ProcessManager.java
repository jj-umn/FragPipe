package com.dmtavt.fragpipe.process;

import com.dmtavt.fragpipe.Fragpipe;
import com.dmtavt.fragpipe.api.Bus;
import com.dmtavt.fragpipe.cmd.ProcessBuilderInfo;
import com.dmtavt.fragpipe.messages.MessageDeletePaths;
import com.dmtavt.fragpipe.messages.MessageKillAll;
import com.dmtavt.fragpipe.messages.MessagePrintToConsole;
import com.dmtavt.fragpipe.messages.MessageRunButtonEnabled;
import com.dmtavt.fragpipe.messages.MessageStartProcesses;
import com.github.chhh.utils.FileDelete;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessManager {
  private static final Logger log = LoggerFactory.getLogger(ProcessManager.class);

  private static final ProcessManager instance = new ProcessManager();
  private final Object lock = new Object();
  private final ConcurrentLinkedQueue<List<RunnableDescription>> taskGroups = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<CompletableFuture<?>> started = new ConcurrentLinkedQueue<>();

  private volatile CompletableFuture<Void> cf = CompletableFuture.completedFuture(null);
  private ExecutorService execSingle;
  private ExecutorService execMulti;

  private ProcessManager() {
    log.debug("Process manager private constructor called");
    init0();
    Bus.register(this);
  }

  public static ProcessManager get() {
    return instance;
  }

  public void init() {
    log.debug("Initializing Process Manager: init()");
    init0();
  }

  private ExecutorService newSingleExecutor() {
    return Executors.newFixedThreadPool(1);
  }

  private ExecutorService newMultiExecutor() {
    return Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
  }

  private void init0() {
    synchronized (lock) {
      taskGroups.clear();
      started.forEach(cf -> cf.cancel(true));
      started.clear();
      cf.cancel(true);
      cf = CompletableFuture.completedFuture(null);

      try {
        if (execSingle != null) {
          execSingle.shutdownNow();
          execSingle.awaitTermination(20, TimeUnit.SECONDS);
        }
      } catch (InterruptedException ex) {
        log.debug("Timed out waiting for sequential executor shutdown. This does not affect processing results.", ex);
      } finally {
        execSingle = newSingleExecutor();
      }

      try {
        if (execMulti != null) {
          execMulti.shutdownNow();
          execMulti.awaitTermination(20, TimeUnit.SECONDS);
        }
      } catch (InterruptedException ex) {
        log.debug("Timed out waiting for parallel executor shutdown. This does not affect processing results.", ex);
      } finally {
        execMulti = newMultiExecutor();
      }

    }
  }

  private void deleteTempFiles() {
    MessageDeletePaths m = Bus.getStickyEvent(MessageDeletePaths.class);
    if (m != null && !m.toDelete.isEmpty()) {
      for (Path path : m.toDelete) {
        try {
          if (Files.exists(path)) {
            log.debug("Deleting temp file/dir: {}", path);

            // try deleting every file a few times (up to 5 seconds)
            AtomicInteger retries = new AtomicInteger(0);
            AtomicBoolean isDeleted = new AtomicBoolean(false);
            while (!isDeleted.get() && retries.get() < 10) {
              retries.getAndIncrement();
              try {
                FileDelete.deleteFileOrFolder(path);
                isDeleted.set(true);
              } catch (FileSystemException e) {
                if (e.getMessage() != null && e.getMessage().toLowerCase()
                    .contains("being used by another process")) {
                  log.warn("Attempt #{} to delete file we need to delete is being used by another process: {}", retries.get(), path);
                  Thread.sleep(1000);
                }
              }
            }

          }
        } catch (IOException | InterruptedException e) {
          log.warn("Error while deleting temporary files", e); // log, but otherwise ignore
        }
      }
      Bus.postSticky(new MessageDeletePaths(Collections.emptySet()));
    }
  }

  private void stop() {
    synchronized (lock) {
      try {
        for (Future<?> proc : started) {
          proc.cancel(true);
        }
      } finally {
        // whatever happens, kill the old executor service, start a new one, clear queues
        init0();
      }
    }
  }

  @Subscribe(threadMode = ThreadMode.BACKGROUND)
  public void on(MessageStartProcesses m) {
    synchronized (lock) {
      if (m.runDescs.isEmpty()) {
        return;
      }
      stop();

      Iterator<RunnableDescription> it = m.runDescs.iterator();
      final List<RunnableDescription> group = new ArrayList<>();
      while (it.hasNext()) {
        RunnableDescription next = it.next();

        // are we starting a new group?
        if (!group.isEmpty()) {
          RunnableDescription last = group.get(group.size() - 1);
          if (last.parallelGroup.equals(next.parallelGroup)) {
            group.add(next);
            continue;
          } else {
            processGroup(group);
          }
        }
        // next is either a null/sequential group element or a new group element
        group.add(next);
        if (next.parallelGroup == null || next.parallelGroup.equals(ProcessBuilderInfo.GROUP_SEQUENTIAL)) {
          processGroup(group);
        }
      }

      if (taskGroups.isEmpty()) {
        log.error("No runnable groups found");
        return;
      }
      submit();

    } // END: sync

  }

//  private CompletableFuture<Void> submit() {
  private void submit() {
    synchronized (lock) {
      List<RunnableDescription> rds = taskGroups.poll();
      if (rds == null) {
        log.debug("No more groups to process, stopping");
        stop();
        return;
      }

      if (rds.size() == 1) {
        RunnableDescription rd = rds.get(0);
        log.debug("Submitting for serial execution: [{}] {}", rd.description.name,
            rd.description.command);
        cf = CompletableFuture.runAsync(rd.runnable, execSingle)
            .thenRunAsync(this::submit, execSingle);

      } else {
        String groupName = rds.stream().map(rd -> rd.parallelGroup).distinct()
            .collect(Collectors.joining(", "));
        String cmds = rds.stream().map(rd -> rd.description.command)
            .collect(Collectors.joining("\n\t"));
        log.debug("Submitting for parallel execution: [{}] {} commands:\n\t{}", groupName,
            rds.size(), cmds);
        List<CompletableFuture<Void>> cfs = new ArrayList<>();
        for (RunnableDescription rd : rds) {
          CompletableFuture<Void> f = CompletableFuture.runAsync(rd.runnable, execMulti);
          cfs.add(f);
        }
        final CompletableFuture<Void> f = CompletableFuture
            .allOf(cfs.toArray(new CompletableFuture[0]));
        cf = f.thenRunAsync(this::submit, execSingle);
      }
    }
  }

  private void processGroup(List<RunnableDescription> group) {
    final List<RunnableDescription> copy = new ArrayList<>(group);
    if (group.size() == 1) {
      RunnableDescription rd = copy.get(0);
      log.debug("Scheduling for serial execution: [{}] {}", rd.description.name, rd.description.command);
    } else {
      // group of several processes to be run in parallel
      String groupName = group.stream().map(rd -> rd.parallelGroup).distinct()
          .collect(Collectors.joining(", "));
      String cmds = group.stream().map(rd -> rd.description.command)
          .collect(Collectors.joining("\n\t"));
      log.debug("Scheduling for parallel execution: [{}] {} commands:\n\t{}", groupName,
          group.size(), cmds);
    }

    taskGroups.add(copy);
    group.clear();
  }

  @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
  public void on(MessageKillAll m) {
    long notStarted = taskGroups.stream().mapToInt(List::size).sum();
    String msg = String.format(
        "\n~~~~~~~~~~~~~~~~~~~~\nCancelling %d remaining tasks", notStarted);
    Bus.post(new MessagePrintToConsole(Fragpipe.COLOR_RED_DARKEST, msg, true));

    try {
      stop();
      deleteTempFiles(); // try deleting old temp files
    } finally {
      // after attempting to stop all previous tasks, re-enable run button
      Bus.post(new MessageRunButtonEnabled(true));
    }
  }

  @Subscribe(threadMode = ThreadMode.BACKGROUND)
  public void on(MessageDeletePaths m) {
    log.debug("Delete Paths Message updated: {}", m.toDelete.isEmpty() ? "empty set" : m.toDelete);
  }

  public static void addFilesToDelete(Collection<Path> toDelete) {
    if (toDelete == null || toDelete.isEmpty()) {
      return;
    }
    MessageDeletePaths m = Bus.getStickyEvent(MessageDeletePaths.class);
    HashSet<Path> s = m != null ? new HashSet<>(m.toDelete) : new HashSet<>();
    s.addAll(toDelete);
    Bus.postSticky(new MessageDeletePaths(s));
  }
}
