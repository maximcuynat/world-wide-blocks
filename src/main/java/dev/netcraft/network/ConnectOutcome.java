package dev.netcraft.network;

import java.util.Optional;

/** Résultat de {@link NetworkGraph#connect} : le lien créé n'est présent qu'en cas de succès. */
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
