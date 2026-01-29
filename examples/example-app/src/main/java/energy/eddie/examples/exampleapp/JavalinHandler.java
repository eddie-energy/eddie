// SPDX-FileCopyrightText: 2023 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.examples.exampleapp;

import io.javalin.Javalin;

public interface JavalinHandler {
    void register(Javalin app);
}
