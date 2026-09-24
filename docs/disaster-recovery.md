# Disaster Recovery & Backup Plan — DevPilot AI

## 1. Automated Database Backups
- Backup script: `scripts/backup-db.sh`.
- Dumps PostgreSQL schema and data to compressed `.sql.gz` archive.

## 2. Restoration Process
- Restore script: `scripts/restore-db.sh <backup-file.sql.gz>`.
- Restores database schema and Flyway migration state seamlessly.

## 3. RPO and RTO Targets
- Recovery Point Objective (RPO): < 1 hour (daily automated snapshots + transaction logs).
- Recovery Time Objective (RTO): < 15 minutes (automated container restart / DB restore).
