package energy.eddie.epdemoapp;

public enum Env {
    JDBC_URL, JDBC_USER, JDBC_PASSWORD, EDDIE_FRAMEWORK_PUBLIC_URL, PUBLIC_CONTEXT_PATH;

    public String get() {
        return System.getenv(this.name());
    }
}
