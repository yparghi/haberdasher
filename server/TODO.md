# TODOs


## App v0

- Branch commit ids as increments
    - Make sure writing/pushing to a branch uses checkAndMutate

- Revisit how 'log' would work, see notes. Flesh them out better...



## Small

- Row key separators should be `:` NOT `_` since `_` can appear in folder names?

- Update ExampleMain to set up a test HBase cluster, for easily testing the client.

- In `checkout()`, does DFS perform better than BFS due to locality?

- Common `*LocalCrawler` in the client? It could use composition with some `FolderInfoSupplier` interface or something, for use with FolderListing, GitTree, or Path.

- Org and repo id's: Use some small randomly generated ID instead of what's typed in (which can just be a display name)?
    - Then the small random ID would be used in the row keys.
    - **Ditto for branch names!**

- Content addressing/hashing for file ids?
    - What would it gain us? Are purely random-access row keys in the Files table okay?
        - Would it be better to keep related rows together, as with folders?



## Big

- How can I take of advantage of ROW LOCALITY to group data together for faster reads/checkouts?
    - Should the file row key include the folder id, to group files in a folder together?
        - What about revisions to that folder that DON'T touch most of the files? Would that screw up the locality?
    - And how do renames affect this?
    - Would a monotonically increasing commit ID make this easier, by keeping related data (close together in history) close together lexicographically?

- Upgrade the JVM to whatever works with HBase.

- Look at Java modules, for stuff like the HBase code.

- SSL/TLS for socket communications from client to server?
    - The server socket/communication config should be configurable from main?

- Encrypted storage?

- Compression in storage?
    - e.g. ContentsType `FULL_GZIP` or something?

- Garbage collection / failed pushes: In the mark phase, save the data/results out to some giant file or a kind of temp table, to use in the sweep phase?
    - Or, idea: every object gets marked with some kind of transaction tag, that gets removed when it's confirmed the transaction has committed/landed/finished -- or is this too much overhead?

- Client extensions, by loading from the classpath?

- Tests to make sure my row keys conform to HBase's lexicographical ordering the way I need them to.

- How would rebase work?

- Implement revert.
    - Something with switching left and right in the crawling?

- Change the client/local DB to match the server, so that e.g. folders are keyed by path?

- For the "squash vs. linear merges?" question, consider saving the branch history separately, so that main has the squashed commit, but you can separately "dig in" to that branches history and its granular as-it-was-developed commits, if you want to.

- Some "inspect" command in the client that just lets you look at objects, given an id?

- Bug about old merge locks, see the long comment in HBaseRawHelper.java.

- `hd inspect server/local folder <path>` ???

