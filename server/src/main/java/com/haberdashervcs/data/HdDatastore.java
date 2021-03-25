package com.haberdashervcs.data;

import com.haberdashervcs.data.change.ApplyChangesetResult;
import com.haberdashervcs.data.change.Changeset;


public interface HdDatastore {

    ApplyChangesetResult applyChangeset(Changeset changeset);
}
