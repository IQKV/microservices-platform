#!/usr/bin/env bash

# Exit on error
set -e

echo "Starting repository initialization..."

# 1. Process all submodules
if [ -f .gitmodules ]; then
    echo "Found .gitmodules, processing submodules..."
    
    # Extract submodule paths and URLs using git config
    # We iterate over the sections in .gitmodules
    submodule_names=$(git config --file .gitmodules --get-regexp path | sed 's/submodule\.\(.*\)\.path.*/\1/')
    
    for name in $submodule_names; do
        path=$(git config --file .gitmodules --get "submodule.$name.path")
        url=$(git config --file .gitmodules --get "submodule.$name.url")
        
        echo "Processing submodule '$name' at '$path'..."
        
        echo "Removing submodule '$name' from git config..."
        git config -f .git/config --remove-section "submodule.$name" 2>/dev/null || true
        
        echo "Deleting submodule directory and git metadata for '$path'..."
        rm -rf ".git/modules/$name" 2>/dev/null || true
        rm -rf "$path"
        
        echo "Cloning fresh repository from '$url' into '$path'..."
        git clone "$url" "$path"
        
        echo "Removing .git directory from '$path' to flatten it..."
        rm -rf "$path/.git"
        
        echo "Adding '$path' to host repository..."
        git add "$path"
    done
    
    echo "Removing .gitmodules file..."
    rm -f .gitmodules
    git rm .gitmodules 2>/dev/null || true
fi

# 2. Handle README files
if [ -f README.template.md ]; then
    echo "Updating README files..."
    if [ -f README.md ]; then
        mv README.md README.boilerplate.md
        git add README.boilerplate.md
    fi
    mv README.template.md README.md
    git add README.md
fi

# 3. Final cleanup
echo "Cleaning up initialization script and workflow..."
rm -f .github/workflows/use-template.yml

# We delete ourselves last
SCRIPT_PATH=$0
echo "Deleting initialization script: $SCRIPT_PATH"
rm -f "$SCRIPT_PATH"

echo "Repository initialization complete."
