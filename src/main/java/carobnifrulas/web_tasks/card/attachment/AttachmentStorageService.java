package carobnifrulas.web_tasks.card.attachment;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

@Service
public class AttachmentStorageService {

    private final Path rootPath;

    public AttachmentStorageService(AttachmentStorageProperties properties) {
        this.rootPath = Paths.get(properties.getStoragePath()).toAbsolutePath().normalize();
        init();
    }

    private void init() {
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Ne mogu kreirati root attachments folder: " + rootPath, e);
        }
    }

    public StoredAttachment save(Long cardId, String originalFilename, String contentType, InputStream inputStream) {
        Objects.requireNonNull(cardId, "cardId ne smije biti null");
        Objects.requireNonNull(originalFilename, "originalFilename ne smije biti null");
        Objects.requireNonNull(inputStream, "inputStream ne smije biti null");

        String cleanedOriginalName = StringUtils.cleanPath(originalFilename);
        String extension = extractExtension(cleanedOriginalName);
        String storedFilename = UUID.randomUUID() + extension;

        Path cardFolder = rootPath.resolve("cards").resolve(String.valueOf(cardId)).normalize();
        Path targetFile = cardFolder.resolve(storedFilename).normalize();

        if (!targetFile.startsWith(cardFolder)) {
            throw new IllegalStateException("Neispravna putanja za attachment.");
        }

        try {
            Files.createDirectories(cardFolder);
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            long sizeBytes = Files.size(targetFile);

            return new StoredAttachment(
                    cleanedOriginalName,
                    storedFilename,
                    contentType,
                    sizeBytes,
                    targetFile
            );
        } catch (IOException e) {
            throw new IllegalStateException("Greška pri snimanju attachment fajla: " + cleanedOriginalName, e);
        }
    }

    public Path loadAsPath(Long cardId, String storedFilename) {
        Objects.requireNonNull(cardId, "cardId ne smije biti null");
        Objects.requireNonNull(storedFilename, "storedFilename ne smije biti null");

        Path path = rootPath.resolve("cards")
                .resolve(String.valueOf(cardId))
                .resolve(storedFilename)
                .normalize();

        if (!path.startsWith(rootPath)) {
            throw new IllegalStateException("Neispravna putanja za attachment.");
        }

        return path;
    }

    public void delete(Long cardId, String storedFilename) {
        Path path = loadAsPath(cardId, storedFilename);

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("Greška pri brisanju attachment fajla: " + storedFilename, e);
        }
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot);
    }

    public record StoredAttachment(
            String originalFilename,
            String storedFilename,
            String contentType,
            long sizeBytes,
            Path fullPath
    ) {
    }
}