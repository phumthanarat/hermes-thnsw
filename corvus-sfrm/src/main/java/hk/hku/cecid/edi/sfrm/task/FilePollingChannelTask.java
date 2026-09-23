package hk.hku.cecid.edi.sfrm.task;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import hk.hku.cecid.edi.sfrm.com.PackagedPayloadsRepository;
import hk.hku.cecid.edi.sfrm.dao.FilePollingChannelDVO;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.piazza.commons.module.ActiveTaskAdaptor;

/**
 * FilePollingChannelTask scans one configured watch_path for ready ".sfrm"
 * files and moves each one into the default outgoing-payload-repository
 * directory, where the existing OutgoingPayloadsCollector picks it up and
 * sends it exactly as if it had been dropped there directly. This lets
 * several independent directories feed the same, already-proven send
 * pipeline instead of teaching that pipeline about multiple locations.
 *
 * @author Hermes2+ (SFRM multi-path file polling)
 */
public class FilePollingChannelTask extends ActiveTaskAdaptor {

    private static final int DEFAULT_MAX_FILES_PER_POLL = 10;

    private final FilePollingChannelDVO channel;

    public FilePollingChannelTask(FilePollingChannelDVO channel) {
        this.channel = channel;
    }

    public void execute() throws Exception {
        File watchDir = new File(channel.getWatchPath());
        if (!watchDir.isDirectory()) {
            SFRMProcessor.getInstance().getLogger().warn(
                    "File polling channel '" + channel.getName() + "' watch path does not exist or is not a directory: "
                            + channel.getWatchPath());
            return;
        }

        File[] files = watchDir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        PackagedPayloadsRepository outgoingRepo = (PackagedPayloadsRepository)
                SFRMProcessor.getInstance().getSystemModule().getComponent("outgoing-payload-repository");
        File targetDir = outgoingRepo.getRepository();

        int maxFiles = channel.getMaxFilesPerPoll();
        if (maxFiles <= 0) {
            maxFiles = DEFAULT_MAX_FILES_PER_POLL;
        }

        int moved = 0;
        for (int i = 0; i < files.length && moved < maxFiles; i++) {
            File f = files[i];
            if (!f.isFile() || !isReady(f.getName())) {
                continue;
            }

            Path target = new File(targetDir, f.getName()).toPath();
            try {
                Files.move(f.toPath(), target, StandardCopyOption.ATOMIC_MOVE);
                moved++;
                SFRMProcessor.getInstance().getLogger().info(
                        "File polling channel '" + channel.getName() + "' funneled " + f.getName()
                                + " into the outgoing repository");
            } catch (java.nio.file.FileAlreadyExistsException e) {
                // Same-named file already queued from another channel/drop; leave it for next poll.
            } catch (java.nio.file.NoSuchFileException e) {
                // File was removed/renamed between listing and moving; skip.
            } catch (java.io.IOException e) {
                // Different filesystem etc -- fall back to copy + delete.
                try {
                    Files.copy(f.toPath(), target, StandardCopyOption.COPY_ATTRIBUTES);
                    Files.delete(f.toPath());
                    moved++;
                } catch (Exception copyEx) {
                    SFRMProcessor.getInstance().getLogger().error(
                            "File polling channel '" + channel.getName() + "' failed to funnel " + f.getName(), copyEx);
                }
            }
        }
    }

    public void onFailure(Throwable e) {
        SFRMProcessor.getInstance().getLogger().error(
                "File polling channel '" + channel.getName() + "' task failed", e);
    }

    private boolean isReady(String filename) {
        if (!filename.endsWith(".sfrm")) {
            return false;
        }
        char first = filename.charAt(0);
        return first != '~' && first != '.' && first != '#' && first != '%';
    }
}
