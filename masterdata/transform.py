#!/usr/bin/env python3

"""
Transforms permission administrators CSV file into JSON.

Usage:
    python transform.py pa input.csv output.json
"""


import argparse
import csv
import json

# Parse the command line arguments
parser = argparse.ArgumentParser()
parser.add_argument('type', help='type of the CSV file (pa or mda)')
parser.add_argument('input_file', help='path to the input CSV file')
parser.add_argument('output_file', help='path to the output JSON file')
args = parser.parse_args()

region_connectors = {
    'at': 'at-eda',
    'dk': 'dk-energinet',
    'es': 'es-datadis',
    'fr': 'fr-enedis'
}

# Read the contents of the input CSV file as a CSV
with open(args.input_file, 'r', encoding='utf-8') as f:
    reader = csv.reader(f, delimiter=',')
    rows = list(reader)

# Extract the required information from the rows
result = []
for row in rows[1:]:

    if (args.type == 'pa'):
        country = row[0].lower()
        regionConnector = region_connectors.get(country, country)

        result.append({
            'country': country,
            'company': row[2],
            'companyId': row[4] if row[4] != 'n.a.' else '',
            'jumpOffUrl': row[6] if 'http' in row[6] else '',
            'regionConnector': regionConnector
        })

    if (args.type == 'mda'):
        result.append({
            'country': row[0].lower(),
            'company': row[2],
            'companyId': row[4] if row[4] != 'n.a.' else '',
            'websiteUrl': row[5] if 'http' in row[5] else '',
            'officialContact': row[6] if row[6] != 'n.a.' else '',
            'permissionAdministrator': row[13]
        })

# Sort the list of dictionaries by country and then company
result = sorted(result, key=lambda x: (x['country'], x['company']))

# Convert the list of dictionaries to JSON and write it to the output file
with open(args.output_file, 'w', encoding='utf-8') as f:
    json.dump(result, f, indent=2, ensure_ascii=False)