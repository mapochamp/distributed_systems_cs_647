1. We don't actually apply any entries because we're not actually doing any procedure calls to execute for the client.
2. Even though the servers can handle multiple requests at once i.e a list of consequtive RPC's, we
have the clients only send a single entry at a time (a single int to represent the request) for simplicity
3. we only send around 1 entry at a time for simplicity

Bugs:
1. If the clients stop sending requests, even though there aren't timeouts, the term and commit indexes
get written wrong and you get an infinite election loop where everyone's terms are "outdated" relative to
the other and it never ends.