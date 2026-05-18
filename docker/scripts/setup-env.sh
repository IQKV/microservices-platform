#!/bin/bash

# Check if .env already exists
if [ -f .env ]; then
    echo ".env file already exists. Skipping copy."
else
    if [ -f .env.example ]; then
        cp .env.example .env
        echo ".env file created from .env.example"
    else
        echo "Error: .env.example not found."
        exit 1
    fi
fi
