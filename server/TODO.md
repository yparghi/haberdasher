# TODOs

- Think about conflicts/merging:
    - Like, what if a file in your push has since been changed by someone else?
    - Does a changeset always set a base commit for a file? (Like, only push this changeset if this file is still at commit xyz.)
    - If so, then _how do you know a file's commit history?_ From the change history stored on the folder, like I proposed in my notes?
    - Use HBase **checkAndPut** for safe merging?

- SSL/TLS for socket communications from client to server?
    - The server socket/communication config should be configurable from main?


