package energy.eddie.dataneeds.utils.cron;

public enum CronExpressionDefaults {
    SECONDLY("* * * * * *"),
    MINUTELY("0 * * * * *"),
    HOURLY("0 0 * * * *"),
    DAILY("0 0 0 * * *");

    private final String expression;

    CronExpressionDefaults(String expression) {
        this.expression = expression;
    }

    public String expression() {
        return expression;
    }
}
