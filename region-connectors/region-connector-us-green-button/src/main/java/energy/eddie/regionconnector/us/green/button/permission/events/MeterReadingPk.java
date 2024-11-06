package energy.eddie.regionconnector.us.green.button.permission.events;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings({"NullAway", "unused"})
public class MeterReadingPk implements Serializable {
    private final String permissionId;
    private final String meterUid;

    public MeterReadingPk(String permissionId, String meterUid) {
        this.permissionId = permissionId;
        this.meterUid = meterUid;
    }

    protected MeterReadingPk() {
        permissionId = null;
        meterUid = null;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(permissionId);
        result = 31 * result + Objects.hashCode(meterUid);
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeterReadingPk that)) return false;

        return Objects.equals(permissionId, that.permissionId) && Objects.equals(meterUid, that.meterUid);
    }

    public String getPermissionId() {
        return permissionId;
    }

    public String getMeterUid() {
        return meterUid;
    }
}
