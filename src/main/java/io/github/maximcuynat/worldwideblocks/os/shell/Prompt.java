package io.github.maximcuynat.worldwideblocks.os.shell;

import java.util.Objects;

/**
 * The prompt of the shell, as data of the machine: the client draws it, {@code user@host} in green and the directory
 * in blue. The colours follow the prompt of a Debian 13 account ({@code PS1} of {@code /etc/skel/.bashrc}: the user
 * and host in bold green {@code 01;32}, the directory in bold blue {@code 01;34}), seen on 2026-11-20
 * ({@code src/test/resources/linux-real-outputs.txt}, sections "bash").
 *
 * @param user      the account, {@code user} until the operating system has accounts
 * @param host      the host name of the machine
 * @param directory the current directory as bash writes it, {@code ~} for the home directory
 */
public record Prompt(String user, String host, String directory) {
    /** The account of the computers until the operating system lots. */
    public static final String USER = "user";
    /** The home directory: the only directory until the computers have files. */
    public static final String HOME = "~";

    public Prompt {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(directory, "directory");
    }

    /** The prompt as bash writes it, colours aside: {@code user@laptop-3f2a:~$ }, its trailing space included. */
    public String text() {
        return this.user + "@" + this.host + ":" + this.directory + "$ ";
    }
}
