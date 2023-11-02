package energy.eddie.examples.exampleapp;

public enum Env {
    JDBC_URL, JDBC_USER, JDBC_PASSWORD, EDDIE_PUBLIC_URL, PUBLIC_CONTEXT_PATH;

    public String get() {
        return System.getenv(this.name());
    }
}
