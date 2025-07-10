package br.com.arthivia.imageCompress;

import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

public class Watch {
    private TreatImage treatImage = null;

    public Watch() {
        treatImage = new TreatImage();
    }

    public void watching() throws IOException, InterruptedException {
        Path inputDir = Paths.get("./input");
        if (!Files.exists(inputDir)) {
            Files.createDirectories(inputDir);
        }

        WatchService watchService = FileSystems.getDefault().newWatchService();
        inputDir.register(watchService, ENTRY_CREATE);

        while (true) {
            WatchKey watchKey = watchService.take();
            for (WatchEvent<?> event : watchKey.pollEvents()) {
                WatchEvent.Kind<?> type = event.kind();
                if (type == ENTRY_CREATE) {
                    Path inputFile = (Path) event.context(); // <- retorna nome do arquivo
                    String fileName = inputFile.toString().toLowerCase();
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                        treatImage.compressImage(inputDir, fileName);
                    }
                }
            }
            boolean valid = watchKey.reset();
            if (!valid) {
                break;
            }
        }
    }
}
