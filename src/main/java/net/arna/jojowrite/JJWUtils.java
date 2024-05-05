package net.arna.jojowrite;

public class JJWUtils {
    public enum FileType {
        ROM,
        OVERWRITE,
        ASSEMBLY,
        PATCH
    }

    public static final String OVERWRITE_FILE_EXTENSION = ".overwrite";
    public static final String ASSEMBLY_FILE_EXTENSION = ".x16asm";
    public static final String PATCH_FILE_EXTENSION = ".patch";
}
