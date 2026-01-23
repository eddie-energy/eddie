// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.examples.exampleapp;

public enum Env {
    JDBC_URL,
    JDBC_USER,
    JDBC_PASSWORD,
    EDDIE_PUBLIC_URL,
    PUBLIC_CONTEXT_PATH,
    KAFKA_BOOTSTRAP_SERVERS;

    public String get() {
        return System.getenv(this.name());
    }
}
