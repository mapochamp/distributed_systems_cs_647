Author: Tajung Jang
Date: 05/31/23

Instructions:


Part 1:
    - I keep 2 states: `unstableState` and `stableState` that both get recomputed everytime there is a new entry
    in the log by `recomputeState` method. It will recompute state up to either the latest entry or up to the commitIndex
    based on the boolean `isStable` passed.
    - The appropriate state is returned based on the message type
    - If a follower is sent a ClientReadStableRequest, it will redirect the client to the leader
    - if the log is empty, the client will know this but reading the unchanged state of the server's state machine:
    aka the # of tickets the server is initialized with.

Part 2:
    - A client will never ask for more tickets than it reads. The server will check to make sure that the # of tickets
    requested does not exceed the number of unstable and stable tickets left.