#!/bin/bash

set -e

function prop {
    grep "${1}" gradle.properties | cut -d'=' -f2
}

MC_VERSION=$(prop minecraft_version)

if [[ $MC_VERSION == *"w"** ]]; then
    branch=port/${MC_VERSION}
    echo "Detected MC snapshot ${MC_VERSION}"
    cd $(mktemp -d)
    echo "Downloading Kits ${branch}..."
    git clone -q -b ${branch} --depth 1 https://github.com/neoforged/NeoForge Kits >/dev/null
    cd Kits
    echo "Compiling Kits"
    ./gradlew neoforge:setup
    ./gradlew neoforge:publishToMavenLocal
fi
