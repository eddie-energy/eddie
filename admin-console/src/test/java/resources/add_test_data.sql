--Some datas to be inserted into the database for testing purposes only

delete from status_messages;

insert into status_messages (permission_id, country, dso, start_date, status)
values ('mrid1', 'NFR', 'Enedis', '2024-05-18T07:28:09+02:00', 'A14'),
       ('mrid1', 'NFR', 'Enedis', '2024-05-18T08:20:03+02:00', 'A08'),
       ('mrid1', 'NFR', 'Enedis', '2024-05-21T09:22:03+02:00', 'A06'),
       ('mrid1', 'NFR', 'Enedis', '2024-05-22T17:41:00+02:00', 'A05'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-17T08:23:03+02:00', 'A14'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-18T08:23:03+02:00', 'A03'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-19T08:23:03+02:00', 'A04'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-20T08:23:03+02:00', 'A05'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-21T08:23:03+02:00', 'A108'),
       ('mrid2', 'NDK', 'Energinet', '2024-05-22T08:23:03+02:00', 'A109'),
       ('mrid3', 'NAT', 'Eda', '2024-04-20T18:10:00+02:00', 'A112'),
       ('mrid3', 'NAT', 'Eda', '2024-04-21T08:42:56+02:00', 'A08'),
       ('mrid3', 'NAT', 'Eda', '2024-04-22T08:33:54+02:00', 'A34'),
       ('mrid4', 'NES', 'Datadis', '2024-05-18T07:28:09+02:00', 'A05'),
       ('mrid4', 'NES', 'Datadis', '2024-05-19T07:30:12+02:00', 'A03'),
       ('mrid4', 'NES', 'Datadis', '2024-05-20T07:32:55+02:00', 'A04'),
       ('mrid4', 'NES', 'Datadis', '2024-05-20T07:33:09+02:00', 'A06'),
       ('mrid5', 'NNL', 'Mijn Aansluiting', '2024-05-20T07:33:09+02:00', 'A05'),
       ('mrid6', 'NNL', 'Mijn Aansluiting', '2024-04-20T07:33:09+02:00', 'A07'),
       ('mrid6', 'NNL', 'Mijn Aansluiting', '2024-04-21T07:33:09+02:00', 'A06'),
       ('mrid6', 'NNL', 'Mijn Aansluiting', '2024-04-21T08:33:09+02:00', 'A05'),
       ('mrid6', 'NNL', 'Mijn Aansluiting', '2024-04-30T07:33:09+02:00', 'A11'),
       ('mrid6', 'NNL', 'Mijn Aansluiting', '2024-05-01T07:12:09+02:00', 'A09'),
       ('mrid7', 'NFR', 'Enedis', '2024-05-25T07:33:09+02:00', 'A112'),
       ('mrid7', 'NFR', 'Enedis', '2024-05-25T08:33:09+02:00', 'A108'),
       ('mrid8', 'NDK', 'Energinet', '2024-05-25T12:33:09+02:00', 'A05'),
       ('mrid9', 'NDK', 'Energinet', '2024-05-25T12:33:09+02:00', 'ABCDEF'),
       ('mrid10', 'NAT', 'Eda', '2024-05-22T20:12:55+02:00', 'A112'),
       ('mrid11', 'NES', 'Datadis', '2024-05-13T07:28:09+02:00', 'A109');




