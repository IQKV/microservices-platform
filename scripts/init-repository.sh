#!/usr/bin/env bash

# Exit on error
set -e

echo "Starting repository initialization..."

# Promote README.template.md to README.md, archiving the previous README.md as
# README.boilerplate.md. Applied to the repository root and, after flattening,
# to every submodule so both layers are initialized the same way.
promote_readme() {
    dir=$1

    if [ ! -f "$dir/README.template.md" ]; then
        return 0
    fi

    if [ "$dir" = "." ]; then
        echo "Updating README files at the repository root..."
    else
        echo "Updating README files in '$dir'..."
    fi

    if [ -f "$dir/README.md" ]; then
        mv "$dir/README.md" "$dir/README.boilerplate.md"
        git add "$dir/README.boilerplate.md"
    fi
    mv "$dir/README.template.md" "$dir/README.md"
    git add "$dir/README.md"
}

# Remove a template "Create" workflow. GitHub only runs the workflow at the
# repository root, but a flattened submodule would otherwise ship a dead copy.
remove_template_workflow() {
    workflow=$1

    if [ ! -f "$workflow" ]; then
        return 0
    fi

    echo "Removing template workflow $workflow..."
    rm -f "$workflow"
    git add -u "$workflow" 2>/dev/null || true
}

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

        # Step 6: Initialize the flattened copy exactly like the root — promote
        # its README.template.md and drop its own template workflow.
        promote_readme "$path"
        remove_template_workflow "$path/.github/workflows/use-template.yml"
    done

    # Stage the .gitmodules deletion
    echo "Removing .gitmodules..."
    rm -f .gitmodules
    git add -u .gitmodules 2>/dev/null || true
fi

# 2. Handle root README files
promote_readme .

# 3. Remove the root template workflow
remove_template_workflow .github/workflows/use-template.yml

# 4. Delete ourselves last (can't stage a file after it's gone, so we skip add)
SCRIPT_PATH=$0
echo "Deleting initialization script: $SCRIPT_PATH"
rm -f "$SCRIPT_PATH"

echo "Repository initialization complete."
