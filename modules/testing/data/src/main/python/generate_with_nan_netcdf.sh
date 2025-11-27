#!/usr/bin/env sh

SCRIPT_DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

python -m venv .venv

.venv/bin/pip install -r "$SCRIPT_DIR/requirements.txt"
.venv/bin/python -s "$SCRIPT_DIR/create_with_nan.nc.py" "$SCRIPT_DIR/../resources/org/constellation/netcdf/with_nan.nc"

rm -rf .venv
