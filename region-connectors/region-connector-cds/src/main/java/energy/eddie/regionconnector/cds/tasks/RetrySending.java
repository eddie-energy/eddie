// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.tasks;

import org.springframework.scheduling.annotation.Scheduled;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Scheduled(cron = "${region-connector.cds.retry:0 0 * * * *}")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RetrySending {
}
