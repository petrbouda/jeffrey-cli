/*
 * Jeffrey
 * Copyright (C) 2025 Petr Bouda
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

CREATE TABLE IF NOT EXISTS main.workspace_projects
(
    project_id   TEXT    PRIMARY KEY,
    project_name TEXT    NOT NULL,
    created_at   INTEGER NOT NULL,
    attributes   TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS main.workspace_sessions
(
    session_id           TEXT    PRIMARY KEY,
    project_id           TEXT    PRIMARY KEY,
    session_path         TEXT    NOT NULL,
    created_at           INTEGER NOT NULL,
    latest_detected_file TEXT
);

