package org.embeddedt.embeddium.taint.incompats;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import javax.annotation.Nullable;
import java.util.Optional;

public interface ModDeclaration {
    boolean matches();

    class Single implements ModDeclaration {
        private final String modId;
        private final String friendlyName;
        private final VersionRange versionRange;

        public Single(String modId, String friendlyName) {
            this(modId, friendlyName, null);
        }

        public Single(String modId, String friendlyName, @Nullable String versionRange) {
            this.modId = modId;
            this.friendlyName = friendlyName;
            try {
                this.versionRange = versionRange == null ? null : VersionRange.createFromVersionSpec(versionRange);
            } catch(InvalidVersionSpecificationException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public boolean matches() {
            Optional<? extends ModContainer> modContainerOpt = ModList.get().getModContainerById(modId);
            //noinspection OptionalIsPresent
            if(!modContainerOpt.isPresent()) {
                return false; // If the mod is not installed, no problem
            }
            return this.versionRange == null || this.versionRange.containsVersion(modContainerOpt.get().getModInfo().getVersion());
        }

        @Override
        public String toString() {
            if(this.versionRange == null)
                return this.friendlyName;
            else {
                return this.friendlyName + "(" + this.versionRange + ")";
            }
        }
    }

    class Or implements ModDeclaration {
        private final ModDeclaration left, right;

        public Or(ModDeclaration left, ModDeclaration right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches() {
            return left.matches() || right.matches();
        }

        @Override
        public String toString() {
            return left.toString() + " or " + right.toString();
        }
    }

    class And implements ModDeclaration {
        private final ModDeclaration left, right;

        public And(ModDeclaration left, ModDeclaration right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches() {
            return left.matches() && right.matches();
        }

        @Override
        public String toString() {
            return left.toString() + " and " + right.toString();
        }
    }
}
