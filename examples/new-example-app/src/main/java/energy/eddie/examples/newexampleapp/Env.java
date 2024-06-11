package energy.eddie.examples.newexampleapp;

public enum Env {
    JDBC_URL,
    JDBC_USER,
    JDBC_PASSWORD;

    public String get() {
        return System.getenv(this.name());
    }
}