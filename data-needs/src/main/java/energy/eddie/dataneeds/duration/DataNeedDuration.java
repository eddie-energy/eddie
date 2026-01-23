// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.duration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.*;

@Entity
@Table(schema = "data_needs")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AbsoluteDuration.class, name = AbsoluteDuration.DISCRIMINATOR_VALUE),
        @JsonSubTypes.Type(value = RelativeDuration.class, name = RelativeDuration.DISCRIMINATOR_VALUE)
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class DataNeedDuration {
    @Id
    @Column(name = "data_need_id")
    @JsonIgnore
    private String dataNeedId;

    @SuppressWarnings("NullAway.Init")
    protected DataNeedDuration() {
    }

    /**
     * Returns the ID of the data need with which this {@link DataNeedDuration} is associated.
     *
     * @return the ID of the data need.
     */
    public String dataNeedId() {
        return dataNeedId;
    }

    /**
     * Sets the id of the data need with which this {@link DataNeedDuration} should be associated.
     *
     * @param dataNeedId new data need ID.,
     */
    public void setDataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
    }
}
