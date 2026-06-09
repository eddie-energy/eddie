// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.utils.cron;

import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.scheduling.support.CronExpression;

@Converter(autoApply = true)
public class CronExpressionConverter implements AttributeConverter<CronExpression, String> {
    @Nullable
    @Override
    public String convertToDatabaseColumn(CronExpression cronExpression) {
        return cronExpression != null ? cronExpression.toString() : null;
    }

    @Nullable
    @Override
    public CronExpression convertToEntityAttribute(String dbCronExpression) {
        if (dbCronExpression == null || dbCronExpression.isEmpty()) {
            return null;
        }

        try {
            return CronExpression.parse(dbCronExpression);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not convert database value to CronExpression", e);
        }
    }
}
