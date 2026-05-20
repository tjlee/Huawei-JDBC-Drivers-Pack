<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Huawei-JDBC-Drivers-Pack Changelog

## [Unreleased]

### Added
- GaussDB IAM (Access Key) authentication provider (`jdbc:dws:iam://` URL handler)
- Apache License 2.0 files bundled for GaussDB and ClickHouse drivers
- Project-level `DriverSwitcher` service to (re-)download driver artifacts for existing data sources

### Changed
- GaussDB driver JARs moved into versioned `GaussDB/8.6.1/` subdirectory for consistency with other drivers
- License agreement dialog now scans bundled drivers recursively and groups licenses by vendor

### Removed
- Template scaffolding: `MyBundle`, `MyProjectService`, `MyProjectActivity`, `MyToolWindowFactory`, `MyBundle.properties`

## [0.1.0] - 2026-05-14

### Added
- GaussDB JDBC Driver 8.6.1 with IAM (Access Key) authentication support
- MySQL Connector/J 9.5.0
- MongoDB JDBC Driver 1.21
- PostgreSQL JDBC Driver 42.6.0
- ClickHouse JDBC Driver 0.9.4
- OceanBase Client 2.4.14
- Redis JDBC Driver 1.6
- License agreement dialog on first startup
- Automatic driver installation to IDE configuration directory

[Unreleased]: https://github.com/tjlee/Huawei-JDBC-Drivers-Pack/compare/1.0.0...HEAD
[1.0.0]: https://github.com/tjlee/Huawei-JDBC-Drivers-Pack/compare/0.1.0...1.0.0
[0.1.0]: https://github.com/tjlee/Huawei-JDBC-Drivers-Pack/commits/0.1.0
