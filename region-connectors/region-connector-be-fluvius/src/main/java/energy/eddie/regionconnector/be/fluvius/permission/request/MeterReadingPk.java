package energy.eddie.regionconnector.be.fluvius.permission.request;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings({"NullAway", "unused"})
public class MeterReadingPk implements Serializable {
    private final String permissionId;
    private final String meterEan;

    public MeterReadingPk(String permissionId, String meterEan) {
        this.permissionId = permissionId;
        this.meterEan = meterEan;
    }

    protected MeterReadingPk() {
        permissionId = null;
        meterEan = null;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(permissionId);
        result = 31 * result + Objects.hashCode(meterEan);
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeterReadingPk that)) return false;

        return Objects.equals(permissionId, that.permissionId) && Objects.equals(meterEan, that.meterEan);
    }

    public String getPermissionId() {
        return permissionId;
    }

    public String getMeterEan() {
        return meterEan;
    }
}
