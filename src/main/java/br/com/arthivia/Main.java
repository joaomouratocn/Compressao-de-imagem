package br.com.arthivia;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        watching();
    }

    private static void watching() throws IOException, InterruptedException {
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
                        compressImage(inputDir, fileName);
                    }
                }
            }
            boolean valid = watchKey.reset();
            if (!valid) {
                break;
            }
        }
    }

    private static void waitCompleteFile(Path path) throws IOException {
        long prevSize = -1;
        long currSize = Files.size(path);
        int attemps = 0;

        while (currSize != prevSize && attemps < 10) {
            prevSize = currSize;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Thread interrompida.");
            }
            currSize = Files.size(path);
            attemps++;
        }

        if (attemps == 10) {
            throw new IOException("Arquivo não estabilizou. Pode estar corrompido ou bloqueado.");
        }
    }

    private static void compressImage(Path inputDir, String fileName) {
        try {
            Path originalPath = inputDir.resolve(fileName);
            waitCompleteFile(originalPath);
            BufferedImage img = ImageIO.read(originalPath.toFile());


            Path outputDir = Paths.get("./output/");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }


            Path outputPath = outputDir.resolve(fileName);
            File outputFile = outputPath.toFile();


            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(fileOutputStream);

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            writer.setOutput(imageOutputStream);

            ImageWriteParam imageWriteParam = writer.getDefaultWriteParam();
            imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            imageWriteParam.setCompressionQuality(0.3f);

            writer.write(null, new IIOImage(img, null, null), imageWriteParam);


            imageOutputStream.close();
            fileOutputStream.close();
            writer.dispose();

            notify(fileName);
            Files.delete(originalPath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void notify(String fileName) {
        try {
            String imageTempPath = null;

            InputStream imageUrlStream = Main.class.getClassLoader().getResourceAsStream("image_notification.png");
            if (imageUrlStream == null) {
                System.err.println("Erro: 'image_notification.png' não encontrado no classpath.");
                return;
            }

            File tempImageFile = File.createTempFile("notification_app_logo_", ".png");
            tempImageFile.deleteOnExit();
            imageTempPath = tempImageFile.getAbsolutePath();

            Files.copy(imageUrlStream, tempImageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            ProcessBuilder pb = getProcessBuilder(fileName, imageTempPath);
            pb.start();

        } catch (Exception e) {
            System.out.printf(e.getMessage());
        }
    }

    private static ProcessBuilder getProcessBuilder(String fileName, String path) {
        String script = String.format("powershell.exe -ExecutionPolicy Bypass -Command \"Import-Module BurntToast; " + "Remove-BTNotification; " + "New-BurntToastNotification -Text 'Sucesso!', 'O arquivo ''%s'' está pronto para ser importado.' -AppLogo '%s'\"", fileName, path);

        return new ProcessBuilder("cmd.exe", "/c", script);
    }
}