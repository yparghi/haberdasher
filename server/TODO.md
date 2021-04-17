# TODOs

## Small

- Update ExampleMain to set up a test HBase cluster, for easily testing the client.

- In `checkout()`, does DFS perform better than BFS due to locality?

- Org and repo id's: Use some small randomly generated ID instead of what's typed in (which can just be a display name)?
    - Then the small random ID would be used in the row keys.


## Big

- How can I take of advantage of ROW LOCALITY to group data together for faster reads/checkouts?
    - Should the file row key include the folder id, to group files in a folder together?
        - What about revisions to that folder that DON'T touch most of the files? Would that screw up the locality?
    - And how do renames affect this?
    - Would a monotonically increasing commit ID make this easier, by keeping related data (close together in history) close together lexicographically?

- Upgrade the JVM to whatever works with HBase.

- Look at Java modules, for stuff like the HBase code.

- Think about conflicts/merging:
    - Like, what if a file in your push has since been changed by someone else?
    - Does a changeset always set a base commit for a file? (Like, only push this changeset if this file is still at commit xyz.)
    - If so, then _how do you know a file's commit history?_ From the change history stored on the folder, like I proposed in my notes?
    - Use HBase **checkAndPut** for safe merging?
    - Does your change need to hold its "place in line" in a way?

- SSL/TLS for socket communications from client to server?
    - The server socket/communication config should be configurable from main?

- Encrypted storage?

- Compression in storage?

- Garbage collection / failed pushes: In the mark phase, save the data/results out to some giant file or a kind of temp table, to use in the sweep phase?

- Client extensions, by loading from the classpath?

