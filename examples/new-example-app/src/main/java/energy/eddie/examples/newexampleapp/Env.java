// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.examples.newexampleapp;

public enum Env {
    JDBC_URL,
    JDBC_USER,
    JDBC_PASSWORD;

    public String get() {
        return System.getenv(this.name());
    }
}