#!/usr/bin/env python3

"""
Transforms permission administrators CSV file into JSON.

Usage:
    python transform.py pa input.csv output.json
"""


import argparse
import csv
import json
import re

# Parse the command line arguments
parser = argparse.ArgumentParser()
parser.add_argument('type', help='type of the CSV file (pa or mda)')
parser.add_argument('input_file', help='path to the input CSV file')
parser.add_argument('output_file', help='path to the output JSON file')
args = parser.parse_args()

REGION_CONNECTORS = {
    'at': 'at-eda',
    'be': 'be-fluvius',
    'de': 'de-eta',
    'dk': 'dk-energinet',
    'es': 'es-datadis',
    'fi': 'fi-fingrid',
    'fr': 'fr-enedis',
    'nl': 'nl-mijn-aansluiting',
    'us': 'us-green-button',
    'ca': 'us-green-button'
}

def kebab(value):
    return re.sub('[^0-9a-z-]', '', value.lower().replace(' / ', '-').replace(' ', '-'))

def empty(value):
    return value.strip().lower() in ['n.a.', 'n/a', '-', '']

# Read the contents of the input CSV file as a CSV
with open(args.input_file, 'r', encoding='utf-8') as f:
    reader = csv.reader(f, delimiter=',')
    rows = list(reader)

# Extract the required information from the rows
result = []
for row in rows[1:]:
    if not row:
        continue

    if (args.type == 'pa'):
        country = row[0].lower()
        name = row[3] if not empty(row[3]) else row[2]
        regionConnector = REGION_CONNECTORS.get(country, country)

        result.append({
            'country': country,
            'company': row[2],
            'name': name,
            'companyId': row[5] if not empty(row[5]) else kebab(name),
            'jumpOffUrl': row[7] if 'http' in row[7] or 'https' in row[7] else '',
            'regionConnector': regionConnector
        })

    if (args.type == 'mda'):
        result.append({
            'country': row[0].lower(),
            'company': row[2],
            'companyId': row[4] if not empty(row[4]) else kebab(row[2]),
            'websiteUrl': row[5] if 'http' in row[5] else '',
            'officialContact': row[6] if row[6] != 'n.a.' else '',
            'permissionAdministrator': row[13]
        })

# Sort the list of dictionaries by country and then company
result = sorted(result, key=lambda x: (x['country'], x['company']))

# Convert the list of dictionaries to JSON and write it to the output file
with open(args.output_file, 'w', encoding='utf-8') as f:
    json.dump(result, f, indent=2, ensure_ascii=False)