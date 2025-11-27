import sys

import numpy as np
import xarray as xr

output_file = sys.argv[1]
if output_file is None or len(output_file) == 0:
    raise ValueError("Missing argument: output file")

# Create the data

# Example data: temperature and humidity
band1 = np.array([
    [ 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 ],
    [ 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 ],
    [ 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 ],
    [ 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 ],
    [ 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 ],
    [ 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 ],
    [ 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 ],
    [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
    [ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 ],
    [ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 ],
    [ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 ],
    [ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 ],
    [ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 ],
    [ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 ],
    [ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 ],
    [ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 ]
], dtype=np.uint8)


# Create the xarray Dataset
ds = xr.Dataset(
    {
        'Band1': (('lat', 'lon'), band1)
    },
    coords={
        'lon': [
            -1.875, -1.625, -1.375, -1.125, -0.875, -0.625, -0.375, -0.125,
            0.125, 0.375, 0.625, 0.875, 1.125, 1.375, 1.625, 1.875
        ],
        'lat': [-1.875, -1.625, -1.375, -1.125, -0.875, -0.625, -0.375, -0.125,
                0.125, 0.375, 0.625, 0.875, 1.125, 1.375, 1.625, 1.875
        ]
    }
)

# Add CF metadata
ds['Band1'].attrs = {
    '_FillValue': 0,
    'long_name': 'Band 1',
}
ds['lat'].attrs = {
    'units': 'degrees_north',
    'standard_name': 'latitude',
    'long_name': 'latitude',
}
ds['lon'].attrs = {
    'units': 'degrees_east',
    'standard_name': 'longitude',
    'long_name': 'longitude',
}

# Set global attributes
ds.attrs = {
    'Conventions': 'CF-1.8',
    'title': 'Example CF-compliant dataset with two latitude strips',
    'history': 'Created with xarray',
}

# Save to NetCDF
ds.to_netcdf(output_file)
print(f"NetCDF sample with nan values saved in {output_file}")
