package energy.eddie.regionconnector.us.green.button.permission.events;

import java.io.Serializable;

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

    public String getPermissionId() {
        return permissionId;
    }

    public String getMeterUid() {
        return meterUid;
    }
}
