package br.com.arthivia.imageCompress;

import java.io.*;
import java.nio.file.*;

public class Notifier {

    /** Caminho do ícone em disco (copiado 1× e reutilizado). */
    private String cachedIconPath;

    /**
     * Exibe a notificação “Sucesso!” dizendo que o arquivo <fileName> está pronto.
     */
    public void notify(String fileName) {
        try {
            if (cachedIconPath == null) {
                cachedIconPath = extractIconOnce();
            }

            ProcessBuilder pb = buildPowerShell(fileName, cachedIconPath);
            Process proc = pb.start();

            // Opcional: log de saída/erro do PowerShell
            logStreamsAsync(proc);

        } catch (Exception ex) {
            System.err.println("[Notifier] Falha ao notificar: " + ex.getMessage());
        }
    }

    /** Copia a imagem do classpath para um arquivo temporário (feito só na 1ª vez). */
    private String extractIconOnce() throws IOException {
        try (InputStream in = Notifier.class.getResourceAsStream("/image_notification.png")) {
            if (in == null) throw new FileNotFoundException("/image_notification.png" + " não encontrado no classpath");

            Path tmp = Files.createTempFile("notification_app_logo_", ".png");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();   // será removido quando a JVM encerrar
            return tmp.toAbsolutePath().toString();
        }
    }

    /** Monta o ProcessBuilder chamando PowerShell diretamente. */
    private ProcessBuilder buildPowerShell(String fileName, String iconPath) {
        // Script em PowerShell; use graves (`) para escapar aspas simples dentro de string simples.
        String script = String.format(
                "Import-Module BurntToast;" +
                        "Remove-BTNotification;" +
                        "New-BurntToastNotification -Text 'Sucesso!', 'O arquivo `%s está pronto para ser importado.' -AppLogo '%s'",
                fileName.replace("'", "''"),  // escapa aspas simples em fileName
                iconPath.replace("'", "''")
        );

        return new ProcessBuilder(
                "powershell.exe",
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy", "Bypass",
                "-Command", script
        );
    }

    /** Lê saída/erro do processo de forma assíncrona (evita bloquear caso algo escreva muito). */
    private void logStreamsAsync(Process proc) {
        new Thread(() -> copyStream(proc.getInputStream(), System.out)).start();
        new Thread(() -> copyStream(proc.getErrorStream(), System.err)).start();
    }

    private void copyStream(InputStream in, PrintStream out) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            br.lines().forEach(out::println);
        } catch (IOException ignored) { }
    }
}
