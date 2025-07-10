package br.com.arthivia.imageCompress;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TreatImage {
    private Notifier notifier;

    public TreatImage() {
        notifier = new Notifier();
    }

    public void compressImage(Path inputDir, String fileName) {
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

            notifier.notify(fileName);
            Files.delete(originalPath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitCompleteFile(Path path) throws IOException {
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
}
