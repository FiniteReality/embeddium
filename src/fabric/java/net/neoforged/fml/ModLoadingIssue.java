package net.neoforged.fml;

public record ModLoadingIssue(String message) {
    public static ModLoadingIssue error(String message) {
        return new ModLoadingIssue(message);
    }
}
