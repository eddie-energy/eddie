package energy.eddie.exampleappbackend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity(name = "time_series_list")
public class TimeSeriesList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "permission_id")
    private Permission permission;

    @Column(name = "temporal_resolution")
    private String temporalResolution;

    @Column(name = "unit")
    private String unit;

    @OneToMany(mappedBy = "timeSeriesList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSeries> timeSeries;
}
