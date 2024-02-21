#!/usr/bin/env python3

"""
Transforms a CSV file into a JSON file.

Usage:
    python transform.py input.csv output.json
"""


import argparse
import csv
import json

# Parse the command line arguments
parser = argparse.ArgumentParser()
parser.add_argument('input_file', help='path to the input CSV file')
parser.add_argument('output_file', help='path to the output JSON file')
args = parser.parse_args()

# Read the contents of the input CSV file as a CSV
with open(args.input_file, 'r', encoding='utf-8') as f:
    reader = csv.reader(f, delimiter=',')
    rows = list(reader)

# Extract the required information from the rows
result = []
for row in rows[1:]:
    country = row[0].lower()
    company = row[2]
    companyId = row[4] if row[4] != 'n.a.' else ''
    jumpOffUrl = row[6] if 'https' in row[6] else ''
    if country == 'at':
        regionConnector = 'at-eda'
    elif country == 'dk':
        regionConnector = 'dk-energinet'
    elif country == 'es':
        regionConnector = 'es-datadis'
    elif country == 'fr':
        regionConnector = 'fr-enedis'
    else:
        regionConnector = country

    # If empty infer company id from company name
    if companyId == '':
        companyId = re.sub(r'[^a-z0-9-]', '', company.lower().replace(' ', '-'))

    result.append({
        'country': country,
        'company': company,
        'companyId': companyId,
        'jumpOffUrl': jumpOffUrl,
        'regionConnector': regionConnector
    })

# Sort the list of dictionaries by country and then company
result = sorted(result, key=lambda x: (x['country'], x['company']))

# Convert the list of dictionaries to JSON and write it to the output file
with open(args.output_file, 'w', encoding='utf-8') as f:
    json.dump(result, f, indent=2, ensure_ascii=False)