package com.haberdashervcs.common.change;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.haberdashervcs.common.objects.FolderListing;
import com.haberdashervcs.common.objects.FolderWithPath;

import static com.google.common.base.Preconditions.checkNotNull;


public final class Changeset {

    public static final class Builder {

        private final ArrayList<FolderWithPath> changedFolders;
        private final ArrayList<AddChange> addChanges;
        private final ArrayList<DeleteChange> deleteChanges;
        private final ArrayList<ModifyChange> modifyChanges;
        private final ArrayList<RenameChange> renameChanges;

        private Builder() {
            changedFolders = new ArrayList<>();
            addChanges = new ArrayList<>();
            deleteChanges = new ArrayList<>();
            modifyChanges = new ArrayList<>();
            renameChanges = new ArrayList<>();
        }

        public Builder withFolderAndPath(String path, FolderListing listing) {
            changedFolders.add(FolderWithPath.forPathAndListing(path, listing));
            return this;
        }

        public Builder withAddChange(AddChange addChange) {
            addChanges.add(addChange);
            return this;
        }

        public Builder withDeleteChange(DeleteChange deleteChange) {
            deleteChanges.add(deleteChange);
            return this;
        }

        public Builder withModifyChange(ModifyChange modifyChange) {
            modifyChanges.add(modifyChange);
            return this;
        }

        public Builder withRenameChange(RenameChange renameChange) {
            renameChanges.add(renameChange);
            return this;
        }

        public Changeset build() {
            return new Changeset(
                    changedFolders, addChanges, deleteChanges, modifyChanges, renameChanges);
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    private final ImmutableList<FolderWithPath> changedFolders;
    private final ImmutableList<AddChange> addChanges;
    private final ImmutableList<DeleteChange> deleteChanges;
    private final ImmutableList<ModifyChange> modifyChanges;
    private final ImmutableList<RenameChange> renameChanges;

    private final String proposedCommitId;
    private final String proposedRootFolderId;

    private Changeset(
            List<FolderWithPath> changedFolders,
            List<AddChange> addChanges,
            List<DeleteChange> deleteChanges,
            List<ModifyChange> modifyChanges,
            List<RenameChange> renameChanges) {
        this.changedFolders = ImmutableList.copyOf(checkNotNull(changedFolders));
        this.addChanges = ImmutableList.copyOf(checkNotNull(addChanges));
        this.deleteChanges = ImmutableList.copyOf(checkNotNull(deleteChanges));
        this.modifyChanges = ImmutableList.copyOf(checkNotNull(modifyChanges));
        this.renameChanges = ImmutableList.copyOf(checkNotNull(renameChanges));

        // TODO where *should* these id's come from?
        this.proposedCommitId = UUID.randomUUID().toString();
        this.proposedRootFolderId = UUID.randomUUID().toString();
    }

    public List<AddChange> getAddChanges() {
        return addChanges;
    }

    public List<DeleteChange> getDeleteChanges() {
        return deleteChanges;
    }

    public List<ModifyChange> getModifyChanges() {
        return modifyChanges;
    }

    public List<RenameChange> getRenameChanges() {
        return renameChanges;
    }

    // TODO some parsing or checking that makes sure when a folder is changed, all of its parent folders on the path
    // have change entries also?
    public List<FolderWithPath> getChangedFolders() {
        return changedFolders;
    }

    public String getProposedCommitId() {
        return proposedCommitId;
    }

    public String getProposedRootFolderId() {
        return proposedRootFolderId;
    }
}
