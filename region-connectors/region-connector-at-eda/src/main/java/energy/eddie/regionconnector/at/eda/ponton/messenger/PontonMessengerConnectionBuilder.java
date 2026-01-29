// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import de.ponton.xp.adapter.api.ConnectionException;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.InboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

public class PontonMessengerConnectionBuilder {

    @Nullable
    private PontonXPAdapterConfiguration config;
    @Nullable
    private InboundMessageFactoryCollection inboundMessageFactoryCollection;
    @Nullable
    private OutboundMessageFactoryCollection outboundMessageFactoryCollection;
    @Nullable
    private MessengerHealth healthApi;
    @Nullable
    private MessengerMonitor messengerMonitor;

    public PontonMessengerConnectionBuilder withConfig(PontonXPAdapterConfiguration config) {
        this.config = config;
        return this;
    }

    public PontonMessengerConnectionBuilder withInboundMessageFactoryCollection(
            InboundMessageFactoryCollection inboundMessageFactoryCollection
    ) {
        this.inboundMessageFactoryCollection = inboundMessageFactoryCollection;
        return this;
    }

    public PontonMessengerConnectionBuilder withOutboundMessageFactoryCollection(
            OutboundMessageFactoryCollection outboundMessageFactoryCollection
    ) {
        this.outboundMessageFactoryCollection = outboundMessageFactoryCollection;
        return this;
    }

    public PontonMessengerConnectionBuilder withHealthApi(MessengerHealth healthApi) {
        this.healthApi = healthApi;
        return this;
    }

    public PontonMessengerConnectionBuilder withMessengerMonitor(MessengerMonitor messengerMonitor) {
        this.messengerMonitor = messengerMonitor;
        return this;
    }

    public PontonMessengerConnection build() throws ConnectionException, IOException {
        requireNonNull(config, "config must be set");
        requireNonNull(inboundMessageFactoryCollection, "inboundMessageFactoryCollection must be set");
        requireNonNull(outboundMessageFactoryCollection, "outboundMessageFactoryCollection must be set");
        requireNonNull(healthApi, "healthApi must be set");
        requireNonNull(messengerMonitor, "messengerMonitor must be set");

        final File workFolder = new File(config.workFolder());

        if (!workFolder.exists()) {
            throw new IOException("Work folder does not exist: " + workFolder.getAbsolutePath());
        }

        return new PontonMessengerConnectionImpl(
                config,
                workFolder,
                inboundMessageFactoryCollection,
                outboundMessageFactoryCollection,
                healthApi,
                messengerMonitor
        );
    }
}
