package org.embeddedt.embeddium.gradle.versioning;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ProjectVersioner {
    public static String computeVersion(File projectDir, Map<String, ?> projectProperties) {
        String modVersion = projectProperties.get("mod_version").toString();
        String minecraftVersion = projectProperties.get("minecraft_version").toString();
        boolean isReleaseBuild = projectProperties.containsKey("build.release");
        if (isReleaseBuild) {
            return "%s+mc%s".formatted(modVersion, minecraftVersion);
        } else {
            boolean isDirty;
            int betaVersion;
            try {
                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                try(Repository repository = builder.setGitDir(new File(projectDir,".git")).readEnvironment().findGitDir().build()) {
                    try(RevWalk walk = new RevWalk(repository)) {
                        var baseCommit = walk.parseCommit(ObjectId.fromString(projectProperties.get("base_mc_version_commit").toString()));
                        var git = new Git(repository);
                        var headCommit = git.log().setMaxCount(1).call().iterator().next();
                        var iterator = git.log().addRange(baseCommit, headCommit).call().iterator();
                        isDirty = !git.status().call().isClean();
                        int count = 0;
                        while(iterator.hasNext()) {
                            count++;
                            iterator.next();
                        }
                        betaVersion = count;
                    }
                }
            } catch(IOException | GitAPIException e) {
                e.printStackTrace();
                isDirty = false;
                betaVersion = 9999;
            }

            if(betaVersion > 0 || isDirty) {
                String[] versionComponents = modVersion.split("\\.");
                if(versionComponents.length >= 3) {
                    // Increase patch version
                    try {
                        versionComponents[2] = String.valueOf(Integer.parseInt(versionComponents[2]) + 1);
                        modVersion = String.join(".", versionComponents);
                    } catch(NumberFormatException ignored) {}
                }
            }

            return "%s-beta.%s%s+mc%s".formatted(modVersion, betaVersion, isDirty ? "-dirty" : "", minecraftVersion);
        }
    }
}
