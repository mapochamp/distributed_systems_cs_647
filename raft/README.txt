1. We don't actually apply any entries because we're not actually doing any procedure calls to execute for the client.
2. Even though the servers can handle multiple requests at once i.e a list of consequtive RPC's, we
have the clients only send a single entry at a time (a single int to represent the request) for simplicity
3. we only send around 1 entry at a time for simplicity

Bugs:
1. If the clients stop sending requests, even though there aren't timeouts, the request
 gets duplicate replicated and you get an infinite election loop where everyone's terms are "outdated" relative to
the other or log lengths are out of sync and it never ends. This happens because when an election happens after
a leader appends client requests and the followers have already replicated the log. The new candidate then starts
writing replicating its log again repeatedly without end.


2. No server shutdown command
3. No client RPC commit confirmation message
4. [ this happens whether we reset nextIndex as leader or not ] Leaders double replicate entries (not sure if its when it becomes leader or every time we
send an appendEntries RPC) TODO: look into whether we call appendEntries consequtively when
we become leader
5. Somteimtes when there is a single entry that gets appended to a leaders log and everyone else's is empty and an election happens,
because the majority don't have anything in their log, a new candidate will win the election. The new leader does not
truncate the machine with the entry in this case.
6. [ no next vote reset ] Sometimes when an entry gets appended to a leader, the leader sends an appendEntries RPC but the follower doesn't
append it. From there after, even if heartbeats or reelections happen, the follower will never be caught up.
7. Race condition where a leader will send a heartbeat and follower will receive it after starting an election. This then
causes the old leader that sent a heartbeat to decrement the nextIndex and resend another appendEntries request. This duplicates
the entry they had already committed.
8. Sometimes when everybody is caught up and a follower who has never been leader before becomes leader, it will duplicate
replicate. I think this is because nextIndex is 0 and the appendEntries prevIndex / lastApplied is not being checked or sent correctly
9. Sometimes a client request gets infinitely redirected to a server that is a leader but the retry from the client happens right after an election
and it loops like that forever.