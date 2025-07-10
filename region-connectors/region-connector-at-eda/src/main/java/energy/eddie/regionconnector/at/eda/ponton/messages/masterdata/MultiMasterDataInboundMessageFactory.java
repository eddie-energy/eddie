package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata;

import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.XmlMappingException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;

public class MultiMasterDataInboundMessageFactory implements EdaMasterDataInboundMessageFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiMasterDataInboundMessageFactory.class);
    private final Set<EdaMasterDataInboundMessageFactory> factories;

    public MultiMasterDataInboundMessageFactory(Set<EdaMasterDataInboundMessageFactory> factories) {this.factories = factories;}

    @Override
    public EdaMasterData parseInputStream(InputStream inputStream) {
        var is = new UnclosableBufferedInputStream(inputStream);
        for (var factory : factories) {
            try {
                var res = factory.parseInputStream(is);
                is.forceClose();
                return res;
            } catch (Exception e) {
                LOGGER.info("Failed to parse master data from input stream, retrying...", e);
            }
        }
        is.forceClose();
        LOGGER.warn("Failed to parse master data from input stream.");
        throw new XmlMappingException("Unable to parse master data") {};
    }

    @Override
    public boolean isActive(LocalDate date) {
        return false;
    }

    private static class UnclosableBufferedInputStream extends BufferedInputStream {
        public UnclosableBufferedInputStream(InputStream in) {
            super(in);
            super.mark(Integer.MAX_VALUE);
        }

        @Override
        public void close() throws IOException {
            super.reset();
        }

        public void forceClose() {
            try {
                super.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close input stream", e);
            }
        }
    }
}
