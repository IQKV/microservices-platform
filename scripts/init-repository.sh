#!/usr/bin/env bash

# Exit on error
set -e

echo "Starting repository initialization..."

# 1. Process all submodules
if [ -f .gitmodules ]; then
    echo "Found .gitmodules, processing submodules..."

    # Extract submodule names from .gitmodules
    submodule_names=$(git config --file .gitmodules --get-regexp path | sed 's/submodule\.\(.*\)\.path.*/\1/')

    for name in $submodule_names; do
        path=$(git config --file .gitmodules --get "submodule.$name.path")
        url=$(git config --file .gitmodules --get "submodule.$name.url")

        echo "Processing submodule '$name' at '$path'..."

        # Step 1: Remove the gitlink entry from the index so git stops treating
        # this path as a submodule. Must happen BEFORE git add, otherwise git
        # re-stages it as an empty gitlink (the "empty folder" seen on GitHub).
        echo "Deregistering submodule gitlink for '$path'..."
        git rm --cached "$path" 2>/dev/null || true

        # Step 2: Clean up submodule metadata
        git config -f .git/config --remove-section "submodule.$name" 2>/dev/null || true
        rm -rf ".git/modules/$name"
        rm -rf "$path"

        # Step 3: Convert SSH URL to HTTPS for environments without SSH keys
        # (CI runners, fresh template checkouts, GitHub Actions).
        # git@github.com:ORG/REPO.git  →  https://github.com/ORG/REPO.git
        https_url=$(echo "$url" | sed -E 's|^git@([^:]+):(.+)$|https://\1/\2|')
        echo "Cloning '$https_url' into '$path'..."
        git clone --depth 1 "$https_url" "$path"

        # Step 4: Strip the cloned .git so the directory becomes plain source
        echo "Flattening '$path' (removing .git)..."
        rm -rf "$path/.git"

        # Step 5: Stage as regular files — git now sees a normal directory
        echo "Staging '$path' as regular files..."
        git add "$path"
    done

    # Stage the .gitmodules deletion
    echo "Removing .gitmodules..."
    rm -f .gitmodules
    git add -u .gitmodules 2>/dev/null || true
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
git add -u .github/workflows/use-template.yml 2>/dev/null || true

# Delete ourselves last (can't stage a file after it's gone, so we skip add)
SCRIPT_PATH=$0
echo "Deleting initialization script: $SCRIPT_PATH"
rm -f "$SCRIPT_PATH"

echo "Repository initialization complete."
