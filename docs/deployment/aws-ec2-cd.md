# AWS EC2 Continuous Deployment

This repository deploys to an AWS EC2 instance through GitHub Actions. The Docker image is built on the GitHub-hosted runner, then copied to EC2 and loaded with `docker load`. EC2 only runs the already-built image, so deployment does not spend CPU and memory compiling the app on the server.

## GitHub Configuration

Add these repository secrets:

- `AWS_EC2_HOST`: EC2 public IPv4 or DNS name.
- `AWS_EC2_USER`: SSH user, commonly `ubuntu` for Ubuntu AMIs or `ec2-user` for Amazon Linux.
- `AWS_EC2_SSH_KEY`: Private key content for the EC2 key pair.
- `AWS_EC2_SSH_PORT`: Optional, defaults to `22`.

Optional repository variable:

- `AWS_EC2_DEPLOY_DIR`: Remote deployment directory, defaults to `bidmart-booking` under the SSH user's home directory.

The deployment runs on every push to `main` or `staging`, and can also be started manually from the `Deploy to AWS EC2` workflow.

## EC2 Prerequisites

Install Docker and the Docker Compose plugin on the EC2 instance. The SSH user must be able to run `docker compose`.

Open the EC2 security group for the application port. The default public application port is `8085`.

## Runtime Environment

On first deploy, the workflow creates `deploy/aws/.env` from `deploy/aws/.env.example` on the EC2 instance. Update that remote `.env` file with the real database password after the first deploy:

```bash
cd ~/bidmart-booking
nano deploy/aws/.env
docker compose --env-file deploy/aws/.env -f deploy/aws/docker-compose.yml up -d
```

Do not commit the real `.env` file or the EC2 private key.
