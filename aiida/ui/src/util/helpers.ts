export function beautifyPermissionStatusName(status: string) {
    return status.charAt(0).toUpperCase() + status.replace("_", " ").toLowerCase().slice(1);
}