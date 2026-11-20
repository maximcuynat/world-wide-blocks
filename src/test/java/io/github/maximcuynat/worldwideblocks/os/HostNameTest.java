package io.github.maximcuynat.worldwideblocks.os;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.maximcuynat.worldwideblocks.network.protocol.MacAddress;

class HostNameTest {
    private static final MacAddress MAC = MacAddress.parse("02:5e:11:a0:3f:2a").orElseThrow();

    @Test
    void theTypeThenTheLastFourHexDigitsOfTheMac() {
        assertEquals("laptop-3f2a", HostName.of("worldwideblocks:laptop", MAC));
        assertEquals("tower-3f2a", HostName.of("worldwideblocks:pc_tower", MAC));
        assertEquals("internet-box-3f2a", HostName.of("worldwideblocks:internet_box", MAC));
    }

    @Test
    void leadingZerosAreKept() {
        assertEquals("tower-0007", HostName.of("worldwideblocks:pc_tower", MacAddress.parse("02:00:00:00:00:07").orElseThrow()));
    }
}
