--  SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
--  SPDX-License-Identifier: Apache-2.0

CREATE TABLE public.data_source_simulation
(
    id                   uuid PRIMARY KEY REFERENCES data_source_interval (id),
    min_consumption_watt BIGINT NOT NULL DEFAULT 200,
    max_consumption_watt BIGINT NOT NULL DEFAULT 1800,
    min_production_watt  BIGINT NOT NULL DEFAULT 0,
    max_production_watt  BIGINT NOT NULL DEFAULT 1200
);

INSERT INTO public.data_source_simulation (id)
SELECT interval.id
FROM public.data_source_interval interval
         JOIN public.data_source data_source ON data_source.id = interval.id
WHERE data_source.type = 'SIMULATION';
