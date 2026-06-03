# Apereo CAS High Availability Deployment Guide

## Overview

This directory contains resources for deploying Apereo CAS in a high availability configuration with Hazelcast and Redis for distributed ticket registry, Prometheus for monitoring, and GitLab CI for automation.

## Directory Structure

- `helm/cas/` - Helm Chart for deploying CAS on Kubernetes
- `prometheus/` - Prometheus alerting rules
- `gitlab/` - GitLab CI pipeline configuration (`.gitlab-ci.yml` at the root)

## Components

### 1. Hazelcast Cluster Health Indicator

A custom Spring Boot HealthIndicator has been added to provide visibility into the Hazelcast cluster status:
- Endpoint: `/actuator/health/hazelcastClusterHealthIndicator`
- Shows cluster size, local member, and all cluster members
- Logs a warning when cluster size is less than 2

### 2. Helm Chart

The Helm Chart configures:
- CAS server deployment with 2 replicas by default
- Hazelcast with TCP-IP and Kubernetes discovery
- Redis Sentinel integration for ticket registry
- Prometheus ServiceMonitor for metrics scraping

### 3. Prometheus Alerts

Key alerts configured:
- `HazelcastClusterSizeLow`: Warns when cluster members < 2 for 5 minutes
- `HazelcastClusterSizeCritical`: Critical alert when cluster members < 1
- `CasInstanceDown`: Critical alert when CAS instance is down
- `CasHighErrorRate`: Warning when high error rate occurs

### 4. GitLab CI Pipeline

Stages:
1. **Build**: Compiles CAS and builds the WAR file
2. **Test**: Runs relevant tests (monitor and Hazelcast modules)
3. **Package-Docker**: Builds and pushes Docker image
4. **Deploy-Helm**: Deploys using Helm to production

## Getting Started

### Prerequisites

- Kubernetes 1.20+
- Helm 3.0+
- Prometheus Operator (for ServiceMonitor and PrometheusRule)
- Redis Sentinel cluster or Redis service
- GitLab Runner (for CI/CD)

### Deploying with Helm

```bash
cd deploy/helm
helm install cas ./cas
```

### Customizing Deployment

Edit `helm/cas/values.yaml` to customize:
- Image repository and tag
- Replica count
- Hazelcast and Redis configuration
- Resources limits
- Ingress settings

### Applying Prometheus Alerts

```bash
kubectl apply -f deploy/prometheus/alerts.yaml
```

### GitLab CI Setup

1. Add CI/CD variables in GitLab:
   - `CI_REGISTRY`: Your container registry URL
   - `CI_REGISTRY_USER`: Registry username
   - `CI_REGISTRY_PASSWORD`: Registry password

2. Push to GitLab, the pipeline will automatically build, test, and deploy.

## Architecture Diagram

```
                    ┌───────────────┐
                    │   Ingress     │
                    └───────┬───────┘
                            │
                    ┌───────▼───────┐
                    │   CAS Nodes   │
                    │  (2 Replicas) │
                    └───────┬───────┘
                            │
             ┌──────────────┼──────────────┐
             │              │              │
    ┌────────▼────────┐    │    ┌────────▼────────┐
    │   Hazelcast     │    │    │   Redis Sentinel│
    │   Cluster       │    │    │   (3 Nodes)     │
    └─────────────────┘    │    └─────────────────┘
                           │
                    ┌──────▼───────┐
                    │  Prometheus  │
                    │   & Grafana  │
                    └──────────────┘
```

## Notes

- The health indicator is automatically registered when using the Hazelcast ticket registry
- Ensure the Hazelcast ports (5701) are open between CAS nodes
- Redis Sentinel provides high availability for Redis
- Prometheus metrics are exposed at `/actuator/prometheus`
