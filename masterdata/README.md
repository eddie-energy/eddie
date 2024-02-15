# Transforming the BPRT Region Roles Spreadsheet to JSON

In order for the application to interpret the data in the BPRT Region Roles spreadsheet, it must be transformed into a JSON format.

## Spreadsheet to CSV

For easier processing, the spreadsheet should be converted to a csv file.

The preferred way to convert the exported spreadsheet to a csv file is to use the `in2csv` command from the `csvkit` package. 
`csvkit` can be installed using Python's `pip` as follows:

```shell
pip install csvkit
```

The following command can be used to convert the exported Excel spreadsheet to a set of csv files:

```shell
in2csv BPRT_Region_Roles.xlsx --write-sheets - --use-sheet-names
```

## CSV to JSON

The exported data requires further processing to be usable by the application. This is done through a Python script that reads the csv file, adds additional fields, and writes the data to a JSON file.

The permission administrators data is transformed using the `transform-permission-administrators.py` script.
To update the permission administrators data, run the following command:

```shell
python transform-permission-administrators.py permission_administrator.csv ../core/src/main/resources/permission-administrators.json
```