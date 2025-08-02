# Jeffrey CLI

A command-line tool for initializing and managing Jeffrey project structures with automated environment variable generation for Java Flight Recorder profiling workflows.

## Overview

Jeffrey CLI simplifies the setup and maintenance of profiling projects by creating organized directory structures and generating environment variables needed for Java Flight Recorder (JFR) profiling sessions.

## Features

- **Project Structure Initialization**: Creates organized directory hierarchies for profiling projects
- **Session Management**: Automatically generates timestamped session directories
- **Environment Variable Generation**: Creates `.env` files with all necessary variables for profiling workflows
- **Flexible Configuration**: Supports both Jeffrey home directory and standalone repositories approaches
- **Docker Support**: Available as a containerized application
- **Java 21 Compatible**: Built with modern Java standards

## Installation

### Prerequisites

- Java 21 or higher
- Maven 3.6+ (for building from source)

### Building from Source

```bash
git clone https://github.com/petrbouda/jeffrey-cli.git
cd jeffrey-cli
mvn clean package
```

The executable JAR will be created at `target/jeffrey-cli.jar`.

### Using Docker

```bash
docker pull petrbouda/jeffrey-cli:latest
```

## Usage

### Command Line Interface

#### Basic Syntax

```bash
java -jar jeffrey-cli.jar init [OPTIONS]
```

#### Required Options

- `--project <name>`: Specify the project name for directory and session generation

#### Configuration Options (Choose One)

- `--jeffrey-home <path>`: Jeffrey HOME directory path (automatically creates repositories subdirectory)
- `--repositories <path>`: Direct path to repositories directory

#### Additional Options

- `--silent`: Suppress output, only create variables without printing
- `--help`: Show help information
- `--version`: Display version information

### Examples

#### Using Jeffrey Home Directory

```bash
java -jar jeffrey-cli.jar init --jeffrey-home /opt/jeffrey --project myapp
```

This creates:
- `/opt/jeffrey/repositories/myapp/` (project directory)
- `/opt/jeffrey/repositories/myapp/2025-01-15-143022/` (timestamped session)
- `/opt/jeffrey/repositories/myapp/.env` (environment variables)

#### Using Direct Repositories Path

```bash
java -jar jeffrey-cli.jar init --repositories /data/profiling --project web-service
```

This creates:
- `/data/profiling/web-service/` (project directory)
- `/data/profiling/web-service/2025-01-15-143022/` (timestamped session)
- `/data/profiling/web-service/.env` (environment variables)

#### Silent Mode

```bash
java -jar jeffrey-cli.jar init --repositories /data/profiling --project myapp --silent
```

Creates the structure without printing the environment variables to stdout.

### Docker Usage

#### Using Jeffrey Home

```bash
docker run -v /opt/jeffrey:/jeffrey petrbouda/jeffrey-cli:latest \
  init --jeffrey-home /jeffrey --project myapp
```

#### Using Repositories Directory

```bash
docker run -v /data/profiling:/repositories petrbouda/jeffrey-cli:latest \
  init --repositories /repositories --project web-service
```

## Generated Environment Variables

The tool creates an `.env` file in the project directory with the following variables:

- `JEFFREY_HOME`: Jeffrey home directory (when using --jeffrey-home)
- `JEFFREY_REPOSITORIES`: Path to repositories directory
- `JEFFREY_CURRENT_PROJECT`: Path to current project directory
- `JEFFREY_CURRENT_SESSION`: Path to current timestamped session directory
- `JEFFREY_FILE`: Path to JFR profile file template (`profile-%t.jfr`)

### Sourcing Environment Variables

```bash
source /path/to/project/.env
```

## Directory Structure

```
jeffrey-home/                    # Jeffrey home (optional)
└── repositories/               # Repositories directory
    └── project-name/           # Your project
        ├── .env                # Environment variables
        ├── 2025-01-15-143022/  # Session directory (timestamp)
        ├── 2025-01-15-150430/  # Another session
        └── ...
```

## Session Naming

Sessions are automatically named using UTC timestamps in the format: `yyyy-MM-dd-HHmmss`

Example: `2025-01-15-143022` represents January 15, 2025 at 14:30:22 UTC.

## Error Handling

The tool performs validation and provides clear error messages for:

- Missing required arguments
- Conflicting configuration options
- Directory creation failures
- File system permission issues

Exit codes:
- `0`: Success
- `1`: Error occurred

## Development

### Project Structure

```
src/main/java/pbouda/jeffrey/init/
├── CliApplication.java          # Main application entry point
├── VersionProvider.java         # Version information provider
├── ResourceUtils.java           # Utility for reading resources
└── command/
    └── InitCommand.java         # Main init command implementation
```

### Building

```bash
mvn clean package
``

## License

This project is licensed under the GNU Affero General Public License v3.0. See the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## Related Projects

This CLI tool is part of the Jeffrey ecosystem for Java application profiling and performance analysis.

