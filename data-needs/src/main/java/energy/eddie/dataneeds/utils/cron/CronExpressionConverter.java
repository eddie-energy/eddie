// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.utils.cron;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.scheduling.support.CronExpression;

@Converter(autoApply = true)
public class CronExpressionConverter implements AttributeConverter<CronExpression, String> {
    @Override
    public String convertToDatabaseColumn(CronExpression cronExpression) {
        return cronExpression != null ? cronExpression.toString() : CronExpressionDefaults.SECONDLY.expression();
    }

    @Override
    public CronExpression convertToEntityAttribute(String dbCronExpression) {
        if (dbCronExpression == null || dbCronExpression.isEmpty()) {
            dbCronExpression = CronExpressionDefaults.SECONDLY.expression();
        }

        try {
            return CronExpression.parse(dbCronExpression);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not convert database value to CronExpression", e);
        }
    }
}
