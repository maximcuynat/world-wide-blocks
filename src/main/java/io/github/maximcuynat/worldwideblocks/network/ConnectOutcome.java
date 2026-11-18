package io.github.maximcuynat.worldwideblocks.network;

import java.util.Optional;

/** Result of {@link NetworkGraph#connect}: the created link is present only on success. */
public record ConnectOutcome(ConnectResult result, Optional<Link> link) {

    static ConnectOutcome failure(ConnectResult result) {
        return new ConnectOutcome(result, Optional.empty());
    }

    static ConnectOutcome success(Link link) {
        return new ConnectOutcome(ConnectResult.OK, Optional.of(link));
    }

    public boolean isSuccess() {
        return this.result == ConnectResult.OK;
    }
}
