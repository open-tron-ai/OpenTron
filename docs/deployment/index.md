---
title: Deployment
description: Deploy OpenTron in production environments
---

# Deployment

OpenTron supports multiple deployment strategies for different environments
and scales.

## Docker

The recommended way to deploy OpenTron in production. Multi-stage builds
with CPU and GPU (NVIDIA CUDA, AMD ROCm) variants.

[:octicons-arrow-right-24: Docker deployment](docker.md)

## systemd (Linux)

Run OpenTron as a managed system service on Linux servers.

[:octicons-arrow-right-24: systemd setup](systemd.md)

## launchd (macOS)

Register OpenTron as a launch agent on macOS.

[:octicons-arrow-right-24: launchd setup](launchd.md)

## API Server

Run OpenTron as an OpenAI-compatible HTTP server via `Tron serve`.

[:octicons-arrow-right-24: API server guide](api-server.md)

