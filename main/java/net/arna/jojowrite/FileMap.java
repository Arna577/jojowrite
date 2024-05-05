package net.arna.jojowrite;

import net.arna.jojowrite.JJWUtils.FileType;

import java.io.File;
import java.util.HashMap;

public class FileMap extends HashMap<FileType, File> {
    private final JoJoWriteController controller;
    public FileMap(JoJoWriteController controller) {
        super(FileType.values().length);
        this.controller = controller;
    }

    @Override
    public File put(FileType key, File value) {
        File lastFile = super.put(key, value);
        controller.updateSelectedFileDisplay();
        return lastFile;
    }
}
