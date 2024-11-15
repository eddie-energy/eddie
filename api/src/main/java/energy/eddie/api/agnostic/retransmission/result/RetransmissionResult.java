package energy.eddie.api.agnostic.retransmission.result;

public sealed interface RetransmissionResult permits
        Success,
        PermissionRequestNotFound,
        NotSupported,
        NoPermissionForTimeFrame,
        NoActivePermission,
        DataNotAvailable,
        Failure {}
