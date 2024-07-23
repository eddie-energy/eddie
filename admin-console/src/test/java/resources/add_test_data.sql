--Some datas to be inserted into the database for testing purposes only

delete from status_messages;

insert into status_messages (permission_id, country, dso, start_date, status, region_connector_id)
values ('mrid1', 'NFR', 'Enedis', '2024-05-18T07:28:09+02:00', 'A14', 'fr-enedis'),
       ('mrid1', 'NFR', 'Enedis', '2024-05-18T08:20:03+02:00', 'A08', 'fr-enedis'),
       ('mrid1', 'NFR', 'Enedis', '2024-05-21T09:22:03+02:00', 'A06', 'fr-enedis'),
       ('mrid1', 'NFR', 'Enedis', '2024-05-22T17:41:00+02:00', 'A05', 'fr-enedis'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-17T08:23:03+02:00', 'A14', 'dk-energinet'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-18T08:23:03+02:00', 'A03', 'dk-energinet'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-19T08:23:03+02:00', 'A04', 'dk-energinet'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-20T08:23:03+02:00', 'A05', 'dk-energinet'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-21T08:23:03+02:00', 'A108', 'dk-energinet'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-22T08:23:03+02:00', 'A109', 'dk-energinet'),
       ('mrid3', 'NAT', 'Eda', '2024-04-20T18:10:00+02:00', 'A112', 'at-eda'),
       ('mrid3', 'NAT', 'Eda', '2024-04-21T08:42:56+02:00', 'A08', 'at-eda'),
       ('mrid3', 'NAT', 'Eda', '2024-04-22T08:33:54+02:00', 'A34', 'at-eda'),
       ('mrid4', 'NES', 'Datadis', '2024-05-18T07:28:09+02:00', 'A05', 'es-datadis'),
       ('mrid4', 'NES', 'Datadis', '2024-05-19T07:30:12+02:00', 'A03', 'es-datadis'),
       ('mrid4', 'NES', 'Datadis', '2024-05-20T07:32:55+02:00', 'A04', 'es-datadis'),
       ('mrid4', 'NES', 'Datadis', '2024-05-20T07:33:09+02:00', 'A06', 'es-datadis'),
       ('mrid5', 'NNL', 'Mijn Aansluiting', '2024-05-20T07:33:09+02:00', 'A05', 'nl-mijn-aansluiting'),
       ('mrid6', 'NNL', 'Mijn Aansluiting', '2024-04-20T07:33:09+02:00', 'A07', 'nl-mijn-aansluiting'),
       ('mrid6', 'NNL', 'Mijn Aansluiting', '2024-04-21T07:33:09+02:00', 'A06', 'nl-mijn-aansluiting'),
       ('mrid6', 'NNL', 'Mijn Aansluiting', '2024-04-21T08:33:09+02:00', 'A05', 'nl-mijn-aansluiting'),
       ('mrid6', 'NNL', 'Mijn Aansluiting', '2024-04-30T07:33:09+02:00', 'A11', 'nl-mijn-aansluiting'),
       ('mrid6', 'NNL', 'Mijn Aansluiting', '2024-05-01T07:12:09+02:00', 'A09', 'nl-mijn-aansluiting'),
       ('mrid7', 'NFR', 'Enedis', '2024-05-25T07:33:09+02:00', 'A112', 'fr-enedis'),
       ('mrid7', 'NFR', 'Enedis', '2024-05-25T08:33:09+02:00', 'A108', 'fr-enedis'),
       ('mrid8', 'NDK', 'Energinet', '2024-05-25T12:33:09+02:00', 'A05', 'dk-energinet'),
       ('mrid9', 'NDK', 'Energinet', '2024-05-25T12:33:09+02:00', 'ABCDEF', 'dk-energinet'),
       ('mrid10', 'NAT', 'Eda', '2024-05-22T20:12:55+02:00', 'A112', 'at-eda'),
       ('mrid11', 'NES', 'Datadis', '2024-05-13T07:28:09+02:00', 'A109', 'es-datadis');




