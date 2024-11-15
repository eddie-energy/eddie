# Transforming the BPRT Region Roles Spreadsheet to JSON

In order for the application to interpret the data in the BPRT Region Roles spreadsheet, it must be transformed into a JSON format.

## Spreadsheet to CSV

For easier processing, the spreadsheet should be converted to a csv file.

The preferred way to convert the exported spreadsheet to a csv file is to use the `in2csv` command from the `csvkit` package. 
`csvkit` can be installed using Python's `pip` as follows:

```shell
pip install csvkit
```

The following commands can be used to convert the exported Excel spreadsheet to csv files:

```shell
in2csv BPRT_Region_Roles.xlsx --sheet "Permission Administrator" > permission_administrator.csv
in2csv BPRT_Region_Roles.xlsx --sheet "Metered Data Administrators" > metered_data_administrators.csv
```

This will create a csv file for each sheet in the Excel file. The csv files will be named after the sheet they were created from.

## CSV to JSON

The exported data requires further processing to be usable by the application. This is done through the Python script `transform.py` that reads the csv file, adds additional fields, and writes the data to a JSON file.

The type of data to be transformed is specified as an argument to the script.
To update the permission administrators, run the following command:

```shell
python transform.py pa permission_administrator.csv ../european-masterdata/src/main/resources/permission-administrators.json
```

For metered data administrators, run this command instead.

```shell
python transform.py mda metered_data_administrators.csv ../european-masterdata/src/main/resources/metered-data-administrators.json
```