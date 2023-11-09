/**
 * Package implementing the storage and APIs for data needs. Data needs are provided by the application via a REST
 * API implemented in {@link energy.eddie.framework.dataneeds.DataNeedsController}. Data needs can be either read from
 * the spring configuration or from the application database but not both. The actual mode depends on the value of
 * the configuration property <tt>eddie.data-needs-config.data-need-source</tt>.
 * <h2>Data needs from configuration</h2>
 * property: <tt>eddie.data-needs-config.data-need-source=CONFIG</tt>
 * <p>
 * If data needs are read from the spring configuration, the following optional spring beans are created:
 * <ul>
 *     <li>{@link energy.eddie.framework.dataneeds.DataNeedsConfigService}</li>
 * </ul>
 *
 * <h2>Data needs from a database</h2>
 * property: <tt>eddie.data-needs-config.data-need-source=DATABASE</tt>
 * If data needs are read from the application db, the following optional spring beans are created:
 * <ul>
 *     <li>{@link energy.eddie.framework.dataneeds.DataNeedsDbService}</li>
 *     <li>{@link energy.eddie.framework.dataneeds.DataNeedsDbRepository}</li>
 *     <li>{@link energy.eddie.framework.dataneeds.DataNeedsManagementController}</li>
 * </ul>
 */
package energy.eddie.core.dataneeds;
