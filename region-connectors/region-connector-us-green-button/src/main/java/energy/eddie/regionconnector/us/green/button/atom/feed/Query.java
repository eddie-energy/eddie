package energy.eddie.regionconnector.us.green.button.atom.feed;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Query {
    private static final Logger LOGGER = LoggerFactory.getLogger(Query.class);
    private final SyndFeed feed;
    private final Unmarshaller unmarshaller;

    public Query(SyndFeed feed, Unmarshaller unmarshaller) {
        this.feed = feed;
        this.unmarshaller = unmarshaller;
    }

    @Nullable
    public static String intervalBlockSelfToUsagePointId(SyndEntry entry) {
        // UsagePoint self:    https://utilityapi.com/DataCustodian/espi/1_1/resource/Subscription/467189/UsagePoint/1669851
        // IntervalBlock self: https://utilityapi.com/DataCustodian/espi/1_1/resource/Subscription/467189/UsagePoint/1669851/MeterReading/1669851-1725408000-1725321600_kwh_1/IntervalBlock/000001
        for (var link : entry.getLinks()) {
            if (link.getRel().equals("self")) {
                var href = link.getHref();
                var splitted = href.split("/MeterReading", -1);
                if (splitted.length <= 1) {
                    LOGGER.warn("Found invalid self link on IntervalBlock: {}", entry);
                    return null;
                }
                return new File(URI.create(splitted[0]).getPath()).getName();
            }
        }
        return null;
    }

    public List<SyndEntry> findAllByTitle(String title) {
        var list = new ArrayList<SyndEntry>();
        for (var entry : feed.getEntries()) {
            if (Objects.equals(entry.getTitle(), title)) {
                list.add(entry);
            }
        }
        return Collections.unmodifiableList(list);
    }

    @Nullable
    public <T> T unmarshal(SyndEntry entry, Class<T> clazz) {
        if (entry.getContents().size() != 1) {
            return null;
        }
        var content = entry.getContents().getFirst().getValue().getBytes(StandardCharsets.UTF_8);
        try {
            var obj = unmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(content)), clazz);
            return obj.getValue();
        } catch (JAXBException e) {
            LOGGER.warn("Error unmarshalling {}", clazz.getCanonicalName(), e);
            return null;
        }
    }

    public Optional<SyndEntry> findFirstBySelfLinkAndTitle(String selfLink, String title) {
        for (var entry : findAllByTitle(title)) {
            if (entry.findRelatedLink("self").getHref().startsWith(selfLink)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public <T> Optional<T> findFirstBySelfLinkAndTitle(String selfLink, String title, Class<T> clazz) {
        return findFirstBySelfLinkAndTitle(selfLink, title)
                .map(res -> unmarshal(res, clazz));
    }
}
