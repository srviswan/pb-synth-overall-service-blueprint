# Environment Setup for Cursor Cloud Agents

This repository includes a repo-level Cursor environment config at:

- `.cursor/environment.json`

It is used to make cloud-agent sessions reproducible and avoid repeated bootstrap work.

## What it installs

The `install` step installs the required tooling:

- Maven
- Docker engine (`docker.io`)
- Docker Compose v2 (`docker-compose-v2`)

## Why Docker is started with special flags

In some cloud/CI kernels, Docker cannot start with default networking and storage settings due to missing or restricted kernel capabilities (for example, iptables NAT table support or overlay filesystem constraints).

To keep Docker usable in restricted environments, the `start` step launches `dockerd` with:

- `--iptables=false`
- `--bridge=none`
- `--storage-driver=vfs`

These settings prioritize compatibility and reliability over networking/performance features.

## Operational implications

- Containers run without Docker bridge networking.
- Compose service-to-service DNS by service name may not work as in default bridge mode.
- Host-network style connectivity may be required for inter-service communication in cloud-agent runs.
- Build and runtime IO can be slower with `vfs` than `overlay2`.

## For more capable environments

If your target environment supports standard Docker features, you can use default daemon settings (or `overlay2` + normal bridge networking) by adjusting `.cursor/environment.json`.
