#!/usr/bin/env bash
#
# Build ORPixip.

set -euo pipefail
cd "$(dirname "$0")"
. ./common.sh

check_toolchain
check_openrate_installed

step "Building ORPixip against OpenRate $(openrate_version)"
mvn -B clean package

say ""
say "Built. Bring the database up with:  ./db.sh"
say "Then start the framework with:      ./run.sh"
say "Or run the whole demo with:         ./demo.sh"
